package us.iorilabs.bukkit.storage;

import java.io.File;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.api.storage.StorageBackend;

public final class DatabaseConfig {

    private final StorageBackend backend;
    private final FileConfiguration yaml;

    private DatabaseConfig(StorageBackend backend, FileConfiguration yaml) {
        this.backend = backend;
        this.yaml = yaml;
    }

    public static DatabaseConfig load(File dataFolder, FileConfiguration settings, Logger logger) {
        File file = new File(dataFolder, "database.yml");
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String fromYaml = yaml.getString("DATABASE", "NONE");
        String fromYamlType = yaml.getString("TYPE", "");
        String fromSettings = settings == null ? "" : settings.getString("DATABASE.TYPE", "");
        String effective = fromYaml;
        if (fromSettings != null && !fromSettings.trim().isEmpty()) {
            effective = fromSettings.trim();
        } else if (fromYamlType != null && !fromYamlType.trim().isEmpty()) {
            effective = fromYamlType.trim();
        }
        StorageBackend backend = parse(effective, logger);
        return new DatabaseConfig(backend, yaml);
    }

    public StorageBackend backend() {
        return backend;
    }

    public FileConfiguration yaml() {
        return yaml;
    }

    public boolean sqlEnabled() {
        return backend == StorageBackend.MYSQL;
    }

    private static StorageBackend parse(String raw, Logger logger) {
        if (raw == null || raw.trim().isEmpty()) return StorageBackend.NONE;
        String key = raw.trim().toLowerCase(Locale.ROOT);
        if ("mysql".equals(key) || "mariadb".equals(key)) return StorageBackend.MYSQL;
        if ("mongo".equals(key) || "mongodb".equals(key)) {
            if (logger != null) {
                logger.fine("DATABASE=mongo · SQL off; SoftMongo optional; punishments use data/");
            }
            return StorageBackend.NONE;
        }
        if ("h2".equals(key)) {
            if (logger != null) {
                logger.warning("DATABASE=H2 removed · using NONE (YAML data/). Set MYSQL to share bans.");
            }
            return StorageBackend.NONE;
        }
        if ("none".equals(key) || "off".equals(key) || "disabled".equals(key) || "yaml".equals(key)) {
            return StorageBackend.NONE;
        }
        if (logger != null) logger.warning("Unknown DATABASE '" + raw + "', using NONE (YAML data/)");
        return StorageBackend.NONE;
    }
}
