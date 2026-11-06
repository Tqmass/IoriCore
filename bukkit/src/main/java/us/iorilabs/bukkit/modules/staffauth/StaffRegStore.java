package us.iorilabs.bukkit.modules.staffauth;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.storage.StoragePaths;

final class StaffRegStore {

    private static final Set<String> ALLOWED_ROOT = Set.of(
        "password-hash",
        "must-change-password",
        "discord-id",
        "status",
        "code",
        "expires-at",
        "linked-at",
        "registered-at",
        "login-failures"
    );

    private final File folder;

    StaffRegStore(IoriCorePlugin plugin) {
        this.folder = StoragePaths.storage(plugin, "staffreg");
        StoragePaths.migrateIfPresent(new File(plugin.getDataFolder(), "data/staffreg"), folder);
        if (!folder.exists()) folder.mkdirs();
    }

    void savePending(UUID uuid, String code, long expiresAt) {
        if (uuid == null || code == null) return;
        FileConfiguration yaml = loadOrCreate(uuid);
        yaml.set("code", code);
        yaml.set("expires-at", expiresAt);
        yaml.set("status", "PENDING");
        yaml.set("discord-id", yaml.getString("discord-id", ""));
        save(uuid, yaml);
    }

    void markLinked(UUID uuid, String discordId) {
        if (uuid == null) return;
        FileConfiguration yaml = loadOrCreate(uuid);
        yaml.set("status", "LINKED");
        yaml.set("discord-id", discordId == null ? "" : discordId);
        yaml.set("code", null);
        yaml.set("expires-at", 0L);
        yaml.set("linked-at", System.currentTimeMillis());
        save(uuid, yaml);
    }

    void savePassword(UUID uuid, String passwordHash) {
        if (uuid == null || passwordHash == null) return;
        FileConfiguration yaml = loadOrCreate(uuid);
        yaml.set("password-hash", passwordHash);
        yaml.set("registered-at", System.currentTimeMillis());
        save(uuid, yaml);
    }

    void markMustChange(UUID uuid, boolean mustChange) {
        if (uuid == null) return;
        FileConfiguration yaml = loadOrCreate(uuid);
        yaml.set("must-change-password", mustChange);
        save(uuid, yaml);
    }

    boolean mustChangePassword(UUID uuid) {
        FileConfiguration yaml = load(uuid);
        return yaml != null && yaml.getBoolean("must-change-password", false);
    }

    boolean registered(UUID uuid) {
        FileConfiguration yaml = load(uuid);
        if (yaml == null) return false;
        String hash = yaml.getString("password-hash", "");
        return hash != null && !hash.isBlank();
    }

    Optional<String> passwordHash(UUID uuid) {
        FileConfiguration yaml = load(uuid);
        if (yaml == null) return Optional.empty();
        String hash = yaml.getString("password-hash", "");
        if (hash == null || hash.isBlank()) return Optional.empty();
        return Optional.of(hash);
    }

    boolean linked(UUID uuid) {
        FileConfiguration yaml = load(uuid);
        if (yaml == null) return false;
        return "LINKED".equalsIgnoreCase(yaml.getString("status", "")) && !yaml.getString("discord-id", "").isBlank();
    }

    Optional<String> code(UUID uuid) {
        FileConfiguration yaml = load(uuid);
        if (yaml == null) return Optional.empty();
        String code = yaml.getString("code", "");
        long expires = yaml.getLong("expires-at", 0L);
        if (code == null || code.isBlank()) return Optional.empty();
        if (expires > 0L && System.currentTimeMillis() > expires) return Optional.empty();
        return Optional.of(code);
    }

    int loginFailures(UUID uuid) {
        FileConfiguration yaml = load(uuid);
        if (yaml == null) return 0;
        return Math.max(0, yaml.getInt("login-failures", 0));
    }

    void incrementLoginFailures(UUID uuid) {
        if (uuid == null) return;
        FileConfiguration yaml = loadOrCreate(uuid);
        yaml.set("login-failures", Math.max(0, yaml.getInt("login-failures", 0)) + 1);
        save(uuid, yaml);
    }

    void clearLoginFailures(UUID uuid) {
        if (uuid == null) return;
        FileConfiguration yaml = load(uuid);
        if (yaml == null || !yaml.contains("login-failures")) return;
        yaml.set("login-failures", null);
        save(uuid, yaml);
    }

    void clear(UUID uuid) {
        File file = file(uuid);
        if (file.exists()) file.delete();
    }

    private FileConfiguration loadOrCreate(UUID uuid) {
        FileConfiguration yaml = load(uuid);
        return yaml == null ? new YamlConfiguration() : yaml;
    }

    private FileConfiguration load(UUID uuid) {
        File file = file(uuid);
        if (!file.exists()) return null;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean pruned = prune(yaml);
        if (pruned) save(uuid, yaml);
        return yaml;
    }

    private boolean prune(FileConfiguration yaml) {
        boolean changed = false;
        for (String key : new HashSet<>(yaml.getKeys(false))) {
            if (ALLOWED_ROOT.contains(key)) continue;
            yaml.set(key, null);
            changed = true;
        }
        return changed;
    }

    private void save(UUID uuid, FileConfiguration yaml) {
        if (uuid == null || yaml == null) return;
        prune(yaml);
        try {
            if (!folder.exists()) folder.mkdirs();
            yaml.save(file(uuid));
        } catch (IOException ignored) {}
    }

    private File file(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }
}
