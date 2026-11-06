package us.iorilabs.bukkit.modules.core.spawn;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsLocations;
import us.iorilabs.bukkit.storage.StoragePaths;

@Getter
@Accessors(fluent = true)
public final class SpawnService {

    private final IoriCorePlugin plugin;
    private final File file;
    private Location spawn;

    public SpawnService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.file = StoragePaths.core(plugin, "spawn.yml");
        load();
    }

    public void load() {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        spawn = EssentialsLocations.read(yaml, "spawn");
    }

    public void setSpawn(Location location) {
        this.spawn = location == null ? null : location.clone();
        save();
    }

    public boolean teleport(Player player) {
        if (player == null || spawn == null || spawn.getWorld() == null) return false;
        return player.teleport(spawn);
    }

    private void save() {
        FileConfiguration yaml = new YamlConfiguration();
        if (spawn != null) EssentialsLocations.write(yaml, "spawn", spawn);
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save spawn", ex);
        }
    }
}
