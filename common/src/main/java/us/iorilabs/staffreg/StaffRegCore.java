package us.iorilabs.staffreg;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.staffreg.StaffRegEnums.AccountStatus;
import us.iorilabs.staffreg.StaffRegEnums.AuthMode;
import us.iorilabs.staffreg.StaffRegEnums.ButtonAction;
import us.iorilabs.staffreg.StaffRegEnums.ChallengeKind;
import us.iorilabs.staffreg.StaffRegEnums.ChallengeStatus;
import us.iorilabs.staffreg.StaffRegEnums.FailAction;
import us.iorilabs.staffreg.StaffRegEnums.LinkStatus;
import us.iorilabs.staffreg.StaffRegEnums.RiskSignal;
import us.iorilabs.staffreg.crypto.ButtonIdSigner;
import us.iorilabs.staffreg.crypto.OtcGenerator;
import us.iorilabs.staffreg.crypto.StaffPasswordHasher;

public final class StaffRegCore {

    private final StaffAccountRepo accounts;
    private final SessionStore sessions;
    private final ChallengeStore challenges;
    private final DeviceRepo devices;
    private final AuditLog audit;
    private final RiskEngine riskEngine;
    private final StaffRegSettings settings;
    private final Map<UUID, Deque<Long>> passwordFails = new ConcurrentHashMap<>();
    private final Map<String, OtcEntry> pendingOtc = new ConcurrentHashMap<>();

