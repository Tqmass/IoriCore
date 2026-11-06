package us.iorilabs.proxy.discord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import us.iorilabs.proxy.config.DiscordConfig;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.discord.enums.DiscordChannelKey;
import us.iorilabs.proxy.discord.enums.DiscordPunishmentType;
import us.iorilabs.proxy.discord.link.DiscordLinkSql;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.maintenance.MaintenanceService;
import us.iorilabs.proxy.platform.BackendServer;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.punishment.BanCheckService;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.staffauth.StaffAuthService;
import us.iorilabs.proxy.staffchat.StaffChatService;
import us.iorilabs.proxy.webhooks.WebhookFormat;
import us.iorilabs.proxy.webhooks.WebhookRegistry;

@Getter
@Accessors(fluent = true)
public final class DiscordBotService {

    private static final String LEGACY_STAFF_AUTH_PREFIX = "staffauth:approve:";

    private final ProxyPlatform platform;
    private final ProxyLogger logger;
    private final LanguageManager language;
    private final WebhookRegistry webhooks;
    private final MaintenanceService maintenance;
    private final StorageService storage;
    private final RedisService redis;
    private final BanCheckService banCheck;
    private final String proxyId;

    private volatile DiscordConfig config;
    private volatile StaffChatService staffChat;
    private volatile StaffAuthService staffAuth;
    private volatile DiscordPunishmentHandler punishments;
    private volatile JDA jda;
    private final AtomicBoolean starting = new AtomicBoolean(false);
    private final DiscordLinkSql linkSql;
    private final ConcurrentHashMap<String, String> staffChatWebhooks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> staffAuthNotifyMessages = new ConcurrentHashMap<>();

    public DiscordBotService(
        ProxyPlatform platform,
        ProxyLogger logger,
        LanguageManager language,
        WebhookRegistry webhooks,
        MaintenanceService maintenance,
        StorageService storage,
        RedisService redis,
        BanCheckService banCheck,
        String proxyId,
        DiscordConfig config
    ) {
        this.platform = platform;
        this.logger = logger;
        this.language = language;
        this.webhooks = webhooks;
        this.maintenance = maintenance;
        this.storage = storage;
        this.redis = redis;
        this.banCheck = banCheck;
        this.proxyId = proxyId;
        this.config = config;
        this.punishments = new DiscordPunishmentHandler(platform, storage, redis, banCheck, logger, proxyId);
        this.linkSql = new DiscordLinkSql(storage, logger);
    }

    public void bindStaffChat(StaffChatService staffChat) {
        this.staffChat = staffChat;
    }

    public void bindStaffAuth(StaffAuthService staffAuth) {
        this.staffAuth = staffAuth;
    }

    public void start() {
        stop();
        DiscordConfig cfg = config;
        if (cfg == null || !cfg.enabled()) {
            logger.module("Discord", "bot disabled");
            return;
        }
        if (!cfg.hasToken()) {
            logger.warn("Discord bot enabled but TOKEN is empty — fail soft");
            return;
        }
        if (!starting.compareAndSet(false, true)) return;

        platform.asyncExecutor().execute(() -> {
            try {
                JDABuilder builder = JDABuilder.createDefault(cfg.token())
                    .enableIntents(
                        EnumSet.of(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_MODERATION
                        )
                    )
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setChunkingFilter(ChunkingFilter.NONE)
                    .addEventListeners(new DiscordGatewayListener(this));

                JDA built = builder.build();
                built.awaitReady();
                this.jda = built;
                registerSlashCommands(built, cfg);
                startStatusTimer();
                logger.module(
                    "Discord",
                    "JDA ready (&7welcome/goodbye = guild members only; MC join/quit not mirrored&r)"
                );
            } catch (Exception ex) {
                this.jda = null;
                logger.warn("Discord bot failed to start (fail soft): " + ex.getMessage());
            } finally {
                starting.set(false);
            }
        });
    }

    public void reload(DiscordConfig config) {
        this.config = config;
        this.staffChatWebhooks.clear();
        this.staffAuthNotifyMessages.clear();
        this.punishments = new DiscordPunishmentHandler(platform, storage, redis, banCheck, logger, proxyId);
        start();
    }

