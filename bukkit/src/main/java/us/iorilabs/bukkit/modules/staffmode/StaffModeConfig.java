package us.iorilabs.bukkit.modules.staffmode;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class StaffModeConfig {

    private final FileConfiguration file;
    private final FileConfiguration fallback;

    private StaffModeConfig(FileConfiguration file, FileConfiguration fallback) {
        this.file = file;
        this.fallback = fallback;
    }

    public static StaffModeConfig load(IoriCorePlugin plugin) {
        FileConfiguration fallback = plugin.settings();
        File onDisk = new File(plugin.getDataFolder(), "staff-mode.yml");
        us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "staff-mode.yml");

        FileConfiguration file = YamlConfiguration.loadConfiguration(onDisk);
        InputStream bundled = plugin.getResource("staff-mode.yml");
        if (bundled != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(bundled, StandardCharsets.UTF_8)
            );
            file.setDefaults(defaults);
            file.options().copyDefaults(true);
        }

        return new StaffModeConfig(file, fallback);
    }

    public boolean getBoolean(String path, boolean def) {
        if (file != null && file.contains(path)) return file.getBoolean(path);
        return fallback.getBoolean("STAFF-MODE." + path, def);
    }

    public long getLong(String path, long def) {
        if (file != null && file.contains(path)) return file.getLong(path);
        return fallback.getLong("STAFF-MODE." + path, def);
    }

    public String getString(String path, String def) {
        if (file != null && file.contains(path)) return file.getString(path, def);
        return fallback.getString("STAFF-MODE." + path, def);
    }

    public List<String> getStringList(String path) {
        if (file != null && file.isList(path)) return file.getStringList(path);
        List<String> legacy = fallback.getStringList("STAFF-MODE." + path);
        return legacy == null ? Collections.emptyList() : legacy;
    }
}
