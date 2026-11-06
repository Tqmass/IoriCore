package us.iorilabs.proxy.staff;

import java.util.List;
import java.util.Map;
import us.iorilabs.proxy.config.CommandsConfig;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.TextNames;

public final class StaffAlertService {

    private final ProxyPlatform platform;
    private final LanguageManager language;
    private final String receivePermission;

    public StaffAlertService(ProxyPlatform platform, LanguageManager language, CommandsConfig commands) {
        this.platform = platform;
        this.language = language;
        this.receivePermission = commands.permission("alerts");
    }

    public void join(String player, String server) {
        alert("alerts.join", Map.of("player", player, "server", TextNames.displayServer(server)));
    }

    public void quit(String player, String server) {
        alert("alerts.quit", Map.of("player", player, "server", TextNames.displayServer(server)));
    }

    public void switchServer(String player, String from, String to) {
        if (from != null && from.equalsIgnoreCase(to)) return;

        alert(
            "alerts.switch",
            Map.of("player", player, "from", TextNames.displayServer(from), "server", TextNames.displayServer(to))
        );
    }

    public void send(String player, String server, String by) {
        alert(
            "alerts.send",
            Map.of("player", player, "server", TextNames.displayServer(server), "by", by == null ? "?" : by)
        );
    }

    public void maintenance(boolean enabled, String scope, String by) {
        alert(
            enabled ? "alerts.maintenance_on" : "alerts.maintenance_off",
            Map.of("scope", scope == null ? "?" : scope, "by", by == null ? "?" : by)
        );
    }

    public void maintenanceAttempt(String player, String scope) {
        alert("alerts.maintenance_attempt", Map.of("player", player, "scope", scope == null ? "?" : scope));
    }

    public void alert(String key, Map<String, String> placeholders) {
        List<String> lines = language.rawList(key, placeholders);
        if (lines.isEmpty()) {
            String flat = language.raw(key, placeholders);
            if (flat == null || flat.isBlank() || flat.equals(key)) return;
            lines = List.of(flat);
        }

        for (String line : lines) {
            if (line == null || line.isBlank()) continue;

            for (ProxyPlayer online : platform.onlinePlayers()) {
                if (online.hasPermission(receivePermission)) {
                    online.sendMessage(line);
                }
            }
        }
    }
}
