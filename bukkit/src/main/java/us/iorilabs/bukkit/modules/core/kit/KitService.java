package us.iorilabs.bukkit.modules.core.kit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsLocations;
import us.iorilabs.bukkit.storage.StoragePaths;

@Getter
@Accessors(fluent = true)
public final class KitService {

    private final IoriCorePlugin plugin;
    private final File kitsFolder;
    private final File cooldownFile;
    private final Map<String, KitDefinition> kits = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public KitService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.kitsFolder = StoragePaths.core(plugin, "kits");
        this.cooldownFile = StoragePaths.core(plugin, "kits", "cooldowns.yml");
        load();
        loadCooldowns();
    }

    public void load() {
        kits.clear();
        if (!kitsFolder.exists() && !kitsFolder.mkdirs()) {
            plugin.getLogger().warning("Cannot create kits folder");
            return;
        }

        File[] files = kitsFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String base = file.getName();
            if ("cooldowns.yml".equalsIgnoreCase(base)) continue;
            String name = EssentialsLocations.normalizeKey(base.substring(0, base.length() - 4));
            if (name.isEmpty()) continue;

            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            long cooldown = yaml.getLong("cooldown-seconds", yaml.getLong("cooldown", 0L));
            String permission = yaml.getString("permission", "ioricore.kit." + name);
            List<ItemStack> items = readItems(yaml);
            kits.put(name, new KitDefinition(name, permission, cooldown, items));
        }
    }

    public KitDefinition get(String name) {
        return kits.get(EssentialsLocations.normalizeKey(name));
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(kits.keySet());
    }

    public boolean create(String name, Player player) {
        if (player == null) return false;
        String key = EssentialsLocations.normalizeKey(name);
        if (key.isEmpty() || "cooldowns".equals(key)) return false;

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() != org.bukkit.Material.AIR) items.add(stack.clone());
        }
        if (items.isEmpty()) return false;

        File file = new File(kitsFolder, key + ".yml");
        FileConfiguration yaml = new YamlConfiguration();
        yaml.set("cooldown-seconds", 0L);
        yaml.set("permission", "ioricore.kit." + key);
        yaml.set("items", items);
        try {
            if (!kitsFolder.exists()) kitsFolder.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save kit " + key, ex);
            return false;
        }

        kits.put(key, new KitDefinition(key, "ioricore.kit." + key, 0L, items));
        return true;
    }

    public boolean delete(String name) {
        String key = EssentialsLocations.normalizeKey(name);
        if (key.isEmpty()) return false;
        if (kits.remove(key) == null && !new File(kitsFolder, key + ".yml").exists()) return false;

        File file = new File(kitsFolder, key + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Failed to delete kit file " + file.getName());
        }
        for (Map<String, Long> map : cooldowns.values()) {
            map.remove(key);
        }
        saveCooldowns();
        return true;
    }

    public long remainingCooldown(UUID playerId, KitDefinition kit) {
        if (playerId == null || kit == null || kit.cooldownSeconds() <= 0L) return 0L;
        Map<String, Long> map = cooldowns.get(playerId);
        if (map == null) return 0L;
        Long last = map.get(kit.name());
        if (last == null) return 0L;
        long ends = last + kit.cooldownSeconds() * 1000L;
        return Math.max(0L, ends - System.currentTimeMillis());
    }

    public boolean give(Player player, KitDefinition kit) {
        if (player == null || kit == null) return false;
        long remaining = remainingCooldown(player.getUniqueId(), kit);
        if (remaining > 0L) return false;

        for (ItemStack item : kit.items()) {
            if (item == null) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        if (kit.cooldownSeconds() > 0L) {
            cooldowns
                .computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
                .put(kit.name(), System.currentTimeMillis());
            saveCooldowns();
        }
        return true;
    }

    private static List<ItemStack> readItems(FileConfiguration yaml) {
        List<ItemStack> items = new ArrayList<>();
        List<?> raw = yaml.getList("items");
        if (raw == null) return items;
        for (Object entry : raw) {
            if (entry instanceof ItemStack) items.add(((ItemStack) entry).clone());
        }
        return items;
    }

    private void loadCooldowns() {
        cooldowns.clear();
        if (!cooldownFile.exists()) return;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(cooldownFile);
        for (String uuidKey : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidKey);
                Map<String, Long> map = new ConcurrentHashMap<>();
                if (yaml.isConfigurationSection(uuidKey)) {
                    for (String kit : yaml.getConfigurationSection(uuidKey).getKeys(false)) {
                        map.put(EssentialsLocations.normalizeKey(kit), yaml.getLong(uuidKey + "." + kit));
                    }
                }
                if (!map.isEmpty()) cooldowns.put(uuid, map);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveCooldowns() {
        if (!plugin.settings().getBoolean("PERSISTENCE.KITS", true)) return;
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
            for (Map.Entry<String, Long> kit : entry.getValue().entrySet()) {
                yaml.set(entry.getKey() + "." + kit.getKey(), kit.getValue());
            }
        }
        try {
            if (!kitsFolder.exists()) kitsFolder.mkdirs();
            yaml.save(cooldownFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save kit cooldowns", ex);
        }
    }
}
