package us.iorilabs.bukkit.modules.core.home;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
public final class HomeService {

    private final IoriCorePlugin plugin;
    private final Map<UUID, Map<String, Location>> homes = new ConcurrentHashMap<>();

    public HomeService(IoriCorePlugin plugin) {
        this.plugin = plugin;
    }

    public Location get(UUID uuid, String name) {
        if (uuid == null) return null;
        Map<String, Location> map = homes.get(uuid);
        if (map == null) {
            map = load(uuid);
            homes.put(uuid, map);
        }
        return map.get(EssentialsLocations.normalizeKey(name));
    }

    public Map<String, Location> list(UUID uuid) {
        if (uuid == null) return Collections.emptyMap();
        Map<String, Location> map = homes.get(uuid);
        if (map == null) {
            map = load(uuid);
            homes.put(uuid, map);
        }
        return Collections.unmodifiableMap(map);
    }

    public boolean set(UUID uuid, String name, Location location) {
        if (uuid == null || location == null || location.getWorld() == null) return false;
        String key = EssentialsLocations.normalizeKey(name);
        if (key.isEmpty()) return false;

        Map<String, Location> map = homes.computeIfAbsent(uuid, id -> new LinkedHashMap<>());
        map.put(key, location.clone());
        save(uuid, map);
        return true;
    }

    public boolean delete(UUID uuid, String name) {
        if (uuid == null) return false;
        String key = EssentialsLocations.normalizeKey(name);
        Map<String, Location> map = homes.computeIfAbsent(uuid, id -> load(id));
        if (map.remove(key) == null) return false;
        save(uuid, map);
        return true;
    }

    public int count(UUID uuid) {
        return list(uuid).size();
    }

    public void unload(UUID uuid) {
        if (uuid == null) return;
        homes.remove(uuid);
    }

    private Map<String, Location> load(UUID uuid) {
        Map<String, Location> map = new LinkedHashMap<>();
        File file = StoragePaths.playerDataFile(plugin, uuid, "homes.yml");
        if (!file.exists()) return map;

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("homes");
        if (section == null) return map;

        for (String key : section.getKeys(false)) {
            Location location = EssentialsLocations.read(section, key);
            if (location != null) map.put(EssentialsLocations.normalizeKey(key), location);
        }
        return map;
    }

    private void save(UUID uuid, Map<String, Location> map) {
        if (!plugin.settings().getBoolean("PERSISTENCE.HOMES", true)) return;

        File file = StoragePaths.playerDataFile(plugin, uuid, "homes.yml");
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Location> entry : map.entrySet()) {
            EssentialsLocations.write(yaml, "homes." + entry.getKey(), entry.getValue());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save homes for " + uuid, ex);
        }
    }

    public Set<String> names(UUID uuid) {
        return list(uuid).keySet();
    }
}
