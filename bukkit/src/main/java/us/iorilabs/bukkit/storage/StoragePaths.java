package us.iorilabs.bukkit.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class StoragePaths {

    private StoragePaths() {}

    public static File root(JavaPlugin plugin) {
        return plugin.getDataFolder();
    }

    public static File data(JavaPlugin plugin, String... segments) {
        return data(root(plugin), segments);
    }

    public static File data(File root, String... segments) {
        return join(root, prepend("data", segments));
    }

    public static File core(JavaPlugin plugin, String... segments) {
        return data(plugin, prepend("core", segments));
    }

    public static File corePlayersRoot(JavaPlugin plugin) {
        return core(plugin, "players");
    }

    /** @deprecated use {@link #data(JavaPlugin, String...)} — same folder */
    @Deprecated
    public static File storage(JavaPlugin plugin, String... segments) {
        return data(plugin, segments);
    }

    /** @deprecated use {@link #data(File, String...)} */
    @Deprecated
    public static File storage(File root, String... segments) {
        return data(root, segments);
    }

    public static File logs(JavaPlugin plugin, String... segments) {
        return join(root(plugin), prepend("logs", segments));
    }

    public static String playerFolderName(String name, UUID uuid) {
        String safe = name == null || name.isEmpty() ? "unknown" : name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return safe + "-" + uuid;
    }

    public static File playerDir(JavaPlugin plugin, String name, UUID uuid) {
        return new File(corePlayersRoot(plugin), playerFolderName(name, uuid));
    }

    public static File findPlayerDir(JavaPlugin plugin, UUID uuid) {
        if (uuid == null) return null;
        File root = corePlayersRoot(plugin);
        if (!root.isDirectory()) return null;
        String suffix = "-" + uuid;
        File[] children = root.listFiles(File::isDirectory);
        if (children == null) return null;
        for (File child : children) {
            if (child.getName().endsWith(suffix)) return child;
        }
        return null;
    }

    public static File playerDataFile(JavaPlugin plugin, String name, UUID uuid, String fileName) {
        File dir = findPlayerDir(plugin, uuid);
        if (dir == null) dir = playerDir(plugin, name, uuid);
        return new File(dir, fileName);
    }

    public static File playerDataFile(JavaPlugin plugin, UUID uuid, String fileName) {
        if (uuid == null) return new File(corePlayersRoot(plugin), fileName);
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name == null || name.isEmpty()) name = uuid.toString();
        return playerDataFile(plugin, name, uuid, fileName);
    }

    public static void migrateLegacyRoots(JavaPlugin plugin) {
        if (plugin == null) return;
        File root = root(plugin);
        migrateIfPresent(new File(root, "storage"), new File(root, "data"));
        migrateCoreLayout(plugin);
    }

    public static void migrateCoreLayout(JavaPlugin plugin) {
        if (plugin == null) return;
        File root = root(plugin);
        File dataRoot = data(plugin);
        File coreRoot = core(plugin);
        File legacyEssentials = new File(dataRoot, "essentials");
        File rootEssentials = new File(root, "essentials");

        migrateIfPresent(new File(legacyEssentials, "warps.yml"), new File(coreRoot, "warps.yml"));
        migrateIfPresent(new File(rootEssentials, "warps.yml"), new File(coreRoot, "warps.yml"));
        migrateIfPresent(new File(legacyEssentials, "spawn.yml"), new File(coreRoot, "spawn.yml"));
        migrateIfPresent(new File(rootEssentials, "spawn.yml"), new File(coreRoot, "spawn.yml"));
        migrateIfPresent(new File(legacyEssentials, "ignore.yml"), new File(coreRoot, "ignore.yml"));
        migrateIfPresent(new File(rootEssentials, "ignore.yml"), new File(coreRoot, "ignore.yml"));
        migrateIfPresent(new File(legacyEssentials, "nicks.yml"), new File(coreRoot, "nicks.yml"));
        migrateIfPresent(new File(rootEssentials, "nicks.yml"), new File(coreRoot, "nicks.yml"));

        migrateIfPresent(new File(dataRoot, "kits"), new File(coreRoot, "kits"));
        migrateIfPresent(new File(root, "kits"), new File(coreRoot, "kits"));

        migrateHomesFolder(plugin, new File(legacyEssentials, "homes"));
        migrateHomesFolder(plugin, new File(rootEssentials, "homes"));
        migrateGlobalBack(plugin, new File(legacyEssentials, "back.yml"));
        migrateGlobalBack(plugin, new File(rootEssentials, "back.yml"));

        migrateFlatPlayerFiles(plugin, new File(dataRoot, "gender"), "gender.yml");
        migrateFlatPlayerFiles(plugin, new File(dataRoot, "player-settings"), "settings.yml");
        migrateFlatPlayerFiles(plugin, new File(root, "player-settings"), "settings.yml");

        deleteIfEmpty(new File(legacyEssentials, "homes"));
        deleteIfEmpty(new File(rootEssentials, "homes"));
        deleteIfEmpty(legacyEssentials);
        deleteIfEmpty(rootEssentials);
    }

    public static void migrateIfPresent(File oldPath, File newPath) {
        if (oldPath == null || newPath == null || !oldPath.exists() || newPath.exists()) return;

        File parent = newPath.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        if (oldPath.renameTo(newPath)) return;

        try {
            if (oldPath.isDirectory()) {
                copyDirectory(oldPath, newPath);
            } else {
                Files.copy(oldPath.toPath(), newPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {}
    }

    private static void migrateHomesFolder(JavaPlugin plugin, File homesDir) {
        if (homesDir == null || !homesDir.isDirectory()) return;
        File[] files = homesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File old : files) {
            String uuidPart = old.getName().substring(0, old.getName().length() - 4);
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidPart);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            File dest = playerDataFile(plugin, uuid, "homes.yml");
            migrateIfPresent(old, dest);
        }
    }

    private static void migrateGlobalBack(JavaPlugin plugin, File backFile) {
        if (backFile == null || !backFile.isFile()) return;

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(backFile);
        ConfigurationSection section = yaml.getConfigurationSection("back");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            File dest = playerDataFile(plugin, uuid, "back.yml");
            if (dest.exists()) continue;
            Object raw = section.get(key);
            if (raw == null) continue;
            FileConfiguration out = new YamlConfiguration();
            out.set("back", raw);
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try {
                out.save(dest);
            } catch (IOException ignored) {}
        }
        if (!backFile.delete()) backFile.renameTo(new File(backFile.getParentFile(), backFile.getName() + ".migrated"));
    }

    private static void migrateFlatPlayerFiles(JavaPlugin plugin, File flatDir, String targetFileName) {
        if (flatDir == null || !flatDir.isDirectory()) return;
        File[] files = flatDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        for (File old : files) {
            String uuidPart = old.getName().substring(0, old.getName().length() - 4);
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidPart);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            File dest = playerDataFile(plugin, uuid, targetFileName);
            migrateIfPresent(old, dest);
        }
        deleteIfEmpty(flatDir);
    }

    private static void deleteIfEmpty(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] children = dir.listFiles();
        if (children != null && children.length > 0) return;
        dir.delete();
    }

    private static void copyDirectory(File source, File target) throws IOException {
        if (!target.exists() && !target.mkdirs()) return;

        File[] children = source.listFiles();
        if (children == null) return;

        for (File child : children) {
            File dest = new File(target, child.getName());
            if (child.isDirectory()) copyDirectory(child, dest);
            else Files.copy(child.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static File join(File root, String[] segments) {
        File file = root;
        for (String segment : segments) {
            if (segment == null || segment.isEmpty()) continue;
            file = new File(file, segment);
        }
        return file;
    }

    private static String[] prepend(String head, String... tail) {
        String[] out = new String[tail.length + 1];
        out[0] = head;
        System.arraycopy(tail, 0, out, 1, tail.length);
        return out;
    }
}
