package us.iorilabs.bukkit.modules.lobby;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TabNpcVisibility {

    private TabNpcVisibility() {}

    public static void refresh(JavaPlugin plugin, boolean enabled, List<String> prefixes) {
        if (!enabled || plugin == null || prefixes == null || prefixes.isEmpty()) return;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (TabNpcNames.isNpc(viewer, prefixes)) continue;
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!TabNpcNames.isNpc(target, prefixes)) continue;
                hideTabOnly(viewer, target, plugin);
            }
        }
    }

    private static void hideTabOnly(Player viewer, Player target, JavaPlugin plugin) {
        if (viewer == null || target == null || viewer.equals(target)) return;
        try {
            Class<?> flags = Class.forName("org.bukkit.entity.Player$HideFlags");
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object listOnly = Enum.valueOf((Class) flags, "LISTED_PLAYERS");
            viewer
                .getClass()
                .getMethod("hidePlayer", JavaPlugin.class, Player.class, flags)
                .invoke(viewer, plugin, target, listOnly);
        } catch (ReflectiveOperationException ignored) {}
    }
}
