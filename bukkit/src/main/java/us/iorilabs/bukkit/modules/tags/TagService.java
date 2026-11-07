package us.iorilabs.bukkit.modules.tags;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    private final IoriCorePlugin plugin;
    private final File definitionsFile;
    private final boolean persist;
    private final Map<String, Tag> tags = new LinkedHashMap<>();
    private final Map<UUID, String> equipped = new ConcurrentHashMap<>();

    public TagService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.persist = plugin.settings().getBoolean("PERSISTENCE.TAGS", true);
        this.definitionsFile = StoragePaths.catalog(plugin, "tags.yml");
    }

    public void load() {
        tags.clear();
        if (!definitionsFile.exists()) {
            us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "tags.yml");
        }

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
        File file = tagsFile(uuid);
        if (!file.exists()) return null;

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = yaml.getString("equipped", "");
        return id == null || id.trim().isEmpty() ? null : id.trim().toLowerCase(Locale.ROOT);
    }

    private void saveEquipped(UUID uuid, String id) {
        if (!persist || uuid == null) return;

        File file = tagsFile(uuid);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("equipped", id == null || id.isEmpty() ? null : id);
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Tag equip save failed · " + uuid, ex);
        }
    }

    private File tagsFile(UUID uuid) {
        return StoragePaths.playerDataFile(plugin, uuid, "tags.yml");
    }
}
