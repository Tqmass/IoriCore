package us.iorilabs.bukkit.punishment;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class PunishmentConfig {

    private final FileConfiguration file;
    private final FileConfiguration fallback;

    private PunishmentConfig(FileConfiguration file, FileConfiguration fallback) {
        this.file = file;
        this.fallback = fallback;
    }

    public static PunishmentConfig load(IoriCorePlugin plugin) {
        File onDisk = new File(plugin.getDataFolder(), "punishment.yml");
        if (!onDisk.exists()) {
            us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "punishment.yml");
        }

        FileConfiguration file = YamlConfiguration.loadConfiguration(onDisk);
        InputStream bundled = plugin.getResource("punishment.yml");
        if (bundled != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(bundled, StandardCharsets.UTF_8)
            );
            file.setDefaults(defaults);
            file.options().copyDefaults(true);
        }

        return new PunishmentConfig(file, plugin.settings());
    }

    public String menuMain() {
        String value = getString("MENUS.MAIN", "punish");
        return value == null || value.isBlank() ? "punish" : value.trim();
    }

    public String menuReasons() {
        String value = getString("MENUS.REASONS", "punish-reasons");
        return value == null || value.isBlank() ? "punish-reasons" : value.trim();
    }

    public String reasonsPackFile() {
        String value = getString("REASONS-PACK", "punishments.yml");
        return value == null || value.isBlank() ? "punishments.yml" : value.trim();
    }

    public List<String> staffInspectConsole(String section) {
        return staffInspectList(section, "CONSOLE");
    }

    public List<String> staffInspectPlayer(String section) {
        return staffInspectList(section, "PLAYER");
    }

    private List<String> staffInspectList(String section, String side) {
        if (section == null || section.isBlank()) return Collections.emptyList();
        String key = section.trim().toUpperCase();
        String path = "STAFF-INSPECT." + key + "." + side;
        if (file != null && file.isList(path)) {
            List<String> list = file.getStringList(path);
            return list == null ? Collections.emptyList() : list;
        }
        List<String> legacy = fallback.getStringList("STAFF-MODE.PUNISH." + key + "." + side);
        return legacy == null ? Collections.emptyList() : legacy;
    }

    public String getString(String path, String def) {
        if (file != null && file.contains(path)) return file.getString(path, def);
        return def;
    }
}
