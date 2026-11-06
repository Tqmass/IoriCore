package us.iorilabs.proxy.staffchat;

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

public final class StaffChatService {

    private final ProxyPlatform platform;
    private final RedisService redis;
    private final LanguageManager language;
    private final String proxyId;
    private final String sendPermission;
    private final String receivePermission;
    private final StaffGroups staffGroups;
    private final Set<UUID> toggled = ConcurrentHashMap.newKeySet();
    private volatile StaffChatDiscordSink discordSink;

    public StaffChatService(
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
        this.sendPermission = commands.permission("staffchat_send");
        this.receivePermission = commands.permission("staffchat_receive");
        this.staffGroups = staffGroups;
        redis.subscribe(ProxyEventType.STAFFCHAT_MESSAGE.id(), this::onRemote);
    }

    public void discordSink(StaffChatDiscordSink sink) {
        this.discordSink = sink;
    }

    @FunctionalInterface
    public interface StaffChatDiscordSink {
        void accept(String player, String rank, String server, String message);
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

        send(player, message);
        return true;
    }

    public void send(ProxyPlayer player, String message) {
        if (!player.hasPermission(sendPermission)) return;

        String rank = resolveRank(player);
        String formatted = format(rank, player.name(), player.currentServer(), message);
        broadcastLocal(formatted);

        StaffChatDiscordSink sink = discordSink;
        if (sink != null) {
            try {
                sink.accept(player.name(), rank, player.currentServer(), message);
            } catch (Exception ignored) {}
        }

        JsonObject data = new JsonObject();
        data.addProperty("player", player.name());
        data.addProperty("rank", rank);
        data.addProperty("server", player.currentServer());
        data.addProperty("message", message);
        redis.publish(new RedisEvent(ProxyEventType.STAFFCHAT_MESSAGE.id(), proxyId, data));
    }

    private void onRemote(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        JsonObject data = event.data();
        String player = data.has("player") ? data.get("player").getAsString() : "?";
        String rank = data.has("rank") ? data.get("rank").getAsString() : "";
        String server = data.has("server") ? data.get("server").getAsString() : "";
        String message = data.has("message") ? data.get("message").getAsString() : "";
        broadcastLocal(format(rank, player, server, message));
    }

    private String resolveRank(ProxyPlayer player) {
        if (staffGroups == null) return "";

        // Solo prefijo LuckPerms — si no hay, no inventar rango.
        String display = staffGroups.displayRank(player);
        return display == null ? "" : display.trim();
    }

    private String format(String rank, String player, String server, String message) {
        String key = rank == null || rank.isBlank() ? "staffchat.format_plain" : "staffchat.format";
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

    private void broadcastLocal(String formatted) {
        for (ProxyPlayer online : platform.onlinePlayers()) {
            if (online.hasPermission(receivePermission)) {
                online.sendMessage(formatted);
            }
        }
    }
}
