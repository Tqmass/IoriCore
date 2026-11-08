package us.iorilabs.bukkit.modules.tags;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.storage.StoragePaths;

@Getter
@Accessors(fluent = true)
public final class TagService {

    private static final int TAGS_CONFIG_VERSION = 1;

    private final IoriCorePlugin plugin;
    private final File definitionsFile;
    private final File playersFolder;
    private final boolean persist;
    private final Map<String, Tag> tags = new LinkedHashMap<>();
    private final Map<UUID, String> equipped = new ConcurrentHashMap<>();

    public TagService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.persist = plugin.settings().getBoolean("PERSISTENCE.TAGS", true);
        File legacyDefs = new File(plugin.getDataFolder(), "data/tags.yml");
        this.definitionsFile = StoragePaths.storage(plugin, "tags.yml");
        StoragePaths.migrateIfPresent(legacyDefs, definitionsFile);
        File legacyPlayers = new File(plugin.getDataFolder(), "tags/players");
        this.playersFolder = StoragePaths.storage(plugin, "tags", "players");
        StoragePaths.migrateIfPresent(legacyPlayers, playersFolder);
        if (persist && !playersFolder.exists()) playersFolder.mkdirs();
    }

    public void load() {
        tags.clear();
        if (!definitionsFile.exists()) {
            if (plugin.getResource("tags.yml") != null) {
                us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "tags.yml");
            } else {
                us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "data/tags.yml");
            }
        }

        migrateCoinshopIntoTags(definitionsFile);

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(definitionsFile);
        ConfigurationSection root = yaml.getConfigurationSection("TAGS");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            String key = id.toLowerCase(Locale.ROOT);
            tags.put(
                key,
                new Tag(
                    key,
                    section.getString("DISPLAY", id),
                    section.getString("PERMISSION", ""),
                    section.getString("EMOJI", ""),
                    section.getString("CHAT", ""),
                    section.getStringList("HOVER"),
                    section.getDouble("PRICE", 0.0D),
                    section.getString("MATERIAL", ""),
                    (short) section.getInt("DATA", 0)
                )
            );
        }
    }

    public Collection<Tag> all() {
        return Collections.unmodifiableCollection(tags.values());
    }

    public Tag get(String id) {
        if (id == null) return null;
        return tags.get(id.toLowerCase(Locale.ROOT));
    }

    public List<Tag> visibleFor(Player player) {
        List<Tag> out = new ArrayList<>();
        for (Tag tag : tags.values()) {
            if (canUse(player, tag)) out.add(tag);
        }
        return out;
    }

    public boolean canUse(Player player, Tag tag) {
        if (player == null || tag == null) return false;
        if (player.hasPermission("ioricore.admin")) return true;
        if (ownsFromShop(player.getUniqueId(), tag.id())) return true;
        if (tag.permission() == null || tag.permission().trim().isEmpty()) return true;
        return player.hasPermission(tag.permission());
    }

    private boolean ownsFromShop(UUID uuid, String tagId) {
        if (plugin.modules() == null) return false;
        us.iorilabs.bukkit.modules.coinshop.CoinshopModule module = plugin.modules().get(ModuleId.COINSHOP);
        if (module == null || !module.enabled()) return false;
        us.iorilabs.bukkit.modules.coinshop.CoinshopService shop = module.service();
        return shop != null && shop.ownsTag(uuid, tagId);
    }

    public Tag equipped(UUID uuid) {
        if (uuid == null) return null;
        String id = equipped.get(uuid);
        if (id == null) {
            id = loadEquipped(uuid);
            if (id != null) equipped.put(uuid, id);
        }
        if (id == null || id.isEmpty()) return null;
        return get(id);
    }

    public String fragment(UUID uuid) {
        Tag tag = equipped(uuid);
        return tag == null ? "" : tag.fragment();
    }

    public Tag equippedTag(UUID uuid) {
        return equipped(uuid);
    }

    public boolean equip(Player player, String id) {
        if (player == null || id == null) return false;
        Tag tag = get(id);
        if (tag == null) return false;
        if (!canUse(player, tag)) return false;

        equipped.put(player.getUniqueId(), tag.id());
        saveEquipped(player.getUniqueId(), tag.id());
        return true;
    }

    public boolean unequip(UUID uuid) {
        if (uuid == null) return false;
        String previous = equipped.remove(uuid);
        saveEquipped(uuid, null);
        return previous != null && !previous.isEmpty();
    }

    public void unload(UUID uuid) {
        if (uuid == null) return;
        equipped.remove(uuid);
    }

    private String loadEquipped(UUID uuid) {
        if (!persist) return null;
        File file = fileFor(uuid);
        if (!file.exists()) return null;

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = yaml.getString("equipped", "");
        return id == null || id.trim().isEmpty() ? null : id.trim().toLowerCase(Locale.ROOT);
    }

    private void saveEquipped(UUID uuid, String id) {
        if (!persist || uuid == null) return;
        if (!playersFolder.exists()) playersFolder.mkdirs();

        File file = fileFor(uuid);
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("equipped", id == null || id.isEmpty() ? null : id);
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Tag equip save failed · " + uuid, ex);
        }
    }

    private File fileFor(UUID uuid) {
        return new File(playersFolder, uuid.toString() + ".yml");
    }

    private void migrateCoinshopIntoTags(File tagsFile) {
        File legacy = new File(plugin.getDataFolder(), "data/coinshop.yml");
        if (!legacy.exists()) {
            ensureTagsCatalogVersion(tagsFile);
            return;
        }

        FileConfiguration tagsYaml = YamlConfiguration.loadConfiguration(tagsFile);
        FileConfiguration shopYaml = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection offers = shopYaml.getConfigurationSection("OFFERS");
        if (offers == null) {
            archiveCoinshopLegacy(legacy);
            ensureTagsCatalogVersion(tagsFile);
            return;
        }

        ConfigurationSection tagRoot = tagsYaml.getConfigurationSection("TAGS");
        if (tagRoot == null) {
            archiveCoinshopLegacy(legacy);
            ensureTagsCatalogVersion(tagsFile);
            return;
        }

        boolean changed = false;
        for (String offerId : offers.getKeys(false)) {
            ConfigurationSection offer = offers.getConfigurationSection(offerId);
            if (offer == null) continue;
            String typeRaw = offer.getString("TYPE", "TAG");
            if (typeRaw != null && !typeRaw.trim().equalsIgnoreCase("TAG")) continue;

            String target = offer.getString("TARGET", offerId);
            if (target == null || target.isBlank()) target = offerId;
            String key = findTagKey(tagRoot, target);
            if (key == null) continue;

            ConfigurationSection tagSec = tagRoot.getConfigurationSection(key);
            if (tagSec == null) continue;

            if (tagSec.contains("PRICE") && tagSec.getDouble("PRICE", 0.0D) > 0.0D) continue;

            tagSec.set("PRICE", offer.getDouble("PRICE", 0.0D));
            tagSec.set("MATERIAL", offer.getString("MATERIAL", "NAME_TAG"));
            tagSec.set("DATA", offer.getInt("DATA", 0));
            changed = true;
        }

        if (changed || !tagsYaml.contains("CONFIG-VERSION")) {
            tagsYaml.set("CONFIG-VERSION", TAGS_CONFIG_VERSION);
            backupFile(tagsFile);
            try {
                tagsYaml.save(tagsFile);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "Tags · migrate save failed · " + tagsFile.getName(), ex);
            }
        }

        archiveCoinshopLegacy(legacy);
    }

    private static void ensureTagsCatalogVersion(File tagsFile) {
        if (tagsFile == null || !tagsFile.exists()) return;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(tagsFile);
        if (yaml.contains("CONFIG-VERSION") && yaml.getInt("CONFIG-VERSION", 0) >= TAGS_CONFIG_VERSION) return;

        yaml.set("CONFIG-VERSION", TAGS_CONFIG_VERSION);
        backupFile(tagsFile);
        try {
            yaml.save(tagsFile);
        } catch (IOException ignored) {}
    }

    private static String findTagKey(ConfigurationSection tagRoot, String wanted) {
        if (tagRoot == null || wanted == null) return null;
        String norm = wanted.trim().toLowerCase(Locale.ROOT);
        for (String id : tagRoot.getKeys(false)) {
            if (id != null && id.trim().toLowerCase(Locale.ROOT).equals(norm)) return id;
        }
        return null;
    }

    private void archiveCoinshopLegacy(File legacy) {
        if (legacy == null || !legacy.exists()) return;
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File dest = new File(legacy.getParentFile(), "coinshop.yml.bak-migrated-" + stamp);
        if (!legacy.renameTo(dest)) {
            plugin.getLogger().warning("Tags · could not archive " + legacy.getName() + " (delete manually)");
        }
    }

    private static void backupFile(File target) {
        if (target == null || !target.exists()) return;
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File backup = new File(target.getParentFile(), target.getName() + ".bak-" + stamp);
        try {
            Files.copy(target.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {}
    }
}
