package us.iorilabs.bukkit.modules.settings;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import us.iorilabs.bukkit.storage.StoragePaths;

public final class PlayerSettingsStore {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<UUID, PlayerSettings> cache = new ConcurrentHashMap<>();

    public PlayerSettingsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public PlayerSettings get(UUID uuid) {
        if (uuid == null) return new PlayerSettings(UUID.randomUUID());
        return cache.computeIfAbsent(uuid, this::load);
    }

    public String cachedLanguage(UUID uuid) {
        PlayerSettings settings = uuid == null ? null : cache.get(uuid);
        return settings == null ? null : settings.language();
    }

    public void save(PlayerSettings settings) {
        if (settings == null || settings.uuid() == null) return;
        cache.put(settings.uuid(), settings);

        File file = StoragePaths.playerDataFile(plugin, settings.uuid(), "settings.yml");
        FileConfiguration yaml = new YamlConfiguration();
        yaml.set("sounds", settings.sounds());
        yaml.set("particles", settings.particles());
        yaml.set("language", settings.language());
        yaml.set("premium-display", settings.premiumDisplay());
        yaml.set("scoreboard", settings.scoreboard());
        yaml.set("scoreboard-style", settings.scoreboardStyle());
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            logger.warning("Failed saving core player settings for " + settings.uuid() + ": " + ex.getMessage());
        }
    }

    public void unload(UUID uuid) {
        if (uuid == null) return;
        PlayerSettings settings = cache.remove(uuid);
        if (settings != null) save(settings);
    }

    private PlayerSettings load(UUID uuid) {
        PlayerSettings settings = new PlayerSettings(uuid);
        File file = StoragePaths.playerDataFile(plugin, uuid, "settings.yml");
        if (!file.exists()) return settings;

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        settings.sounds(yaml.getBoolean("sounds", true));
        settings.particles(yaml.getBoolean("particles", true));
        settings.language(yaml.getString("language", null));
        settings.premiumDisplay(yaml.getBoolean("premium-display", true));
        settings.scoreboard(yaml.getBoolean("scoreboard", true));
        settings.scoreboardStyle(yaml.getString("scoreboard-style", null));
        return settings;
    }
}
