package us.iorilabs.bukkit.modules.economy;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.bukkit.storage.StoragePaths;

public final class EconomyYamlStore {

    private final File folder;
    private final Logger logger;
    private final boolean persist;

    public EconomyYamlStore(File dataFolder, Logger logger, boolean persist) {
        this.folder = new File(dataFolder, "data/economy");
        this.logger = logger;
        this.persist = persist;
        if (persist && !folder.exists()) folder.mkdirs();
    }

    public double load(UUID uuid, double fallback) {
        if (uuid == null) return fallback;
        File file = fileFor(uuid);
        if (!file.exists()) return fallback;

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return yaml.getDouble("balance", fallback);
    }

    public void save(UUID uuid, double balance) {
        if (!persist || uuid == null) return;
        if (!folder.exists()) folder.mkdirs();

        File file = fileFor(uuid);
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("balance", balance);
        try {
            yaml.save(file);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Economy YAML save failed · " + uuid, ex);
        }
    }

    private File fileFor(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }
}
