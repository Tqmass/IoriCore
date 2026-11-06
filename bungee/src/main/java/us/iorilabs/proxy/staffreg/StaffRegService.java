package us.iorilabs.proxy.staffreg;

import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.proxy.config.StaffRegConfig;
import us.iorilabs.proxy.discord.DiscordBotService;
import us.iorilabs.proxy.discord.link.DiscordLinkService;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProfileLookup;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.punishment.BanCheckService;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.staff.StaffGroups;
import us.iorilabs.staffreg.AuditEntry;
import us.iorilabs.staffreg.Challenge;
import us.iorilabs.staffreg.DeviceRecord;
import us.iorilabs.staffreg.DiscordEmbedDto;
import us.iorilabs.staffreg.DiscordMessageRequest;
import us.iorilabs.staffreg.DiscordPort;
import us.iorilabs.staffreg.RiskContext;
import us.iorilabs.staffreg.StaffAccount;
import us.iorilabs.staffreg.StaffRegEnums.AccountStatus;
import us.iorilabs.staffreg.StaffRegEnums.AuthMode;
import us.iorilabs.staffreg.StaffRegEnums.ChallengeKind;
import us.iorilabs.staffreg.StaffRegEnums.ChallengeStatus;
import us.iorilabs.staffreg.StaffRegEnums.LinkStatus;
import us.iorilabs.staffreg.StaffRegEnums.RiskSignal;
import us.iorilabs.staffreg.StaffRegEnums.TrustField;
import us.iorilabs.staffreg.StaffSession;
import us.iorilabs.staffreg.TrustSnapshot;
import us.iorilabs.staffreg.crypto.StaffPasswordHasher;
import us.iorilabs.staffreg.impl.DefaultRiskEngine;

public final class StaffRegService {

    private static final Set<String> STAFF_COMMAND_PREFIXES = Set.of("staff", "staffreg", "staffauth");

    private final ProxyPlatform platform;
    private final ProxyLogger logger;
    private final LanguageManager language;
    private final StaffGroups staffGroups;
    private final StaffRegConfig config;
    private final StaffRegRepositories repos;
    private final StaffRegFailCounter failCounter;
    private final DefaultRiskEngine riskEngine = new DefaultRiskEngine();
    private final ProfileLookup profiles;
    private final RedisService redis;
    private final String proxyId;
    private final String ipPepper;
    private final Set<UUID> restricted = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Instant> stepUpUntil = new ConcurrentHashMap<>();
    private volatile DiscordPort discord;
    private volatile DiscordLinkService discordLinks;
    private volatile BanCheckService banCheck;

    public StaffRegService(
        ProxyPlatform platform,
        ProxyLogger logger,
        LanguageManager language,
        StaffGroups staffGroups,
        StaffRegConfig config,
        us.iorilabs.proxy.database.StorageService storage,
        RedisService redis,
        String proxyId
    ) {
        this.platform = platform;
        this.logger = logger;
        this.language = language;
        this.staffGroups = staffGroups;
        this.config = config;
        this.repos = new StaffRegRepositories(storage, logger);
        this.failCounter = new StaffRegFailCounter(redis, redis == null ? "" : redis.keyPrefix(), config);
        this.profiles = new ProfileLookup(storage, logger);
        this.redis = redis;
        this.proxyId = proxyId == null ? "" : proxyId;
        this.ipPepper = System.getenv("ROYALCTW_IP_PEPPER") == null ? "" : System.getenv("ROYALCTW_IP_PEPPER");
        if (ipPepper.isBlank()) logger.warn("ROYALCTW_IP_PEPPER unset — StaffReg IP HMAC uses empty pepper");
        if (redis != null) {
            redis.subscribe(ProxyEventType.DISCORD_LINK_UPDATE.id(), this::onDiscordLinkRemote);
        }
    }

    public void bindDiscord(DiscordBotService bot, DiscordLinkService links, String bridgeSecret) {
        this.discordLinks = links;
        this.discord = new StaffRegDiscordPort(bot, bot == null ? null : bot.config(), redis, logger, bridgeSecret);
    }

    public void bindBanCheck(BanCheckService banCheck) {
        this.banCheck = banCheck;
    }

