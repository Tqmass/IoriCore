package us.iorilabs.bukkit.modules.core.warp;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsLocations;
import us.iorilabs.bukkit.storage.StoragePaths;

@Getter
@Accessors(fluent = true)
public final class WarpService {

    private final IoriCorePlugin plugin;
    private final File file;
    private final Map<String, Location> warps = new LinkedHashMap<>();

    public WarpService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.file = StoragePaths.core(plugin, "warps.yml");
        load();
    }

    public void load() {
        warps.clear();
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (!file.exists()) return;

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("warps");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            Location location = EssentialsLocations.read(section, key);
            if (location != null) warps.put(EssentialsLocations.normalizeKey(key), location);
        }
    }

    public Location get(String name) {
        return warps.get(EssentialsLocations.normalizeKey(name));
    }

    public boolean set(String name, Location location) {
        if (location == null || location.getWorld() == null) return false;
        String key = EssentialsLocations.normalizeKey(name);
        if (key.isEmpty()) return false;
        warps.put(key, location.clone());
        save();
        return true;
    }

    public boolean delete(String name) {
        String key = EssentialsLocations.normalizeKey(name);
        if (warps.remove(key) == null) return false;
        save();
        return true;
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(warps.keySet());
    }

    public Map<String, Location> all() {
        return Collections.unmodifiableMap(warps);
    }

    private void save() {
        if (!plugin.settings().getBoolean("PERSISTENCE.WARPS", true)) return;

        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            EssentialsLocations.write(yaml, "warps." + entry.getKey(), entry.getValue());
        }
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save warps", ex);
        }
    }
}
