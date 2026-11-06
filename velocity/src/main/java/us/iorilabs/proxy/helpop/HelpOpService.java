package us.iorilabs.proxy.helpop;

import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import us.iorilabs.proxy.config.CommandsConfig;
import us.iorilabs.proxy.config.HelpOpConfig;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.server.PresenceService;
import us.iorilabs.proxy.utilities.ColorUtil;
import us.iorilabs.proxy.utilities.TextNames;
import us.iorilabs.proxy.webhooks.WebhookRegistry;

public final class HelpOpService {

    private static final String MESSAGE_SLOT = "{iori:helpop-message}";

    private final ProxyPlatform platform;
    private final RedisService redis;
    private final LanguageManager language;
    private final WebhookRegistry webhooks;
    private final PresenceService presence;
    private final StorageService storage;
    private final ProxyLogger logger;
    private final CommandsConfig commands;
    private final String proxyId;
    private final Map<String, HelpOpTicket> byId = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownMs = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(1);
    private HelpOpConfig helpOpConfig;

    public HelpOpService(
        ProxyPlatform platform,
        RedisService redis,
        LanguageManager language,
        WebhookRegistry webhooks,
        PresenceService presence,
        StorageService storage,
        ProxyLogger logger,
        CommandsConfig commands,
        String proxyId,
        HelpOpConfig helpOpConfig
    ) {
        this.platform = platform;
        this.redis = redis;
        this.language = language;
        this.webhooks = webhooks;
        this.presence = presence;
        this.storage = storage;
        this.logger = logger;
        this.commands = commands;
        this.proxyId = proxyId;
        this.helpOpConfig = helpOpConfig;
        redis.subscribe(ProxyEventType.HELPOP_CREATED.id(), this::onRemoteCreated);
        redis.subscribe(ProxyEventType.HELPOP_REPLY.id(), this::onRemoteReply);
    }

    public void reloadConfig(HelpOpConfig helpOpConfig) {
        this.helpOpConfig = helpOpConfig;
    }

    public Optional<Long> cooldownRemainingSeconds(UUID uuid) {
        Long last = cooldownMs.get(uuid);
        if (last == null) return Optional.empty();

        long cooldown = helpOpConfig.cooldownSeconds() * 1000L;
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed >= cooldown) return Optional.empty();

