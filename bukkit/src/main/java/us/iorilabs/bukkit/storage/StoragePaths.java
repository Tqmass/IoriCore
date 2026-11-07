package us.iorilabs.bukkit.storage;

import java.io.File;
import java.util.UUID;
import org.bukkit.Bukkit;
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

    /** @deprecated use {@link #data(JavaPlugin, String...)} */
    @Deprecated
    public static File core(JavaPlugin plugin, String... segments) {
        return data(plugin, segments);
    }

    public static File playersRoot(JavaPlugin plugin) {
        return data(plugin, "players");
    }

    /** @deprecated use {@link #data(JavaPlugin, String...)} */
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
        return new File(playersRoot(plugin), playerFolderName(name, uuid));
    }

    public static File findPlayerDir(JavaPlugin plugin, UUID uuid) {
        if (uuid == null) return null;
        File root = playersRoot(plugin);
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
        if (uuid == null) return new File(playersRoot(plugin), fileName);
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name == null || name.isEmpty()) name = uuid.toString();
        return playerDataFile(plugin, name, uuid, fileName);
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
