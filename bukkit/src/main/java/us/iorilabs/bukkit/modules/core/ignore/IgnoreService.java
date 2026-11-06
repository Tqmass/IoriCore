package us.iorilabs.bukkit.modules.core.ignore;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.storage.StoragePaths;

@Getter
@Accessors(fluent = true)
public final class IgnoreService {

    private final IoriCorePlugin plugin;
    private final File file;
    private final Map<UUID, Set<UUID>> ignored = new ConcurrentHashMap<>();

    public IgnoreService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.file = StoragePaths.core(plugin, "ignore.yml");
        load();
    }

    public boolean ignores(UUID playerId, UUID targetId) {
        if (playerId == null || targetId == null) return false;
        Set<UUID> set = ignored.get(playerId);
        return set != null && set.contains(targetId);
    }

    public boolean toggle(UUID playerId, UUID targetId) {
        if (playerId == null || targetId == null || playerId.equals(targetId)) return false;
        Set<UUID> set = ignored.computeIfAbsent(playerId, id -> ConcurrentHashMap.newKeySet());
        boolean nowIgnored;
        if (set.contains(targetId)) {
            set.remove(targetId);
            nowIgnored = false;
        } else {
            set.add(targetId);
            nowIgnored = true;
        }
        if (set.isEmpty()) ignored.remove(playerId);
        save();
        return nowIgnored;
    }

    public Set<UUID> list(UUID playerId) {
        if (playerId == null) return Collections.emptySet();
        Set<UUID> set = ignored.get(playerId);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(set));
    }

    private void load() {
        ignored.clear();
        if (!file.exists()) return;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(key);
                Set<UUID> set = ConcurrentHashMap.newKeySet();
                for (String raw : yaml.getStringList(key)) {
                    try {
                        set.add(UUID.fromString(raw));
                    } catch (IllegalArgumentException ignored) {}
                }
                if (!set.isEmpty()) ignored.put(owner, set);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void save() {
        if (!plugin.settings().getBoolean("PERSISTENCE.STATS", true)) return;
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> entry : ignored.entrySet()) {
            java.util.List<String> list = new java.util.ArrayList<>();
            for (UUID id : entry.getValue()) list.add(id.toString());
            yaml.set(entry.getKey().toString(), list);
        }
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save ignore list", ex);
        }
    }
}
