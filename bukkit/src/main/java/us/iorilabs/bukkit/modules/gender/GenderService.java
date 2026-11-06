package us.iorilabs.bukkit.modules.gender;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.storage.StoragePaths;

@Getter
@Accessors(fluent = true)
public final class GenderService {

    private final IoriCorePlugin plugin;
    private final Map<UUID, GenderKind> cache = new ConcurrentHashMap<>();
    private final Map<GenderKind, String> emoji = new ConcurrentHashMap<>();

    public GenderService(IoriCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        cache.clear();
        emoji.clear();
        FileConfiguration settings = plugin.settings();
        emoji.put(GenderKind.MALE, readEmoji(settings, "GENDER.MALE", "&b♂"));
        emoji.put(GenderKind.FEMALE, readEmoji(settings, "GENDER.FEMALE", "&d♀"));
        emoji.put(GenderKind.OTHER, readEmoji(settings, "GENDER.OTHER", "&7·"));
        emoji.put(GenderKind.UNKNOWN, readEmoji(settings, "GENDER.UNKNOWN", ""));
    }

    public GenderKind kind(UUID uuid) {
        if (uuid == null) return GenderKind.UNKNOWN;
        return cache.computeIfAbsent(uuid, this::readKind);
    }

    public String display(UUID uuid) {
        return colored(emoji.getOrDefault(kind(uuid), ""));
    }

    public String display(Player player) {
        return player == null ? "" : display(player.getUniqueId());
    }

    public boolean set(Player player, GenderKind kind) {
        if (player == null || kind == null || kind == GenderKind.UNKNOWN) return false;
        cache.put(player.getUniqueId(), kind);
        save(player.getUniqueId(), player.getName(), kind);
        return true;
    }

    public boolean clear(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        cache.put(uuid, GenderKind.UNKNOWN);
        File file = fileFor(uuid);
        if (file.exists() && !file.delete()) {
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            yaml.set("gender", null);
            try {
                yaml.save(file);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "Gender clear failed · " + uuid, ex);
                return false;
            }
        }
        return true;
    }

    public void unload(UUID uuid) {
        if (uuid == null) return;
        cache.remove(uuid);
    }

    private GenderKind readKind(UUID uuid) {
        File file = fileFor(uuid);
        if (!file.exists()) return GenderKind.UNKNOWN;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return GenderKind.parse(yaml.getString("gender", ""));
    }

    private void save(UUID uuid, String name, GenderKind kind) {
        if (uuid == null || kind == null) return;
        File file = StoragePaths.playerDataFile(plugin, name, uuid, "gender.yml");
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("gender", kind.name().toLowerCase(Locale.ROOT));
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Gender save failed · " + uuid, ex);
        }
    }

    private File fileFor(UUID uuid) {
        return StoragePaths.playerDataFile(plugin, uuid, "gender.yml");
    }

    private static String readEmoji(FileConfiguration settings, String path, String fallback) {
        if (settings == null) return fallback;
        String value = settings.getString(path, fallback);
        return value == null ? fallback : value;
    }

    private static String colored(String input) {
        if (input == null || input.isEmpty()) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