    public void reload(StaffRegConfig config) {
        if (config != null) {
            // config is immutable holder; IoriProxy recreates service on reload if needed
        }
    }

    public CompletableFuture<CreateResult> create(String username, String password, UUID actorUuid) {
        if (username == null || username.isBlank() || username.length() > 16) {
            return CompletableFuture.completedFuture(CreateResult.invalid("staffreg.invalid-nick"));
        }
        if (password == null || password.length() < config.passwordMinLength()) {
            return CompletableFuture.completedFuture(CreateResult.invalid("staffreg.password-too-short"));
        }
        if (config.requireStepUpOnCreate() && !hasFreshStepUp(actorUuid)) {
            return CompletableFuture.completedFuture(CreateResult.invalid("staffreg.step-up-required"));
        }

        Optional<ProfileLookup.Known> known = profiles.findByName(username.trim());
        if (known.isEmpty()) {
            return CompletableFuture.completedFuture(CreateResult.invalid("staffreg.player-never-joined"));
        }

        UUID targetUuid = known.get().uuid();
        return repos
            .accounts()
            .findByUuid(targetUuid)
            .thenCompose(existing -> {
                if (existing.isPresent()) {
                    return CompletableFuture.completedFuture(CreateResult.invalid("staffreg.already-registered"));
                }

                Instant now = Instant.now();
                StaffAccount account = new StaffAccount()
                    .uuid(targetUuid)
                    .username(known.get().name())
                    .passwordHash(StaffPasswordHasher.hash(password))
                    .mustChangePassword(config.mustChangeOnFirstLogin())
                    .linkStatus(LinkStatus.UNLINKED)
                    .accountStatus(AccountStatus.ACTIVE)
                    .preferredAuthMode(AuthMode.DISCORD)
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy(actorUuid);

                return repos
                    .accounts()
                    .save(account)
                    .thenCompose(v -> audit("STAFF_CREATED", actorUuid, targetUuid, "Staff account created"))
                    .thenApply(v -> {
                        notifyCreated(account);
                        return CreateResult.ok(targetUuid, known.get().name());
                    });
            });
    }

    public void onStaffJoin(ProxyPlayer player, String ip) {
        if (player == null) return;
        repos
            .accounts()
            .findByUuid(player.uuid())
            .thenAccept(opt -> {
                if (opt.isEmpty()) return;
                StaffAccount account = opt.get();
                if (
                    account.accountStatus() == AccountStatus.LOCKED ||
                    account.accountStatus() == AccountStatus.BANNED_SECURITY
                ) {
                    applyRestrict(player.uuid(), true);
                    return;
                }
                if (
                    config.requireDiscordLink() &&
                    account.linkStatus() != LinkStatus.LINKED &&
                    config.unlinkedNoticeOnJoin()
                ) {
                    player.sendMessage(language.raw("staffreg.unlinked-notice"));
                }
                evaluateTrust(player, account, ip);
            });
    }

    public boolean isRestricted(UUID uuid) {
        return uuid != null && restricted.contains(uuid);
    }

    public boolean allowServerSwitch(UUID uuid) {
        return !isRestricted(uuid);
    }

    public boolean allowCommand(ProxyPlayer player, String commandLine) {
        if (player == null || commandLine == null) return true;
        String root = rootLabel(commandLine);
        if (isRestricted(player.uuid())) {
            return STAFF_COMMAND_PREFIXES.contains(root);
        }
        if (!requiresStepUp(player, root)) return true;
        Instant until = stepUpUntil.get(player.uuid());
        return until != null && until.isAfter(Instant.now());
    }

    public void onCommandDenied(ProxyPlayer player, boolean stepUp) {
        if (player == null) return;
        if (stepUp) {
            player.sendMessage(language.raw("staffreg.step-up-required"));
            issueStepUpChallenge(player.uuid());
        } else {
            player.sendMessage(language.raw("staffreg.restricted"));
        }
    }

