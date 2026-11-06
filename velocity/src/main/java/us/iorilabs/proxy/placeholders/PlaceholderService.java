package us.iorilabs.proxy.placeholders;

import lombok.RequiredArgsConstructor;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.maintenance.MaintenanceService;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.DurationParser;

@RequiredArgsConstructor
public final class PlaceholderService {

    private static final String LUCKPERMS_PREFIX_TOKEN = "%luckperms_prefix%";

    private final IoriProxy proxy;

    public String apply(String input) {
        return apply(input, null);
    }

    public String apply(String input, ProxyPlayer player) {
        String resolved = PlaceholderTokens.replace(input, key -> resolve(key, player));
        if (resolved == null || player == null) return resolved;

        return resolved.replace(LUCKPERMS_PREFIX_TOKEN, proxy.luckPerms().getPrefix(player.uuid()));
    }

    private String resolve(String key, ProxyPlayer player) {
        return switch (key) {
            case "online" -> String.valueOf(proxy.platform().onlinePlayers().size());
            case "proxy_id" -> proxy.configs().settings().proxyId();
            case "server" -> player == null ? "" : nullToEmpty(player.currentServer());
            case "playtime_total" -> formatPlaytime(player);
            case "maintenance" -> maintenanceLabel();
            default -> "";
        };
    }

    private String formatPlaytime(ProxyPlayer player) {
        if (player == null || proxy.playtime() == null) return "0s";

        return DurationParser.format(proxy.playtime().totalMillis(player.uuid()));
    }

    private String maintenanceLabel() {
        MaintenanceService maintenance = proxy.maintenance();
        if (maintenance == null || maintenance.activeStates().isEmpty()) return "off";

        return "on";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
