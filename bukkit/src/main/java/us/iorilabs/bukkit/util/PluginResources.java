package us.iorilabs.bukkit.util;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginResources {

    private PluginResources() {}

    public static void saveDefault(JavaPlugin plugin, String path) {
        if (plugin == null || path == null || path.isEmpty()) return;
        File out = new File(plugin.getDataFolder(), path);
        if (out.exists()) return;
        if (plugin.getResource(path) == null) return;
        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        plugin.saveResource(path, false);
    }
}