    public CompletableFuture<Boolean> verifyPassword(UUID accountUuid, String password) {
        return repos
            .accounts()
            .findByUuid(accountUuid)
            .thenApply(opt -> {
                if (opt.isEmpty()) return false;
                StaffAccount account = opt.get();
                boolean ok = StaffPasswordHasher.verify(password, account.passwordHash());
                if (ok) {
                    failCounter.clear(accountUuid);
                    grantStepUp(accountUuid);
                    return true;
                }
                int fails = failCounter.recordFail(accountUuid);
                if (fails >= config.maxFails()) {
                    handleSecurityBan(account);
                }
                return false;
            });
    }

    public CompletableFuture<Boolean> lock(UUID targetUuid, UUID actorUuid) {
        return mutateStatus(targetUuid, actorUuid, AccountStatus.LOCKED, "STAFF_LOCKED");
    }

    public CompletableFuture<Boolean> unlock(UUID targetUuid, UUID actorUuid) {
        return mutateStatus(targetUuid, actorUuid, AccountStatus.ACTIVE, "STAFF_UNLOCKED");
    }

    public CompletableFuture<List<StaffSession>> listSessions(UUID accountUuid) {
        return repos.sessions().listForAccount(accountUuid);
    }

    public CompletableFuture<StubResult> resetAccount(UUID targetUuid, UUID actorUuid, String note) {
        return audit("STAFF_RESET_REQUESTED", actorUuid, targetUuid, note == null ? "needs 2nd admin" : note).thenApply(
            v -> StubResult.needsSecondAdmin("staffreg.reset-needs-second-admin")
        );
    }

    public CompletableFuture<StubResult> unlinkDiscord(UUID targetUuid, UUID actorUuid, String note) {
        return audit(
            "STAFF_UNLINK_REQUESTED",
            actorUuid,
            targetUuid,
            note == null ? "needs 2nd admin" : note
        ).thenApply(v -> StubResult.needsSecondAdmin("staffreg.unlink-needs-second-admin"));
    }

