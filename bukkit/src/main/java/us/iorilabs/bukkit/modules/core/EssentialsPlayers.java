package us.iorilabs.bukkit.modules.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.vanish.VanishVisibility;
import us.iorilabs.proxy.commands.CommandSource;

public final class EssentialsPlayers {

    private EssentialsPlayers() {}

    public static Player player(CommandSource source) {
        if (source instanceof BukkitCommandSource bukkit) return bukkit.playerOrNull();
        return null;
    }

    public static Player findOnline(String name) {
        return findOnline(null, name);
    }

    public static Player findOnline(CommandSource viewer, String name) {
        if (name == null || name.trim().isEmpty()) return null;
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null && visible(viewer, exact)) return exact;

        String needle = name.toLowerCase(Locale.ROOT);
        Player partial = null;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!visible(viewer, online)) continue;
            String candidate = online.getName().toLowerCase(Locale.ROOT);
            if (candidate.equals(needle)) return online;
            if (partial == null && candidate.startsWith(needle)) partial = online;
        }
        return partial;
    }

    public static List<String> suggestOnline(String prefix) {
        return suggestOnline(null, prefix);
    }

    public static List<String> suggestOnline(CommandSource viewer, String prefix) {
        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!visible(viewer, online)) continue;
            if (online.getName().toLowerCase(Locale.ROOT).startsWith(needle)) {
                out.add(online.getName());
            }
        }
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private static boolean visible(CommandSource viewer, Player target) {
        if (target == null) return false;
        if (viewer == null) return true;
        if (!(viewer instanceof BukkitCommandSource bukkit)) return true;
        Player player = bukkit.playerOrNull();
        if (player == null) return true;
        return VanishVisibility.canSee(core(), player, target);
    }

    private static IoriCorePlugin core() {
        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("IoriCore");
        return plugin instanceof IoriCorePlugin ? (IoriCorePlugin) plugin : null;
    }
}
