package us.iorilabs.bukkit.modules.staffmode;

import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.redis.CoreRedisEvent;
import us.iorilabs.bukkit.redis.CoreRedisService;

public final class StaffChatService {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final StaffModeConfig config;
    private final Set<UUID> toggled = ConcurrentHashMap.newKeySet();

    public StaffChatService(IoriCorePlugin plugin, StaffModeConfig config) {
        this.plugin = plugin;
        this.language = plugin.language();
        this.config = config;

        CoreRedisService redis = plugin.redis();
        if (redis != null) {
            redis.subscribe(NetworkEventType.STAFFCHAT_MESSAGE, this::onRemote);
        }
    }

    public boolean toggled(UUID uuid) {
        return uuid != null && toggled.contains(uuid);
    }

    public boolean toggle(Player player) {
        if (player == null) return false;
        if (!player.hasPermission(sendPermission()) && !player.hasPermission("ioricore.admin")) {
            player.sendMessage(language.rawFor(player, "GENERAL.NO-PERMISSION"));
            return toggled(player.getUniqueId());
        }

        UUID id = player.getUniqueId();
        if (toggled.remove(id)) {
            player.sendMessage(language.rawFor(player, "STAFFCHAT.TOGGLE-OFF"));
            return false;
        }

        toggled.add(id);
        player.sendMessage(language.rawFor(player, "STAFFCHAT.TOGGLE-ON"));
        return true;
    }

    public boolean intercept(Player player, String message) {
        if (player == null || message == null) return false;
        if (!toggled(player.getUniqueId())) return false;
        if (!player.hasPermission(sendPermission()) && !player.hasPermission("ioricore.admin")) return false;

        sendLocal(player, message);
        return true;
    }

    public void untoggle(UUID uuid) {
        if (uuid != null) toggled.remove(uuid);
    }

    private void sendLocal(Player sender, String message) {
        String formatted = format(sender.getName(), serverName(), message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(receivePermission()) && !online.hasPermission("ioricore.admin")) continue;
            online.sendMessage(formatted);
        }
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    private void publishRemote(Player sender, String message) {
        CoreRedisService redis = plugin.redis();
        if (redis == null) return;

        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.PLAYER, sender.getName());
        data.addProperty(NetworkEventKeys.MESSAGE, message);
        data.addProperty(NetworkEventKeys.SERVER, serverName());
        data.addProperty(NetworkEventKeys.RANK_KEY, "");
        redis.publish(NetworkEventType.STAFFCHAT_MESSAGE, data);
    }

    private void onRemote(CoreRedisEvent event) {
        if (event == null || event.data() == null) return;

        JsonObject data = event.data();
        String player = data.has(NetworkEventKeys.PLAYER) ? data.get(NetworkEventKeys.PLAYER).getAsString() : "?";
        String message = data.has(NetworkEventKeys.MESSAGE) ? data.get(NetworkEventKeys.MESSAGE).getAsString() : "";
        String server = data.has(NetworkEventKeys.SERVER) ? data.get(NetworkEventKeys.SERVER).getAsString() : "";
        String formatted = format(player, server, message);

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission(receivePermission()) && !online.hasPermission("ioricore.admin")) continue;
                online.sendMessage(formatted);
            }
        });
    }

    private String format(String player, String server, String message) {
        Map<String, String> ph = Map.of(
            "player",
            player == null ? "?" : player,
            "server",
            server == null ? "" : server,
            "message",
            message == null ? "" : message
        );
        String raw = language.raw("STAFFCHAT.FORMAT-PLAIN", ph);
        if (raw == null || raw.isEmpty() || raw.equals("STAFFCHAT.FORMAT-PLAIN")) {
            return language.raw("STAFFCHAT.FORMAT", ph);
        }
        return raw;
    }

    private String serverName() {
        String name = plugin.settings().getString("SETTINGS.SERVER-NAME", "");
        return name == null ? "" : name.trim();
    }

    private String sendPermission() {
        return permissionNode("STAFFCHAT.SEND", "STAFFCHAT.SEND-PERMISSION", "ioricore.staffchat.send");
    }

    private String receivePermission() {
        return permissionNode("STAFFCHAT.RECEIVE", "STAFFCHAT.RECEIVE-PERMISSION", "ioricore.staffchat.receive");
    }

    private String permissionNode(String permissionsKey, String staffModeKey, String fallback) {
        String node = plugin.permissions() == null ? null : plugin.permissions().getString(permissionsKey);
        if (node == null || node.isBlank()) node = config.getString(staffModeKey, fallback);
        return node == null || node.isBlank() ? fallback : node.trim();
    }

    public String defaultStaffAction() {
        String mode = config.getString("STAFF-COMMAND.DEFAULT", "STAFFCHAT");
        return mode == null ? "STAFFCHAT" : mode.trim().toUpperCase(Locale.ROOT);
    }

    public String staffMenuId() {
        String id = config.getString("STAFF-COMMAND.MENU-ID", "staff-players");
        return id == null || id.isBlank() ? "staff-players" : id.trim();
    }
}