    public StaffRegCore(
        StaffAccountRepo accounts,
        SessionStore sessions,
        ChallengeStore challenges,
        DeviceRepo devices,
        AuditLog audit,
        RiskEngine riskEngine,
        StaffRegSettings settings
    ) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.challenges = challenges;
        this.devices = devices;
        this.audit = audit;
        this.riskEngine = riskEngine == null ? new us.iorilabs.staffreg.impl.DefaultRiskEngine() : riskEngine;
        this.settings = settings == null ? new StaffRegSettings() : settings;
    }

    public CompletableFuture<StaffAccount> createAccount(UUID actorUuid, String username, String password) {
        if (username == null || username.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("username"));
        }
        if (password == null || password.length() < settings.passwordMinLength()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("password-too-short"));
        }
        String nick = username.trim().toLowerCase(Locale.ROOT);
        return accounts.findByUsername(nick).thenCompose(existing -> {
            if (existing.isPresent()) {
                return CompletableFuture.failedFuture(new IllegalStateException("username-taken"));
            }
            Instant now = Instant.now();
            StaffAccount account = new StaffAccount()
                .uuid(UUID.randomUUID())
                .username(nick)
                .passwordHash(StaffPasswordHasher.hash(password))
                .mustChangePassword(settings.mustChangeOnFirstLogin())
                .linkStatus(LinkStatus.UNLINKED)
                .accountStatus(AccountStatus.ACTIVE)
                .preferredAuthMode(AuthMode.DISCORD)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actorUuid);
            return accounts
                .save(account)
                .thenCompose(v ->
                    audit(actorUuid, account.uuid(), "STAFFREG_CREATE", "created staff " + nick).thenApply(x -> account)
                );
        });
    }

    public CompletableFuture<String> beginOtcLink(UUID accountUuid) {
        return accounts.findByUuid(accountUuid).thenCompose(opt -> {
            if (opt.isEmpty()) return CompletableFuture.failedFuture(new IllegalStateException("no-account"));
            String code = OtcGenerator.generate(12);
            String hash = OtcGenerator.hash(code);
            pendingOtc.put(
                hash,
                new OtcEntry(accountUuid, Instant.now().plusSeconds(Math.max(60, settings.syncCodeTtlSeconds())))
            );
            return audit(accountUuid, accountUuid, "STAFFREG_OTC_ISSUED", "otc issued").thenApply(v -> code);
        });
    }

    public CompletableFuture<StaffAccount> redeemOtc(String code, String discordId) {
        if (code == null || code.isBlank() || discordId == null || discordId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("otc"));
        }
        String hash = OtcGenerator.hash(code);
        OtcEntry entry = pendingOtc.remove(hash);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return CompletableFuture.failedFuture(new IllegalStateException("otc-invalid"));
        }
        CompletableFuture<Optional<StaffAccount>> byDiscord = settings.oneDiscordPerAccount()
            ? accounts.findByDiscordId(discordId)
            : CompletableFuture.completedFuture(Optional.empty());
        return byDiscord.thenCompose(existingDiscord -> {
            if (existingDiscord.isPresent() && !existingDiscord.get().uuid().equals(entry.accountUuid())) {
                return CompletableFuture.failedFuture(new IllegalStateException("discord-taken"));
            }
            return accounts.findByUuid(entry.accountUuid()).thenCompose(opt -> {
                if (opt.isEmpty()) return CompletableFuture.failedFuture(new IllegalStateException("no-account"));
                StaffAccount account = opt.get();
                account.discordId(discordId);
                account.linkStatus(LinkStatus.LINKED);
                account.updatedAt(Instant.now());
                return accounts
                    .save(account)
                    .thenCompose(v ->
                        audit(account.uuid(), account.uuid(), "STAFFREG_LINKED", "discord linked").thenApply(
                            x -> account
                        )
                    );
            });
        });
    }

    public CompletableFuture<Challenge> adminAssignDiscord(
        UUID actorUuid,
        UUID targetUuid,
        String discordId,
        String buttonSecret
    ) {
        if (!settings.adminAssignEnabled()) {
            return CompletableFuture.failedFuture(new IllegalStateException("admin-assign-disabled"));
        }
        if (discordId == null || discordId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("discord"));
        }
        return accounts.findByUuid(targetUuid).thenCompose(opt -> {
            if (opt.isEmpty()) return CompletableFuture.failedFuture(new IllegalStateException("no-account"));
            StaffAccount account = opt.get();
            account.discordId(discordId);
            account.linkStatus(
                settings.adminAssignRequireOwnerConfirm() ? LinkStatus.PENDING_CONFIRM : LinkStatus.LINKED
            );
            account.updatedAt(Instant.now());
            String token = OtcGenerator.generate(16);
            Challenge challenge = new Challenge()
                .id(UUID.randomUUID())
                .accountUuid(targetUuid)
                .kind(ChallengeKind.ADMIN_ASSIGN)
                .status(ChallengeStatus.PENDING)
                .tokenHash(ButtonIdSigner.hashToken(token))
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(Math.max(60, settings.adminAssignConfirmTtlSeconds())));
            return accounts
                .save(account)
                .thenCompose(v -> challenges.save(challenge))
                .thenCompose(v ->
                    audit(actorUuid, targetUuid, "STAFFREG_ADMIN_ASSIGN", "discord assigned pending confirm")
                )
                .thenApply(v -> {
                    challenge.tokenHash(token);
                    return challenge;
                });
        });
    }

    public CompletableFuture<LoginResult> loginPassword(
        UUID accountUuid,
        String password,
        TrustSnapshot observed,
        String serverId,
        String proxyId
    ) {
        return accounts.findByUuid(accountUuid).thenCompose(opt -> {
            if (opt.isEmpty()) return CompletableFuture.completedFuture(LoginResult.fail("no-account"));
            StaffAccount account = opt.get();
            if (
                account.accountStatus() == AccountStatus.BANNED_SECURITY ||
                account.accountStatus() == AccountStatus.REVOKED ||
                account.accountStatus() == AccountStatus.LOCKED
            ) {
                return CompletableFuture.completedFuture(LoginResult.fail("locked"));
            }
            if (!StaffPasswordHasher.verify(password, account.passwordHash())) {
                return registerFail(account).thenApply(action ->
                    LoginResult.fail(action == FailAction.BAN ? "banned" : "bad-password")
                );
            }
            passwordFails.remove(accountUuid);
            if (account.mustChangePassword()) {
                return CompletableFuture.completedFuture(LoginResult.mustChange(account));
            }
            return establishSession(account, observed, AuthMode.PASSWORD, serverId, proxyId);
        });
    }

    public CompletableFuture<Void> changePassword(UUID accountUuid, String newPassword) {
        if (newPassword == null || newPassword.length() < settings.passwordMinLength()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("password-too-short"));
        }
        return accounts.findByUuid(accountUuid).thenCompose(opt -> {
            if (opt.isEmpty()) return CompletableFuture.failedFuture(new IllegalStateException("no-account"));
            StaffAccount account = opt.get();
            account.passwordHash(StaffPasswordHasher.hash(newPassword));
            account.mustChangePassword(false);
            account.updatedAt(Instant.now());
            return accounts
                .save(account)
                .thenCompose(v -> audit(accountUuid, accountUuid, "STAFFREG_PASSWORD_CHANGE", "password changed"));
        });
    }

    public CompletableFuture<JoinDecision> evaluateJoin(
        StaffAccount account,
        TrustSnapshot observed,
        DeviceRecord device
    ) {
        if (account == null) {
            return CompletableFuture.completedFuture(JoinDecision.deny("no-account"));
        }
        if (account.accountStatus() != AccountStatus.ACTIVE) {
            return CompletableFuture.completedFuture(JoinDecision.deny(account.accountStatus().name()));
        }
        RiskContext context = new RiskContext()
            .detectCountry(settings.detectCountry())
            .detectRegion(settings.detectRegion())
            .detectCity(settings.detectCity())
            .treatVpnAsChange(settings.detectVpnProxy())
            .storedFields(settings.trustStore());
        Set<RiskSignal> signals = riskEngine.evaluate(account.trust(), observed, device, context);
        if (signals.contains(RiskSignal.NO_BASELINE)) {
            return establishTrust(account, observed).thenApply(v -> JoinDecision.ok(account, true));
        }
        if (signals.isEmpty()) {
            return CompletableFuture.completedFuture(JoinDecision.ok(account, false));
        }
        return sessions
            .revokeAll(account.uuid())
            .thenCompose(v -> createLocationChallenge(account))
            .thenApply(challenge -> JoinDecision.challenge(account, challenge, signals));
    }

    public CompletableFuture<Boolean> hasFreshStepUp(StaffSession session) {
        if (session == null || !session.stepUpGranted()) return CompletableFuture.completedFuture(false);
        Instant exp = session.stepUpExpiresAt();
        if (exp == null || exp.isBefore(Instant.now())) return CompletableFuture.completedFuture(false);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Challenge> requestStepUp(UUID accountUuid) {
        String token = OtcGenerator.generate(16);
        Challenge challenge = new Challenge()
            .id(UUID.randomUUID())
            .accountUuid(accountUuid)
            .kind(ChallengeKind.STEP_UP)
            .status(ChallengeStatus.PENDING)
            .tokenHash(ButtonIdSigner.hashToken(token))
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(Math.max(30, settings.stepUpTtlSeconds())));
        return challenges.save(challenge).thenApply(v -> {
            challenge.tokenHash(token);
            return challenge;
        });
    }

    public CompletableFuture<StaffSession> grantStepUp(StaffSession session) {
        if (session == null) return CompletableFuture.failedFuture(new IllegalArgumentException("session"));
        session.stepUpGranted(true);
        session.stepUpExpiresAt(Instant.now().plusSeconds(Math.max(30, settings.stepUpTtlSeconds())));
        return sessions.save(session).thenApply(v -> session);
    }

    public CompletableFuture<Boolean> resolveButton(
        ButtonAction action,
        UUID challengeId,
        String token,
        UUID actorUuid
    ) {
        if (action == null || challengeId == null || token == null) {
            return CompletableFuture.completedFuture(false);
        }
        String hash = ButtonIdSigner.hashToken(token);
        return challenges.findById(challengeId).thenCompose(opt -> {
            if (opt.isEmpty()) return CompletableFuture.completedFuture(false);
            Challenge challenge = opt.get();
            if (challenge.status() != ChallengeStatus.PENDING) return CompletableFuture.completedFuture(false);
            if (challenge.expiresAt() != null && challenge.expiresAt().isBefore(Instant.now())) {
                challenge.status(ChallengeStatus.EXPIRED);
                return challenges.save(challenge).thenApply(v -> false);
            }
            return challenges.consumeToken(challengeId, hash).thenCompose(ok -> {
                if (!ok) return CompletableFuture.completedFuture(false);
                ChallengeStatus status = switch (action) {
                    case APPROVE, CONFIRM_LINK -> ChallengeStatus.APPROVED;
                    case DENY -> ChallengeStatus.DENIED;
                    case NOT_ME -> ChallengeStatus.REPORTED_NOT_ME;
                    case UNBAN -> ChallengeStatus.APPROVED;
                };
                challenge.status(status);
                challenge.resolvedAt(Instant.now());
                challenge.resolvedByUuid(actorUuid);
                CompletableFuture<Void> after = challenges.save(challenge);
                if (action == ButtonAction.CONFIRM_LINK || action == ButtonAction.APPROVE) {
                    after = after.thenCompose(v -> onChallengeApproved(challenge));
                }
                if (action == ButtonAction.NOT_ME) {
                    after = after.thenCompose(v -> onNotMe(challenge));
                }
                return after.thenApply(v -> true);
            });
        });
    }

    public boolean requiresStepUpCommand(String rootCommand) {
        if (rootCommand == null || rootCommand.isBlank()) return false;
        String root = rootCommand.toLowerCase(Locale.ROOT);
        for (String cmd : settings.stepUpCommands()) {
            if (root.equalsIgnoreCase(cmd)) return true;
        }
        return false;
    }

    public StaffRegSettings settings() {
        return settings;
    }

    private CompletableFuture<LoginResult> establishSession(
        StaffAccount account,
        TrustSnapshot observed,
        AuthMode mode,
        String serverId,
        String proxyId
    ) {
        CompletableFuture<Void> trustFuture =
            account.trust() == null ? establishTrust(account, observed) : CompletableFuture.completedFuture(null);
        return trustFuture.thenCompose(v -> {
            StaffSession session = new StaffSession()
                .sessionId(UUID.randomUUID())
                .accountUuid(account.uuid())
                .proxyId(proxyId)
                .serverId(serverId)
                .authMode(mode)
                .authenticated(true)
                .stepUpGranted(false)
                .createdAt(Instant.now())
                .expiresAt(null);
            return sessions
                .save(session)
                .thenCompose(x ->
                    audit(account.uuid(), account.uuid(), "STAFFREG_LOGIN", "login " + mode.name()).thenApply(y ->
                        LoginResult.ok(account, session)
                    )
                );
        });
    }

    private CompletableFuture<Void> establishTrust(StaffAccount account, TrustSnapshot observed) {
        if (observed == null) return CompletableFuture.completedFuture(null);
        Instant now = Instant.now();
        TrustSnapshot trust = new TrustSnapshot()
            .ipHmac(observed.ipHmac())
            .country(observed.country())
            .region(observed.region())
            .city(observed.city())
            .vpnOrProxy(observed.vpnOrProxy())
            .establishedAt(now);
        if (settings.trustExpireDays() > 0) {
            trust.expiresAt(now.plus(Duration.ofDays(settings.trustExpireDays())));
        }
        account.trust(trust);
        account.updatedAt(now);
        DeviceRecord device = new DeviceRecord()
            .id(UUID.randomUUID())
            .accountUuid(account.uuid())
            .ipHmac(observed.ipHmac())
            .country(observed.country())
            .region(observed.region())
            .city(observed.city())
            .vpnOrProxy(observed.vpnOrProxy())
            .firstSeenAt(now)
            .lastSeenAt(now);
        return accounts.save(account).thenCompose(v -> devices.upsert(device));
    }

    private CompletableFuture<Challenge> createLocationChallenge(StaffAccount account) {
        String token = OtcGenerator.generate(16);
        Challenge challenge = new Challenge()
            .id(UUID.randomUUID())
            .accountUuid(account.uuid())
            .kind(ChallengeKind.LOCATION_CHANGE)
            .status(ChallengeStatus.PENDING)
            .tokenHash(ButtonIdSigner.hashToken(token))
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(Math.max(30, settings.onChangeChallengeTtlSeconds())));
        return challenges.save(challenge).thenCompose(v ->
            audit(account.uuid(), account.uuid(), "STAFFREG_LOCATION_CHALLENGE", "location change").thenApply(x -> {
                challenge.tokenHash(token);
                return challenge;
            })
        );
    }

    private CompletableFuture<Void> onChallengeApproved(Challenge challenge) {
        if (challenge.kind() == ChallengeKind.ADMIN_ASSIGN || challenge.kind() == ChallengeKind.LINK_CONFIRM) {
            return accounts.findByUuid(challenge.accountUuid()).thenCompose(opt -> {
                if (opt.isEmpty()) return CompletableFuture.completedFuture(null);
                StaffAccount account = opt.get();
                account.linkStatus(LinkStatus.LINKED);
                account.updatedAt(Instant.now());
                return accounts.save(account);
            });
        }
        if (challenge.kind() == ChallengeKind.LOCATION_CHANGE) {
            return sessions.listForAccount(challenge.accountUuid()).thenCompose(list -> {
                CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
                for (StaffSession session : list) {
                    session.authenticated(true);
                    chain = chain.thenCompose(v -> sessions.save(session));
                }
                return chain;
            });
        }
        if (challenge.kind() == ChallengeKind.STEP_UP) {
            return sessions.listForAccount(challenge.accountUuid()).thenCompose(list -> {
                if (list.isEmpty()) return CompletableFuture.completedFuture(null);
                return grantStepUp(list.get(0)).thenApply(s -> null);
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> onNotMe(Challenge challenge) {
        return sessions.revokeAll(challenge.accountUuid()).thenCompose(v ->
            accounts.findByUuid(challenge.accountUuid()).thenCompose(opt -> {
                if (opt.isEmpty()) return CompletableFuture.completedFuture(null);
                StaffAccount account = opt.get();
                account.accountStatus(AccountStatus.LOCKED);
                account.updatedAt(Instant.now());
                return accounts
                    .save(account)
                    .thenCompose(x ->
                        audit(challenge.accountUuid(), challenge.accountUuid(), "STAFFREG_NOT_ME", "reported not me")
                    );
            })
        );
    }

    private CompletableFuture<FailAction> registerFail(StaffAccount account) {
        long now = System.currentTimeMillis();
        long window = Math.max(60L, settings.failsWindowSeconds()) * 1000L;
        Deque<Long> fails = passwordFails.computeIfAbsent(account.uuid(), id -> new ArrayDeque<>());
        synchronized (fails) {
            while (!fails.isEmpty() && now - fails.peekFirst() > window) fails.pollFirst();
            fails.addLast(now);
            if (fails.size() < Math.max(1, settings.failsMax())) {
                return audit(account.uuid(), account.uuid(), "STAFFREG_LOGIN_FAIL", "fail " + fails.size()).thenApply(
                    v -> FailAction.NONE
                );
            }
        }
        FailAction action = settings.failsAction() == null ? FailAction.BAN : settings.failsAction();
        account.accountStatus(action == FailAction.LOCK ? AccountStatus.LOCKED : AccountStatus.BANNED_SECURITY);
        account.updatedAt(Instant.now());
        CompletableFuture<Void> chain = accounts.save(account);
        if (settings.failsRevokeSessions()) {
            chain = chain.thenCompose(v -> sessions.revokeAll(account.uuid()));
        }
        return chain
            .thenCompose(v -> audit(account.uuid(), account.uuid(), "STAFFREG_FAILS_ACTION", action.name()))
            .thenApply(v -> action);
    }

    private CompletableFuture<Void> audit(UUID actor, UUID target, String action, String summary) {
        AuditEntry entry = new AuditEntry()
            .id(UUID.randomUUID())
            .actorUuid(actor)
            .targetUuid(target)
            .action(action)
            .summary(summary)
            .at(Instant.now());
        return audit.append(entry);
    }

    private record OtcEntry(UUID accountUuid, Instant expiresAt) {}

    public record LoginResult(
        boolean ok,
        boolean mustChangePassword,
        String error,
        StaffAccount account,
        StaffSession session
    ) {
        static LoginResult ok(StaffAccount account, StaffSession session) {
            return new LoginResult(true, false, null, account, session);
        }

        static LoginResult mustChange(StaffAccount account) {
            return new LoginResult(false, true, "must-change", account, null);
        }

        static LoginResult fail(String error) {
            return new LoginResult(false, false, error, null, null);
        }
    }

    public record JoinDecision(
        boolean allowed,
        boolean firstTrust,
        boolean needsChallenge,
        StaffAccount account,
        Challenge challenge,
        Set<RiskSignal> signals,
        String denyReason
    ) {
        static JoinDecision ok(StaffAccount account, boolean firstTrust) {
            return new JoinDecision(true, firstTrust, false, account, null, Set.of(), null);
        }

        static JoinDecision challenge(StaffAccount account, Challenge challenge, Set<RiskSignal> signals) {
            return new JoinDecision(false, false, true, account, challenge, signals, null);
        }

        static JoinDecision deny(String reason) {
            return new JoinDecision(false, false, false, null, null, Set.of(), reason);
        }
    }
}