        return Optional.of((cooldown - elapsed + 999) / 1000);
    }

    public Optional<HelpOpTicket> create(ProxyPlayer player, String message) {
        Optional<Long> cd = cooldownRemainingSeconds(player.uuid());
        if (cd.isPresent()) return Optional.empty();

        String id = String.format("%05d", idSequence.getAndIncrement() % 100_000);
        String server = player.currentServer() == null ? "unknown" : player.currentServer();
        HelpOpTicket ticket = new HelpOpTicket(
            id,
            player.uuid(),
            player.name(),
            message,
            server,
            System.currentTimeMillis(),
            true
        );
        byId.put(id, ticket);
        cooldownMs.put(player.uuid(), System.currentTimeMillis());
        persistAsync(ticket);
        notifyStaff(ticket);
        publishCreated(ticket);
        fireWebhook(ticket);
        return Optional.of(ticket);
    }

    public Optional<HelpOpTicket> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public boolean reply(ProxyPlayer staff, String id, String replyMessage) {
        HelpOpTicket ticket = byId.get(id);
        if (ticket == null || !ticket.open()) return false;

        deliverReply(ticket.playerUuid(), ticket.playerName(), staff.name(), replyMessage);
        JsonObject data = new JsonObject();
        data.addProperty("player_uuid", ticket.playerUuid().toString());
        data.addProperty("player", ticket.playerName());
        data.addProperty("staff", staff.name());
        data.addProperty("message", replyMessage);
        redis.publish(new RedisEvent(ProxyEventType.HELPOP_REPLY.id(), proxyId, data));
        return true;
    }

    public boolean moveStaffToTicket(ProxyPlayer staff, String id) {
        HelpOpTicket ticket = byId.get(id);
        if (ticket == null) return false;

        Optional<String> server = presence.findByName(ticket.playerName()).map(p -> p.server());
        if (server.isEmpty()) {
            server = platform.findPlayerByUuid(ticket.playerUuid()).map(ProxyPlayer::currentServer);
        }

        if (server.isEmpty() || server.get().isBlank()) return false;

        return platform.connectPlayer(staff.uuid(), server.get());
    }

    private void deliverReply(UUID targetUuid, String targetName, String staffName, String message) {
        platform
            .findPlayerByUuid(targetUuid)
            .ifPresentOrElse(
                player ->
                    player.sendMessage(language.raw("helpop.replied", Map.of("staff", staffName, "message", message))),
                () -> logger.module("HelpOp", "reply for offline " + targetName)
            );
    }

    private void notifyStaff(HelpOpTicket ticket) {
        String perm = commands.permission("request_receive");
        Map<String, String> ph = Map.of(
            "id",
            ticket.id(),
            "player",
            ticket.playerName(),
            "message",
            ticket.message(),
            "server",
            TextNames.displayServer(ticket.server())
        );
        for (ProxyPlayer online : platform.onlinePlayers()) {
            if (!online.hasPermission(perm)) continue;

            for (String line : language.rawList("helpop.notify_title", ph)) {
                online.sendMessage(line);
            }

            online.sendMessage(notifyBody(online, ticket));
        }
    }

    private Component notifyBody(ProxyPlayer viewer, HelpOpTicket ticket) {
        Component reply = ColorUtil.parse(language.rawFor(viewer, "helpop.reply_button"))
            .clickEvent(ClickEvent.suggestCommand("/request reply " + ticket.id() + " "))
            .hoverEvent(HoverEvent.showText(ColorUtil.parse(language.rawFor(viewer, "helpop.reply_hover"))));
        Component move = ColorUtil.parse(language.rawFor(viewer, "helpop.move_button"))
            .clickEvent(ClickEvent.runCommand("/request move " + ticket.id()))
            .hoverEvent(HoverEvent.showText(ColorUtil.parse(language.rawFor(viewer, "helpop.move_hover"))));
        Component line = ColorUtil.parse(
            language.rawFor(
                viewer,
                "helpop.notify_line",
                Map.of("player", ticket.playerName(), "message", MESSAGE_SLOT)
            )
        ).replaceText(
            TextReplacementConfig.builder()
                .matchLiteral(MESSAGE_SLOT)
                .replacement(Component.text(ticket.message()))
                .build()
        );

        return line.append(Component.space()).append(reply).append(Component.space()).append(move);
    }

    private void publishCreated(HelpOpTicket ticket) {
        JsonObject data = new JsonObject();
        data.addProperty("id", ticket.id());
        data.addProperty("player_uuid", ticket.playerUuid().toString());
        data.addProperty("player", ticket.playerName());
        data.addProperty("message", ticket.message());
        data.addProperty("server", ticket.server());
        data.addProperty("created_at", ticket.createdAt());
        redis.publish(new RedisEvent(ProxyEventType.HELPOP_CREATED.id(), proxyId, data));
    }

    private void onRemoteCreated(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        JsonObject data = event.data();
        HelpOpTicket ticket = new HelpOpTicket(
            data.get("id").getAsString(),
            UUID.fromString(data.get("player_uuid").getAsString()),
            data.get("player").getAsString(),
            data.get("message").getAsString(),
            data.get("server").getAsString(),
            data.has("created_at") ? data.get("created_at").getAsLong() : System.currentTimeMillis(),
            true
        );
        byId.put(ticket.id(), ticket);
        notifyStaff(ticket);
    }

    private void onRemoteReply(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        JsonObject data = event.data();
        if (!data.has("player_uuid") || !data.has("message")) return;

        UUID uuid = UUID.fromString(data.get("player_uuid").getAsString());
        String staff = data.has("staff") ? data.get("staff").getAsString() : "Staff";
        deliverReply(
            uuid,
            data.has("player") ? data.get("player").getAsString() : "?",
            staff,
            data.get("message").getAsString()
        );
    }

    private void fireWebhook(HelpOpTicket ticket) {
        webhooks.send(
            "request",
            Map.of(
                "id",
                ticket.id(),
                "player",
                ticket.playerName(),
                "server",
                ticket.server(),
                "message",
                ticket.message()
            )
        );
    }

    private void persistAsync(HelpOpTicket ticket) {
        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty() || !mysql.get().isConnected()) return;

        platform.asyncExecutor().execute(() -> {
            try {
                mysql.get().call(connection -> {
                    try {
                        insert(connection, ticket);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }

                    return null;
                });
            } catch (SQLException ex) {
                logger.error("Failed persisting helpop " + ticket.id(), ex);
            }
        });
    }

    private void insert(Connection connection, HelpOpTicket ticket) throws SQLException {
        String sql = """
        INSERT INTO ip_helpop (id, player_uuid, player_name, message, server, created_at, open_flag)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE message = VALUES(message), open_flag = VALUES(open_flag)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticket.id());
            ps.setString(2, ticket.playerUuid().toString());
            ps.setString(3, ticket.playerName());
            ps.setString(4, ticket.message());
            ps.setString(5, ticket.server());
            ps.setLong(6, ticket.createdAt());
            ps.setBoolean(7, ticket.open());
            ps.executeUpdate();
        }
    }
}