    public void stop() {
        JDA current = this.jda;
        this.jda = null;
        if (current == null) return;

        try {
            current.shutdown();
        } catch (Exception ex) {
            logger.warn("Discord bot shutdown: " + ex.getMessage());
        }
    }

    public boolean ready() {
        return jda != null && config != null && config.enabled();
    }

    public void mirrorWebhook(String name, Map<String, String> placeholders) {
        DiscordConfig cfg = config;
        if (!ready() || cfg == null || !cfg.sendWebhookEvents()) return;

        String key = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if ("staff-auth".equals(key)) return;

        if (
            ("donations".equals(key) || "donation".equals(key)) &&
            (!cfg.store().enabled() || !cfg.store().donationsModule())
        ) {
            return;
        }

        String channelKey = switch (key) {
            case "donations", "donation" -> DiscordChannelKey.STORE.id();
            case "status" -> DiscordChannelKey.STATUS.id();
            case "stream", "video", "reports", "request", "maintenance", "blacklist" -> DiscordChannelKey.STATUS.id();
            default -> key;
        };

        WebhookFormat format = webhooks.format(key).orElse(null);
        if (format != null) {
            sendEmbed(channelKey, DiscordEmbeds.fromWebhookFormat(format, placeholders));
            return;
        }

        if (DiscordChannelKey.STORE.id().equals(channelKey) && cfg.store().enabled() && cfg.store().donationsModule()) {
            sendTemplate(channelKey, "store", placeholders);
        }
    }

    public void onGuildMemberJoin(Member member) {
        if (member == null || member.getUser().isBot() || !ready()) return;
        DiscordConfig cfg = config;
        if (cfg == null || !cfg.welcome().enabled()) return;

        var guild = member.getGuild();
        Map<String, String> ph = new LinkedHashMap<>();
        ph.put("user", member.getEffectiveName());
        ph.put("mention", member.getAsMention());
        ph.put("guild", guild.getName());
        ph.put("members", String.valueOf(guild.getMemberCount()));
        ph.put("player", member.getEffectiveName());
        sendTemplate(DiscordChannelKey.WELCOME.id(), "welcome", ph);
    }

    public void onGuildMemberRemove(Member member) {
        if (member == null || member.getUser().isBot() || !ready()) return;
        DiscordConfig cfg = config;
        if (cfg == null || !cfg.goodbye().enabled()) return;

        Map<String, String> ph = new LinkedHashMap<>();
        ph.put("user", member.getEffectiveName());
        ph.put("mention", member.getUser().getAsMention());
        ph.put("guild", member.getGuild().getName());
        ph.put("members", String.valueOf(member.getGuild().getMemberCount()));
        ph.put("player", member.getEffectiveName());
        sendTemplate(DiscordChannelKey.GOODBYE.id(), "goodbye", ph);
    }

    public void onStaffChatFromMinecraft(String player, String rank, String server, String message) {
        if (!ready() || message == null || message.isBlank()) return;
        DiscordConfig cfg = config;
        if (cfg == null || !cfg.staffChat().enabled()) return;

        Map<String, String> ph = new LinkedHashMap<>();
        ph.put("player", player == null ? "?" : player);
        ph.put("rank", rank == null ? "" : rank);
        ph.put("server", server == null ? "?" : server);
        ph.put("message", message);

        String webhookUrl = cfg.staffChat().webhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            sendTemplate(DiscordChannelKey.STAFF_CHAT.id(), "staff-chat", ph);
            return;
        }

