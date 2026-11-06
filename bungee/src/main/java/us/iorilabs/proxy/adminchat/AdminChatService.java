package us.iorilabs.proxy.adminchat;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.proxy.config.CommandsConfig;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.staff.StaffGroups;
import us.iorilabs.proxy.utilities.TextNames;

public final class AdminChatService {

    private final ProxyPlatform platform;
    private final RedisService redis;
    private final LanguageManager language;
    private final String proxyId;
    private final String sendPermission;
    private final String receivePermission;
    private final String requestNotifyPermission;
    private final StaffGroups staffGroups;
    private final Set<UUID> toggled = ConcurrentHashMap.newKeySet();

    public AdminChatService(
        ProxyPlatform platform,
        RedisService redis,
        LanguageManager language,
        String proxyId,
        CommandsConfig commands,
        StaffGroups staffGroups
    ) {
        this.platform = platform;
        this.redis = redis;
        this.language = language;
        this.proxyId = proxyId;
        this.sendPermission = commands.permission("adminchat_send");
        this.receivePermission = commands.permission("adminchat_receive");
        this.requestNotifyPermission = commands.permission("request_receive");
        this.staffGroups = staffGroups;
        redis.subscribe(ProxyEventType.ADMINCHAT_MESSAGE.id(), this::onRemote);
    }

    public boolean isToggled(UUID uuid) {
        return toggled.contains(uuid);
    }

    public boolean toggle(UUID uuid) {
        if (toggled.contains(uuid)) {
            toggled.remove(uuid);
            return false;
        }

        toggled.add(uuid);
        return true;
    }

    public void untoggle(UUID uuid) {
        toggled.remove(uuid);
    }

    public boolean interceptChat(ProxyPlayer player, String message) {
        if (!toggled.contains(player.uuid())) return false;
        if (!player.hasPermission(sendPermission)) return false;

        sendAdmin(player, message);
        return true;
    }

    public void sendAdmin(ProxyPlayer player, String message) {
        if (!player.hasPermission(sendPermission)) return;

        String rank = resolveRank(player);
        String formatted = formatAdmin(rank, player.name(), player.currentServer(), message);
        broadcastAdmin(formatted);

        JsonObject data = new JsonObject();
        data.addProperty("kind", "admin");
        data.addProperty("player", player.name());
        data.addProperty("rank", rank);
        data.addProperty("server", player.currentServer());
        data.addProperty("message", message);
        redis.publish(new RedisEvent(ProxyEventType.ADMINCHAT_MESSAGE.id(), proxyId, data));
    }

    public void sendRequestAlert(ProxyPlayer player, String message) {
        String formatted = language.raw(
            "adminchat.request_alert",
            Map.of(
                "player",
                player.name(),
                "server",
                TextNames.displayServer(player.currentServer()),
                "message",
                message
            )
        );
        broadcastRequest(formatted);

        JsonObject data = new JsonObject();
        data.addProperty("kind", "request");
        data.addProperty("player", player.name());
        data.addProperty("server", player.currentServer());
        data.addProperty("message", message);
        redis.publish(new RedisEvent(ProxyEventType.ADMINCHAT_MESSAGE.id(), proxyId, data));
    }

    private void onRemote(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        JsonObject data = event.data();
        String kind = data.has("kind") ? data.get("kind").getAsString() : "admin";
        String player = data.has("player") ? data.get("player").getAsString() : "?";
        String rank = data.has("rank") ? data.get("rank").getAsString() : "";
        String server = data.has("server") ? data.get("server").getAsString() : "";
        String message = data.has("message") ? data.get("message").getAsString() : "";

        if ("request".equalsIgnoreCase(kind)) {
            broadcastRequest(
                language.raw(
                    "adminchat.request_alert",
                    Map.of("player", player, "server", TextNames.displayServer(server), "message", message)
                )
            );
            return;
        }

        broadcastAdmin(formatAdmin(rank, player, server, message));
    }

    private String resolveRank(ProxyPlayer player) {
        if (staffGroups == null) return "";

        String display = staffGroups.displayRank(player);
        return display == null ? "" : display.trim();
    }

    private String formatAdmin(String rank, String player, String server, String message) {
        String key = rank == null || rank.isBlank() ? "adminchat.format_plain" : "adminchat.format";
        return language.raw(
            key,
            Map.of(
                "rank",
                rank == null ? "" : rank,
                "player",
                player,
                "server",
                TextNames.displayServer(server == null || server.isBlank() ? "?" : server),
                "message",
                message
            )
        );
    }

    private void broadcastAdmin(String formatted) {
        for (ProxyPlayer online : platform.onlinePlayers()) {
            if (online.hasPermission(receivePermission)) {
                online.sendMessage(formatted);
            }
        }
    }

    private void broadcastRequest(String formatted) {
        for (ProxyPlayer online : platform.onlinePlayers()) {
            if (online.hasPermission(requestNotifyPermission) || online.hasPermission(receivePermission)) {
                online.sendMessage(formatted);
            }
        }
    }
}
