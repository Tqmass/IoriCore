package us.iorilabs.bukkit.modules.lobby;

import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class TabNpcNames {

    private TabNpcNames() {}

    public static boolean isNpc(Player player, List<String> prefixes) {
        if (player == null) return false;
        if (player.hasMetadata("NPC")) return true;
        return matches(player.getName(), prefixes);
    }

    public static boolean matches(String name, List<String> prefixes) {
        if (name == null || name.isEmpty() || prefixes == null || prefixes.isEmpty()) return false;
        String stripped = ChatColor.stripColor(name).trim();
        if (stripped.isEmpty()) return false;
        String upper = stripped.toUpperCase(Locale.ROOT);
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isEmpty()) continue;
            String key = prefix.toUpperCase(Locale.ROOT);
            if (upper.startsWith(key)) return true;
        }
        return false;
    }
}