        String format = cfg.staffChat().format();
        if (format == null || format.isBlank()) format = "**[%server%]** %message%";
        DiscordWebhookExecutor.sendContent(
            platform,
            logger,
            webhookUrl,
            cfg.staffChat().username(),
            cfg.staffChat().avatarUrl(),
            format,
            ph
        );
    }

    public void relayDiscordStaffChat(String discordName, String avatarUrl, String message) {
        if (staffChat == null || language == null) return;

        String formatted = language.raw(
            "staffchat.format-discord",
            Map.of(
                "player",
                discordName == null ? "Discord" : discordName,
                "message",
                message == null ? "" : message,
                "rank",
                "Discord",
                "server",
                "Discord"
            )
        );
        if (
            formatted == null ||
            formatted.isBlank() ||
            formatted.equalsIgnoreCase("STAFFCHAT.FORMAT-DISCORD") ||
            formatted.equalsIgnoreCase("staffchat.format-discord")
        ) {
            formatted =
                "&8[&dSC&8] &5DISCORD &e" +
                (discordName == null ? "?" : discordName) +
                " &8» &f" +
                (message == null ? "" : message);
        }

        for (ProxyPlayer online : platform.onlinePlayers()) {
            if (online.hasPermission("ioriproxy.staffchat.receive") || online.hasPermission("ioriproxy.staff")) {
                online.sendMessage(formatted);
            }
        }
    }

    public void emitBoost(String booster, String avatarUrl, int totalBoosts, String guildName) {
        DiscordConfig cfg = config;
        if (cfg == null || !cfg.boost().enabled()) return;

        Map<String, String> ph = new LinkedHashMap<>();
        ph.put("booster", booster == null ? "?" : booster);
        ph.put("player", booster == null ? "?" : booster);
        ph.put("boosts", String.valueOf(totalBoosts));
        ph.put("guild", guildName == null ? "?" : guildName);
        sendTemplate(DiscordChannelKey.BOOST.id(), "boost", ph);
    }

    public String sendStaffRegMessage(us.iorilabs.staffreg.DiscordMessageRequest request, boolean direct) {
        if (!ready() || request == null) return "";

        var embed = us.iorilabs.proxy.staffreg.StaffRegDiscordPort.toJdaEmbed(config, request);
        var buttons = us.iorilabs.proxy.staffreg.StaffRegDiscordPort.toButtons(request);

        if (direct && request.discordUserId() != null && !request.discordUserId().isBlank()) {
            jda.retrieveUserById(request.discordUserId()).queue(
                user ->
                    user.openPrivateChannel().queue(
                        channel -> {
                            var action = channel.sendMessageEmbeds(embed);
                            if (buttons.length > 0) action.setActionRow(buttons);
                            action.queue(null, err -> logger.warn("StaffReg DM failed"));
                        },
                        err -> logger.warn("StaffReg DM open failed")
                    ),
                err -> logger.warn("StaffReg user lookup failed")
            );
            return "queued";
        }

        String channelId = request.channelId();
        if (channelId == null || channelId.isBlank()) {
            channelId = config.channel("staff-auth");
        }
        TextChannel channel = channelId == null || channelId.isBlank() ? null : jda.getTextChannelById(channelId);
        if (channel == null) return "";
        var action = channel.sendMessageEmbeds(embed);
        if (buttons.length > 0) action.setActionRow(buttons);
        action.queue(null, err -> logger.warn("StaffReg channel send failed"));
        return "queued";
    }

    public void emitStaffAuth(Map<String, String> placeholders) {
        if (!ready()) return;
        DiscordConfig cfg = config;
        if (cfg == null || cfg.staffAuthFlow() == null || !cfg.staffAuthFlow().enabled()) return;

        String token = placeholders == null ? "" : placeholders.getOrDefault("token", "");
        if (token == null || token.isBlank()) return;

        DiscordConfig.EmbedTemplate notifyT = cfg.embed("staff-auth-notify");
        if (isBlankEmbed(notifyT)) {
            notifyT = cfg.embed("staff-auth");
        }
        MessageEmbed notifyEmbed = DiscordEmbeds.fromTemplate(notifyT, placeholders);

        String notifyChannelId = cfg.staffAuthFlow().notifyChannelId();
        if (notifyChannelId == null || notifyChannelId.isBlank()) {
            notifyChannelId = cfg.channel(DiscordChannelKey.STAFF_AUTH.id());
        }
        TextChannel notifyChannel =
            notifyChannelId == null || notifyChannelId.isBlank() ? null : jda.getTextChannelById(notifyChannelId);
        if (notifyChannel != null) {
            notifyChannel
                .sendMessageEmbeds(notifyEmbed)
                .queue(
                    msg -> staffAuthNotifyMessages.put(token.trim().toLowerCase(Locale.ROOT), msg.getIdLong()),
                    err -> logger.warn("Discord staff-auth notify send failed")
                );
        }

        if (!cfg.staffAuthFlow().dmEnabled() || staffAuth == null) return;

        String secret = buttonSecret(cfg);
        if (secret == null || secret.isBlank()) {
            logger.warn("Staff-auth DM buttons skipped (set BRIDGE secret env for signed customId)");
            return;
        }

        Optional<UUID> targetUuid = staffAuth.pendingTargetUuid(token);
        if (targetUuid.isEmpty()) return;

        String discordUserId = linkSql.discordId(targetUuid.get()).orElse("");
        if (discordUserId.isBlank()) {
            logger.module("StaffAuth", "DM buttons skipped (no discord link for pending target)");
            return;
        }

        long expiresAt = staffAuth
            .pendingExpiresAtMillis(token)
            .orElse(System.currentTimeMillis() + (long) staffAuth.config().tokenTtlSeconds() * 1000L);
        String approveId = StaffAuthButtonCodec.encodeApprove(token, secret, expiresAt, discordUserId);
        String denyId = StaffAuthButtonCodec.encodeDeny(token, secret, expiresAt, discordUserId);
        if (approveId.isBlank() || denyId.isBlank()) return;

        MessageEmbed dmEmbed = DiscordEmbeds.fromTemplate(cfg.embed("staff-auth"), placeholders);
        jda.retrieveUserById(discordUserId).queue(
            user ->
                user.openPrivateChannel().queue(
                    ch ->
                        ch
                            .sendMessageEmbeds(dmEmbed)
                            .setActionRow(Button.success(approveId, "Approve"), Button.danger(denyId, "Deny"))
                            .queue(null, err -> logger.warn("Staff-auth DM delivery failed (closed DMs?)")),
                    err -> logger.warn("Staff-auth DM open failed")
                ),
            err -> logger.warn("Staff-auth user lookup failed")
        );
    }

    public boolean handleStaffAuthDmButton(
        StaffAuthButtonCodec.Parsed parsed,
        net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event
    ) {
        if (parsed == null || staffAuth == null || !ready()) return false;
        DiscordConfig cfg = config;
        if (cfg == null) return false;

        String secret = buttonSecret(cfg);
        if (secret == null || secret.isBlank()) return false;

        Optional<UUID> targetOpt = staffAuth.pendingTargetUuid(parsed.token());
        if (targetOpt.isEmpty()) return false;

        String linkedDiscord = linkSql.discordId(targetOpt.get()).orElse("");
        long expiresAt = staffAuth.pendingExpiresAtMillis(parsed.token()).orElse(0L);
        if (expiresAt <= 0L || !StaffAuthButtonCodec.verify(parsed, secret, expiresAt, linkedDiscord)) return false;

        Member member = event.getMember();
        String userId = event.getUser().getId();
        boolean overrideActor = hasStaffAuthOverride(member, cfg);
        if (!overrideActor && (linkedDiscord.isBlank() || !linkedDiscord.equals(userId))) return false;

        String actorName = event.getUser().getEffectiveName();
        UUID actorUuid = overrideActor ? null : resolveActorUuid(userId);

        boolean ok;
        if ("a".equals(parsed.action())) {
            ok = staffAuth.approve(parsed.token(), actorName, null).isPresent();
        } else if ("d".equals(parsed.action())) {
            ok = staffAuth.deny(parsed.token(), actorName).isPresent();
        } else {
            return false;
        }

        if (ok && overrideActor) {
            logStaffAuthOverride(parsed.token(), actorName, userId, "a".equals(parsed.action()));
            maybeEditNotifyAfterResolve(parsed.token(), actorName, userId, overrideActor, "a".equals(parsed.action()));
        } else if (ok) {
            maybeEditNotifyAfterResolve(parsed.token(), actorName, userId, false, "a".equals(parsed.action()));
        }

        return ok;
    }

    public boolean approveStaffAuth(String token, String actor, String discordId) {
        if (staffAuth == null) return false;
        UUID actorUuid = resolveActorUuid(discordId);
        return staffAuth.approve(token, actor == null ? "Discord" : actor, actorUuid).isPresent();
    }

    private void logStaffAuthOverride(String token, String actorName, String discordId, boolean approved) {
        DiscordConfig cfg = config;
        if (cfg == null || cfg.staffAuthFlow() == null) return;
        String logId = cfg.staffAuthFlow().overrideLogChannelId();
        if (logId == null || logId.isBlank()) return;
        TextChannel ch = jda.getTextChannelById(logId);
        if (ch == null) return;

        String line =
            (approved ? "Override approve" : "Override deny") +
            " · token " +
            safeTokenTail(token) +
            " · actor " +
            actorName +
            " (<@" +
            discordId +
            ">)";
        ch.sendMessage(line).queue(null, err -> logger.warn("Staff-auth override log failed"));
    }

    private void maybeEditNotifyAfterResolve(
        String token,
        String actorName,
        String discordId,
        boolean override,
        boolean approved
    ) {
        Long msgId = staffAuthNotifyMessages.remove(token.trim().toLowerCase(Locale.ROOT));
        DiscordConfig cfg = config;
        if (cfg == null || msgId == null || jda == null) return;

        String notifyChannelId = cfg.staffAuthFlow().notifyChannelId();
        if (notifyChannelId == null || notifyChannelId.isBlank()) {
            notifyChannelId = cfg.channel(DiscordChannelKey.STAFF_AUTH.id());
        }
        TextChannel ch =
            notifyChannelId == null || notifyChannelId.isBlank() ? null : jda.getTextChannelById(notifyChannelId);
        if (ch == null) return;

        ch.retrieveMessageById(msgId).queue(
            msg -> {
                String suffix = approved
                    ? " · " + (override ? "Aprobado por " : "Aprobado por ") + actorName + " (<@" + discordId + ">)"
                    : " · Denegado por " + actorName + " (<@" + discordId + ">)";
                MessageEmbed orig = msg.getEmbeds().isEmpty() ? null : msg.getEmbeds().get(0);
                if (orig == null) return;
                String desc = orig.getDescription() == null ? "" : orig.getDescription();
                EmbedBuilder builder = new EmbedBuilder(orig);
                builder.setDescription(desc + suffix);
                msg.editMessageEmbeds(builder.build()).queue(null, err -> {});
            },
            err -> {}
        );
    }

    private static String safeTokenTail(String token) {
        if (token == null || token.length() < 4) return "****";
        return "…" + token.substring(Math.max(0, token.length() - 4));
    }

    private boolean hasStaffAuthOverride(Member member, DiscordConfig cfg) {
        if (member == null || cfg == null || cfg.staffAuthFlow() == null) return false;

        if (member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) return true;

        List<String> roles = cfg.staffAuthFlow().overrideRoleIds();
        if (roles != null && !roles.isEmpty()) {
            java.util.Set<String> ids = member
                .getRoles()
                .stream()
                .map(net.dv8tion.jda.api.entities.Role::getId)
                .collect(java.util.stream.Collectors.toSet());
            for (String r : roles) {
                if (r != null && !r.isBlank() && ids.contains(r.trim())) return true;
            }
        }

        String perm = cfg.staffAuthFlow().overrideMcPermission();
        if (perm != null && !perm.isBlank()) {
            UUID linked = resolveActorUuid(member.getId());
            if (linked != null) {
                Optional<ProxyPlayer> p = platform.findPlayerByUuid(linked);
                if (p.isPresent() && p.get().hasPermission(perm)) return true;
            }
        }

        return false;
    }

    private String buttonSecret(DiscordConfig cfg) {
        if (cfg == null || cfg.staffAuthFlow() == null) return "";
        String env = cfg.staffAuthFlow().buttonSecretEnv();
        if (env == null || env.isBlank()) env = cfg.bridge().secretEnv();
        if (env == null || env.isBlank()) return "";
        String v = System.getenv(env);
        return v == null ? "" : v;
    }

    private static boolean isBlankEmbed(DiscordConfig.EmbedTemplate t) {
        if (t == null) return true;
        return (
            t.authorName().isBlank() &&
            t.authorIconUrl().isBlank() &&
            t.title().isBlank() &&
            t.description().isBlank() &&
            t.footer().isBlank() &&
            t.thumbnailUrl().isBlank()
        );
    }

    private UUID resolveActorUuid(String discordId) {
        if (discordId == null || discordId.isBlank() || storage == null) return null;

        return storage
            .mysql()
            .map(mysql -> lookupLinkedUuid(mysql, discordId))
            .orElse(null);
    }

    private static UUID lookupLinkedUuid(MySqlStorageService mysql, String discordId) {
        try (
            Connection connection = mysql.connection();
            PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid FROM ic_discord_links WHERE discord_id = ? LIMIT 1"
            )
        ) {
            ps.setString(1, discordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return UUID.fromString(rs.getString(1));
            }
        } catch (Exception ex) {
            return null;
        }
    }

    public void handlePunishment(
        DiscordPunishmentType type,
        String player,
        String duration,
        String reason,
        String actor,
        MessageChannel replyChannel
    ) {
        platform.asyncExecutor().execute(() -> {
            DiscordPunishmentHandler.Result result = punishments.apply(type, player, duration, reason, actor);
            if (!result.success()) {
                if (replyChannel != null) replyChannel.sendMessage("❌ " + result.error()).queue();
                return;
            }

            sendPunishmentEmbed(type, result);
            if (replyChannel != null) {
                replyChannel
                    .sendMessage(
                        "✅ **" +
                            result.type().name() +
                            "** applied to **" +
                            result.player() +
                            "** (`" +
                            result.duration() +
                            "`)"
                    )
                    .queue();
            }
        });
    }

    public void handlePunishment(
        DiscordPunishmentType type,
        String player,
        String duration,
        String reason,
        String actor,
        InteractionHook hook
    ) {
        platform.asyncExecutor().execute(() -> {
            DiscordPunishmentHandler.Result result = punishments.apply(type, player, duration, reason, actor);
            if (!result.success()) {
                hook.sendMessage("❌ " + result.error()).queue();
                return;
            }

            sendPunishmentEmbed(type, result);
            hook.sendMessage(
                "✅ **" +
                    result.type().name() +
                    "** applied to **" +
                    result.player() +
                    "** (`" +
                    result.duration() +
                    "`)"
            ).queue();
        });
    }

    private void sendPunishmentEmbed(DiscordPunishmentType type, DiscordPunishmentHandler.Result result) {
        DiscordConfig cfg = config;
        if (cfg == null || cfg.punishments() == null || !cfg.punishments().enabled()) return;

        String subKey = "punishment-" + type.name().toLowerCase(Locale.ROOT).replace('_', '-');
        DiscordConfig.EmbedTemplate tmpl = cfg.embed(subKey);
        if (isBlankEmbed(tmpl)) tmpl = cfg.embed("punishment");
        MessageEmbed embed = DiscordEmbeds.fromTemplate(tmpl, result.placeholders());
        String channelId = cfg.punishmentChannelId(type);
        TextChannel ch = channelId == null || channelId.isBlank() ? null : jda.getTextChannelById(channelId);
        if (ch == null) return;
        ch.sendMessageEmbeds(embed).queue(null, err -> logger.warn("Discord punishment send failed"));
    }

    public void sendStatusReply(InteractionHook hook, String serverName) {
        DiscordConfig cfg = config;
        if (cfg == null || !cfg.status().enabled()) {
            hook.sendMessage("Status disabled.").setEphemeral(true).queue();
            return;
        }

        Map<String, String> ph = statusPlaceholders(serverName);
        MessageEmbed embed = DiscordEmbeds.fromTemplate(cfg.embed("status"), ph);
        hook.sendMessageEmbeds(embed).setEphemeral(true).queue();
    }

    public List<String> statusChoices(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (BackendServer s : platform.registeredServers()) {
            String name = s.name();
            if (name == null || name.isBlank()) continue;
            if (!q.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(name);
            if (out.size() >= 25) break;
        }
        return out;
    }

    public void onPlayerJoin(ProxyPlayer player, String server) {}

    public void onPlayerQuit(String playerName, String server) {}

    private Map<String, String> statusPlaceholders(String serverName) {
        Map<String, String> ph = new LinkedHashMap<>();
        ph.put("online", String.valueOf(platform.onlinePlayers().size()));
        ph.put("proxy", proxyId);
        ph.put("servers", String.valueOf(platform.registeredServers().size()));
        ph.put(
            "maintenance",
            maintenance != null && maintenance.isGlobalActive()
                ? "Yes"
                : maintenance != null && serverName != null && maintenance.isServerBlocked(serverName)
                  ? "Yes"
                  : "No"
        );
        ph.put("server", serverName == null ? "?" : serverName);

        var srv = serverName == null ? Optional.<BackendServer>empty() : platform.server(serverName);
        if (srv.isEmpty()) {
            ph.put("server_online", "?");
            ph.put("server_players", "?");
            ph.put("ping", "?");
            ph.put("tps", "?");
            return ph;
        }

        BackendServer s = srv.get();
        ph.put("server_online", s.online() ? "Online" : "Offline");
        ph.put("server_players", s.players() + "/?");
        ph.put("ping", "?");
        ph.put("tps", "?");
        return ph;
    }

    private void sendTemplate(String channelKey, String embedKey, Map<String, String> placeholders) {
        if (!ready() || config == null) return;
        MessageEmbed embed = DiscordEmbeds.fromTemplate(config.embed(embedKey), placeholders);
        sendEmbed(channelKey, embed);
    }

    private void sendEmbed(String channelKey, MessageEmbed embed) {
        TextChannel channel = textChannel(channelKey);
        if (channel == null || embed == null) return;
        channel.sendMessageEmbeds(embed).queue(null, err -> logger.warn("Discord send failed (" + channelKey + ")"));
    }

    private TextChannel textChannel(String channelKey) {
        JDA current = jda;
        DiscordConfig cfg = config;
        if (current == null || cfg == null) return null;

        String id = cfg.channel(channelKey);
        if (id == null || id.isBlank()) return null;

        try {
            return current.getTextChannelById(id);
        } catch (Exception ex) {
            return null;
        }
    }

    private void registerSlashCommands(JDA jda, DiscordConfig cfg) {
        if (!cfg.slashCommands()) return;

        try {
            List<net.dv8tion.jda.api.interactions.commands.build.CommandData> commands = new ArrayList<>();
            for (DiscordPunishmentType type : DiscordPunishmentType.values()) {
                if (!cfg.commandEnabled(type.name())) continue;
                SlashCommandData data = Commands.slash(type.name().toLowerCase(Locale.ROOT), "RoyalCTW " + type.name());
                data.addOption(OptionType.STRING, "player", "Minecraft player name", true);
                if (type.requiresDuration()) {
                    data.addOption(OptionType.STRING, "duration", "Duration e.g. 1h 30m", true);
                }
                data.addOption(OptionType.STRING, "reason", "Reason", false);
                commands.add(data);
            }

            if (cfg.status().enabled()) {
                SlashCommandData status = Commands.slash("status", "RoyalCTW backend server status");
                status.addOption(OptionType.STRING, "server", "Server name", true, true);
                commands.add(status);
            }

            String guildId = cfg.guildId();
            if (guildId != null && !guildId.isBlank()) {
                var guild = jda.getGuildById(guildId);
                if (guild != null) {
                    guild.updateCommands().addCommands(commands).queue();
                    return;
                }
            }
            jda.updateCommands().addCommands(commands).queue();
        } catch (Exception ex) {
            logger.warn("Discord slash register failed: " + ex.getMessage());
        }
    }

    private void startStatusTimer() {}

    public static boolean isLegacyStaffAuthButton(String id) {
        return id != null && id.startsWith(LEGACY_STAFF_AUTH_PREFIX);
    }

    public static String legacyStaffAuthToken(String id) {
        if (!isLegacyStaffAuthButton(id)) return "";
        return id.substring(LEGACY_STAFF_AUTH_PREFIX.length());
    }
}
