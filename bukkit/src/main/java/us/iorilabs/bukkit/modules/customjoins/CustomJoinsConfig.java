package us.iorilabs.bukkit.modules.customjoins;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class CustomJoinsConfig {

    private final IoriCorePlugin plugin;
    private List<CustomJoinsEntry> entries = Collections.emptyList();

    public CustomJoinsConfig(IoriCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ConfigurationSection root = plugin.settings().getConfigurationSection("CUSTOM-JOINS.VIP-MESSAGES");
        if (root == null) {
            File file = new File(plugin.getDataFolder(), "custom-joins.yml");
            if (!file.exists()) {
                us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "custom-joins.yml");
            }
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            root = yaml.getConfigurationSection("VIP-MESSAGES");
        }
        if (root == null) {
            entries = Collections.emptyList();
            return;
        }

        List<CustomJoinsEntry> loaded = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;

            String permission = section.getString("PERMISSION", "");
            ConfigurationSection join = section.getConfigurationSection("JOIN");
            ConfigurationSection leave = section.getConfigurationSection("LEAVE");
            loaded.add(
                new CustomJoinsEntry(
                    id,
                    permission == null ? "" : permission.trim(),
                    join != null && join.getBoolean("ENABLED", false),
                    join == null ? "" : join.getString("MESSAGE", ""),
                    join == null ? "" : join.getString("SOUND", ""),
                    leave != null && leave.getBoolean("ENABLED", false),
                    leave == null ? "" : leave.getString("MESSAGE", ""),
                    leave == null ? "" : leave.getString("SOUND", "")
                )
            );
        }
        entries = Collections.unmodifiableList(loaded);
    }

    public List<CustomJoinsEntry> entries() {
        return entries;
    }
}
