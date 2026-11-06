package us.iorilabs.bukkit.modules.core;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.storage.StoragePaths;

@Getter
@Accessors(fluent = true)
public final class EssentialsState {

    private final IoriCorePlugin plugin;
    private final Map<UUID, Boolean> god = new ConcurrentHashMap<>();
    private final Map<UUID, Location> back = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> reply = new ConcurrentHashMap<>();
    private final Map<UUID, String> nicks = new ConcurrentHashMap<>();
    private final File nickFile;

    public EssentialsState(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.nickFile = StoragePaths.core(plugin, "nicks.yml");
        loadNicks();
    }

    public boolean god(UUID uuid) {
        return uuid != null && Boolean.TRUE.equals(god.get(uuid));
    }

    public boolean toggleGod(UUID uuid) {
        if (uuid == null) return false;
        boolean next = !god(uuid);
        if (next) god.put(uuid, true);
        else god.remove(uuid);
        return next;
    }

    public void setGod(UUID uuid, boolean enabled) {
        if (uuid == null) return;
        if (enabled) god.put(uuid, true);
        else god.remove(uuid);
    }

    public void rememberBack(Player player) {
        if (player == null || player.getLocation() == null) return;
        UUID uuid = player.getUniqueId();
        Location location = player.getLocation().clone();
        back.put(uuid, location);
        saveBack(uuid, player.getName(), location);
    }

    public Location takeBack(UUID uuid) {
        if (uuid == null) return null;
        Location cached = back.get(uuid);
        if (cached != null) return cached;
        Location loaded = loadBack(uuid);
        if (loaded != null) back.put(uuid, loaded);
        return loaded;
    }

    public void setReply(UUID from, UUID to) {
        if (from == null || to == null) return;
        reply.put(from, to);
        reply.put(to, from);
    }

    public UUID replyTarget(UUID uuid) {
        return uuid == null ? null : reply.get(uuid);
    }

    public void setNick(UUID uuid, String nick) {
        if (uuid == null) return;
        if (nick == null || nick.trim().isEmpty()) nicks.remove(uuid);
        else nicks.put(uuid, nick.trim());
        saveNicks();
        applyNick(Bukkit.getPlayer(uuid));
    }

    public String nick(UUID uuid) {
        return uuid == null ? null : nicks.get(uuid);
    }

    public void applyNick(Player player) {
        if (player == null) return;
        String nick = nicks.get(player.getUniqueId());
        if (nick == null || nick.isEmpty()) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            return;
        }
        String colored = org.bukkit.ChatColor.translateAlternateColorCodes('&', nick);
        player.setDisplayName(colored);
        String plain = org.bukkit.ChatColor.stripColor(colored);
        if (plain.length() > 16) plain = plain.substring(0, 16);
        player.setPlayerListName(plain);
    }

    public void clear(UUID uuid) {
        if (uuid == null) return;
        god.remove(uuid);
        reply.remove(uuid);
    }

    private Location loadBack(UUID uuid) {
        File file = StoragePaths.playerDataFile(plugin, uuid, "back.yml");
        if (!file.exists()) return null;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return EssentialsLocations.read(yaml, "back");
    }

    private void saveBack(UUID uuid, String name, Location location) {
        if (!plugin.settings().getBoolean("PERSISTENCE.BACK", true)) return;
        if (uuid == null || location == null) return;

        File file = StoragePaths.playerDataFile(plugin, name, uuid, "back.yml");
        FileConfiguration yaml = new YamlConfiguration();
        EssentialsLocations.write(yaml, "back", location);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save back location for " + uuid, ex);
        }
    }

    private void loadNicks() {
        if (!nickFile.exists()) return;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(nickFile);
        ConfigurationSection section = yaml.getConfigurationSection("nicks");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String nick = section.getString(key);
                if (nick != null && !nick.trim().isEmpty()) nicks.put(uuid, nick.trim());
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveNicks() {
        if (!plugin.settings().getBoolean("PERSISTENCE.STATS", true)) return;
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : nicks.entrySet()) {
            yaml.set("nicks." + entry.getKey(), entry.getValue());
        }
        try {
            File parent = nickFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(nickFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save nicks", ex);
        }
    }
}