    public void assignDiscordPending(UUID accountUuid, String discordId) {
        if (accountUuid == null || discordId == null || discordId.isBlank()) return;
        repos
            .accounts()
            .findByUuid(accountUuid)
            .thenAccept(opt -> {
                if (opt.isEmpty()) return;
                StaffAccount account = opt.get();
                account.linkStatus(LinkStatus.PENDING_CONFIRM);
                account.updatedAt(Instant.now());
                repos.accounts().save(account);

                Challenge challenge = new Challenge()
                    .id(UUID.randomUUID())
                    .accountUuid(accountUuid)
                    .kind(ChallengeKind.LINK_CONFIRM)
                    .status(ChallengeStatus.PENDING)
                    .tokenHash(UUID.randomUUID().toString())
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300));
                repos.challenges().save(challenge);

                Map<String, String> ph = new HashMap<>();
                ph.put("player", account.username());
                ph.put("uuid", accountUuid.toString());
                ph.put("discord", discordId);
                sendDiscord(
                    new DiscordMessageRequest()
                        .discordUserId(discordId)
                        .embed(new DiscordEmbedDto().templateKey("staff-reg").placeholders(ph))
                );
            });
    }

    private void evaluateTrust(ProxyPlayer player, StaffAccount account, String ip) {
        TrustSnapshot current = buildTrustSnapshot(ip);
        TrustSnapshot baseline = account.trust();
        DeviceRecord device = new DeviceRecord()
            .accountUuid(account.uuid())
            .ipHmac(current.ipHmac())
            .country(current.country())
            .region(current.region())
            .city(current.city())
            .vpnOrProxy(current.vpnOrProxy())
            .lastSeenAt(Instant.now());
        repos.devices().upsert(device);

        RiskContext ctx = new RiskContext()
            .detectCountry(config.detectCountry())
            .detectRegion(config.detectRegion())
            .detectCity(config.detectCity())
            .treatVpnAsChange(config.detectVpn())
            .storedFields(java.util.EnumSet.copyOf(config.trustStoreFields()));

        Set<RiskSignal> signals = riskEngine.evaluate(baseline, current, device, ctx);
        if (signals.isEmpty() || signals.contains(RiskSignal.NO_BASELINE)) {
            if (baseline == null) {
                account.trust(current.establishedAt(Instant.now()));
                repos.accounts().save(account);
            }
            applyRestrict(player.uuid(), false);
            publishSession(player.uuid(), true);
            return;
        }

        applyRestrict(player.uuid(), true);
        publishSession(player.uuid(), false);
        createLocationChallenge(player, account, current);
    }

    private void createLocationChallenge(ProxyPlayer player, StaffAccount account, TrustSnapshot current) {
        Challenge challenge = new Challenge()
            .id(UUID.randomUUID())
            .accountUuid(account.uuid())
            .kind(ChallengeKind.LOCATION_CHANGE)
            .status(ChallengeStatus.PENDING)
            .tokenHash(UUID.randomUUID().toString())
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(config.challengeTtlSeconds()));
        repos.challenges().save(challenge);

        Map<String, String> ph = new HashMap<>();
        ph.put("player", player.name());
        ph.put("uuid", player.uuid().toString());
        ph.put("country", current.country() == null ? "?" : current.country());
        ph.put("region", current.region() == null ? "?" : current.region());
        ph.put("city", current.city() == null ? "?" : current.city());
        ph.put("ip", maskIpHint(current.ipHmac()));

        DiscordPort port = discord;
        if (port == null) return;

        String discordId = account.discordId();
        if (discordId != null && !discordId.isBlank()) {
            port.sendDirectMessage(
                new DiscordMessageRequest()
                    .discordUserId(discordId)
                    .embed(new DiscordEmbedDto().templateKey("staff-auth").placeholders(ph))
            );
            return;
        }
        port.sendChannelMessage(
            new DiscordMessageRequest()
                .channelId(null)
                .embed(new DiscordEmbedDto().templateKey("staff-auth").placeholders(ph))
        );
    }

    private TrustSnapshot buildTrustSnapshot(String ip) {
        String hmac = StaffPasswordHasher.hmacIp(normalizeIp(ip), ipPepper);
        return new TrustSnapshot()
            .ipHmac(hmac)
            .country("?")
            .region("?")
            .city("?")
            .vpnOrProxy(false)
            .establishedAt(Instant.now());
    }

    private void handleSecurityBan(StaffAccount account) {
        account.accountStatus(AccountStatus.BANNED_SECURITY);
        account.updatedAt(Instant.now());
        repos.accounts().save(account);
        repos.sessions().revokeAll(account.uuid());
        applyRestrict(account.uuid(), true);
        publishSession(account.uuid(), false);

        Map<String, String> ph = new HashMap<>();
        ph.put("player", account.username());
        ph.put("uuid", account.uuid().toString());
        sendDiscord(new DiscordMessageRequest().embed(new DiscordEmbedDto().templateKey("security").placeholders(ph)));

        if (banCheck != null) {
            banCheck.invalidate(account.uuid());
        }
    }

    private CompletableFuture<Boolean> mutateStatus(
        UUID targetUuid,
        UUID actorUuid,
        AccountStatus status,
        String action
    ) {
        return repos
            .accounts()
            .findByUuid(targetUuid)
            .thenCompose(opt -> {
                if (opt.isEmpty()) return CompletableFuture.completedFuture(false);
                StaffAccount account = opt.get();
                account.accountStatus(status);
                account.updatedAt(Instant.now());
                return repos
                    .accounts()
                    .save(account)
                    .thenCompose(v -> audit(action, actorUuid, targetUuid, status.name()))
                    .thenApply(v -> {
                        applyRestrict(targetUuid, status != AccountStatus.ACTIVE);
                        return true;
                    });
            });
    }

    private boolean hasFreshStepUp(UUID accountUuid) {
        if (accountUuid == null) return false;
        Instant until = stepUpUntil.get(accountUuid);
        return until != null && until.isAfter(Instant.now());
    }

    private void grantStepUp(UUID accountUuid) {
        stepUpUntil.put(accountUuid, Instant.now().plusSeconds(config.stepUpTtlSeconds()));
    }

    private void issueStepUpChallenge(UUID accountUuid) {
        Challenge challenge = new Challenge()
            .id(UUID.randomUUID())
            .accountUuid(accountUuid)
            .kind(ChallengeKind.STEP_UP)
            .status(ChallengeStatus.PENDING)
            .tokenHash(UUID.randomUUID().toString())
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(config.stepUpTtlSeconds()));
        repos.challenges().save(challenge);
    }

    private boolean requiresStepUp(ProxyPlayer player, String root) {
        if (root.isBlank()) return false;
        for (String blocked : config.stepUpCommands()) {
            if (root.equalsIgnoreCase(blocked)) return true;
        }
        for (String perm : config.stepUpPermissions()) {
            if ("*".equals(perm) && player.hasPermission("*")) return true;
            if (player.hasPermission(perm)) return true;
        }
        return false;
    }

    private void applyRestrict(UUID uuid, boolean restrict) {
        if (restrict) restricted.add(uuid);
        else restricted.remove(uuid);
    }

    private void publishSession(UUID uuid, boolean approved) {
        if (redis == null || !redis.isConnected()) return;
        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.UUID, uuid.toString());
        data.addProperty(NetworkEventKeys.APPROVED, approved);
        redis.publish(new RedisEvent(ProxyEventType.STAFF_AUTH_SESSION.id(), proxyId, data));
    }

    private void onDiscordLinkRemote(us.iorilabs.proxy.redis.RedisEvent event) {
        if (event == null || event.data() == null) return;
        String uuidRaw = event.data().has(NetworkEventKeys.UUID)
            ? event.data().get(NetworkEventKeys.UUID).getAsString()
            : null;
        String discordId = event.data().has(NetworkEventKeys.DISCORD_ID)
            ? event.data().get(NetworkEventKeys.DISCORD_ID).getAsString()
            : null;
        if (uuidRaw == null || discordId == null) return;
        try {
            UUID uuid = UUID.fromString(uuidRaw);
            repos
                .accounts()
                .findByUuid(uuid)
                .thenAccept(opt -> {
                    if (opt.isEmpty()) return;
                    StaffAccount account = opt.get();
                    account.discordId(discordId);
                    account.linkStatus(LinkStatus.LINKED);
                    account.updatedAt(Instant.now());
                    repos.accounts().save(account);
                });
        } catch (IllegalArgumentException ignored) {}
    }

    private CompletableFuture<Void> audit(String action, UUID actor, UUID target, String summary) {
        AuditEntry entry = new AuditEntry()
            .id(UUID.randomUUID())
            .action(action)
            .actorUuid(actor)
            .targetUuid(target)
            .summary(summary == null ? "" : summary)
            .at(Instant.now());
        return repos.audit().append(entry);
    }

    private void notifyCreated(StaffAccount account) {
        Map<String, String> ph = new HashMap<>();
        ph.put("player", account.username());
        ph.put("uuid", account.uuid().toString());
        sendDiscord(new DiscordMessageRequest().embed(new DiscordEmbedDto().templateKey("staff-reg").placeholders(ph)));
    }

    private void sendDiscord(DiscordMessageRequest request) {
        DiscordPort port = discord;
        if (port == null) return;
        port.sendChannelMessage(request);
    }

    private static String rootLabel(String commandLine) {
        String trimmed = commandLine.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        int space = trimmed.indexOf(' ');
        if (space >= 0) trimmed = trimmed.substring(0, space);
        int colon = trimmed.indexOf(':');
        if (colon > 0) trimmed = trimmed.substring(colon + 1);
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static String normalizeIp(String ip) {
        if (ip == null) return "";
        int zone = ip.indexOf('%');
        return zone >= 0 ? ip.substring(0, zone) : ip.trim();
    }

    private static String maskIpHint(String hmac) {
        if (hmac == null || hmac.length() < 8) return "****";
        return "****" + hmac.substring(hmac.length() - 4);
    }

    public record CreateResult(boolean success, String messageKey, UUID uuid, String username) {
        static CreateResult ok(UUID uuid, String username) {
            return new CreateResult(true, "staffreg.create-ok", uuid, username);
        }

        static CreateResult invalid(String key) {
            return new CreateResult(false, key, null, null);
        }
    }

    public record StubResult(boolean ok, String messageKey) {
        static StubResult needsSecondAdmin(String key) {
            return new StubResult(false, key);
        }
    }
}
