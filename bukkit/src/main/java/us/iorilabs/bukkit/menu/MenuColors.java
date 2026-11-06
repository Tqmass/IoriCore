package us.iorilabs.bukkit.menu;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.util.ProtocolProbe;
import us.iorilabs.proxy.utilities.ProtocolVersions;

public final class MenuColors {

    private static final int LEGACY_TITLE_LIMIT = 32;
    private static final boolean LEGACY_SERVER =
        ProtocolProbe.serverFallbackProtocol() <= ProtocolVersions.LEGACY_MAX_PROTOCOL;

    private MenuColors() {}

    public static String legacy(String input) {
        if (input == null || input.isEmpty()) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String title(String input) {
        return clipIfNeeded(legacy(input), LEGACY_SERVER);
    }

    public static String title(Player player, String input) {
        String colored = legacy(input);
        int protocol = ProtocolProbe.protocol(player, ProtocolProbe.serverFallbackProtocol());
        boolean legacy = ProtocolVersions.isLegacy(protocol) || LEGACY_SERVER;
        return clipIfNeeded(colored, legacy);
    }

    private static String clipIfNeeded(String colored, boolean legacy) {
        if (!legacy || colored.length() <= LEGACY_TITLE_LIMIT) return colored;
        String clipped = colored.substring(0, LEGACY_TITLE_LIMIT);
        if (clipped.endsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        return clipped;
    }
}
