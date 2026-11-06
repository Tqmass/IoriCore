package us.iorilabs.bukkit.modules.coinshop;

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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.coinshop.enums.CoinshopOfferType;
import us.iorilabs.bukkit.modules.coinshop.enums.PurchaseResult;
import us.iorilabs.bukkit.modules.economy.EconomyModule;
import us.iorilabs.bukkit.modules.economy.EconomyService;
import us.iorilabs.bukkit.modules.tags.Tag;
import us.iorilabs.bukkit.modules.tags.TagService;
import us.iorilabs.bukkit.modules.tags.TagsModule;
import us.iorilabs.bukkit.storage.StoragePaths;

@Getter
@Accessors(fluent = true)
public final class CoinshopService {

    private final IoriCorePlugin plugin;
    private final File playersFolder;
    private final Map<String, CoinshopOffer> offers = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> ownedTags = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> ownedCosmetics = new ConcurrentHashMap<>();

    public CoinshopService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        File legacyPlayers = new File(plugin.getDataFolder(), "coinshop/players");
        this.playersFolder = StoragePaths.storage(plugin, "coinshop", "players");
        StoragePaths.migrateIfPresent(legacyPlayers, playersFolder);
        if (!playersFolder.exists()) playersFolder.mkdirs();
    }

    public void load() {
        offers.clear();
        TagService tagSvc = tagService();
        if (tagSvc != null) {
            addOffersFromTags(tagSvc);
        } else {
            loadOffersFromTagsYaml();
        }
    }

    private void addOffersFromTags(TagService tagSvc) {
        for (Tag tag : tagSvc.all()) {
            if (tag.shopPrice() <= 0.0D || tag.shopMaterial().isEmpty()) continue;
            String key = tag.id().toLowerCase(Locale.ROOT);
            offers.put(
                key,
                new CoinshopOffer(
                    key,
                    CoinshopOfferType.TAG,
                    key,
                    tag.shopPrice(),
                    tag.shopMaterial(),
                    tag.shopData(),
                    false
                )
            );
        }
    }

    private void loadOffersFromTagsYaml() {
        File file = StoragePaths.storage(plugin, "tags.yml");
        if (!file.exists()) return;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("TAGS");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            double price = section.getDouble("PRICE", 0.0D);
            String material = section.getString("MATERIAL", "");
            if (price <= 0.0D || material == null || material.isBlank()) continue;
            String key = id.toLowerCase(Locale.ROOT);
            offers.put(
                key,
                new CoinshopOffer(
                    key,
                    CoinshopOfferType.TAG,
                    key,
                    price,
                    material,
                    (short) section.getInt("DATA", 0),
                    false
                )
            );
        }
    }

    public List<CoinshopOffer> offers(CoinshopOfferType type) {
        List<CoinshopOffer> out = new ArrayList<>();
        for (CoinshopOffer offer : offers.values()) {
            if (offer.type() == type) out.add(offer);
        }
        return Collections.unmodifiableList(out);
    }

    public CoinshopOffer offer(String id) {
        if (id == null) return null;
        return offers.get(id.toLowerCase(Locale.ROOT));
    }

    public EconomyService economy() {
        EconomyModule module = plugin.modules().get(ModuleId.ECONOMY);
        return module == null ? null : module.service();
    }

    public TagService tags() {
        return tagService();
    }

    private TagService tagService() {
        if (plugin.modules() == null) return null;
        TagsModule module = plugin.modules().get(ModuleId.TAGS);
        if (module == null || !module.enabled()) return null;
        return module.service();
    }

    public boolean ownsTag(UUID uuid, String tagId) {
        if (uuid == null || tagId == null) return false;
        return owned(uuid, ownedTags).contains(tagId.toLowerCase(Locale.ROOT));
    }

    public boolean ownsCosmetic(UUID uuid, String cosmeticId) {
        if (uuid == null || cosmeticId == null) return false;
        return owned(uuid, ownedCosmetics).contains(cosmeticId.toLowerCase(Locale.ROOT));
    }

    public boolean owns(Player player, CoinshopOffer offer) {
        if (player == null || offer == null) return false;
        if (offer.type() == CoinshopOfferType.TAG) return ownsTag(player.getUniqueId(), offer.target());
        return ownsCosmetic(player.getUniqueId(), offer.target());
    }

    public PurchaseResult purchase(Player player, CoinshopOffer offer) {
        if (player == null || offer == null) return PurchaseResult.FAILED;
        if (owns(player, offer)) return PurchaseResult.ALREADY_OWNED;

        if (offer.type() == CoinshopOfferType.COSMETIC || offer.stub()) {
            return PurchaseResult.COSMETIC_UNAVAILABLE;
        }

        if (offer.type() == CoinshopOfferType.TAG) {
            TagService tags = tags();
            if (tags == null) return PurchaseResult.UNAVAILABLE;
            Tag tag = tags.get(offer.target());
            if (tag == null) return PurchaseResult.UNKNOWN;
        } else {
            return PurchaseResult.COSMETIC_UNAVAILABLE;
        }

        EconomyService economy = economy();
        if (economy == null) return PurchaseResult.UNAVAILABLE;
        if (!economy.has(player.getUniqueId(), offer.price())) return PurchaseResult.INSUFFICIENT;
        if (!economy.withdraw(player.getUniqueId(), offer.price())) return PurchaseResult.FAILED;

        owned(player.getUniqueId(), ownedTags).add(offer.target().toLowerCase(Locale.ROOT));
        saveOwned(player.getUniqueId());
        return PurchaseResult.SUCCESS;
    }

    public void unload(UUID uuid) {
        if (uuid == null) return;
        ownedTags.remove(uuid);
        ownedCosmetics.remove(uuid);
    }

    private Set<String> owned(UUID uuid, Map<UUID, Set<String>> cache) {
        return cache.computeIfAbsent(uuid, id -> {
            Set<String> loaded = ConcurrentHashMap.newKeySet();
            loaded.addAll(loadOwned(id, cache == ownedTags ? "tags" : "cosmetics"));
            return loaded;
        });
    }

    private List<String> loadOwned(UUID uuid, String key) {
        File file = fileFor(uuid);
        if (!file.exists()) return Collections.emptyList();

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<String> raw = yaml.getStringList(key);
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        List<String> out = new ArrayList<>();
        for (String value : raw) {
            if (value == null || value.trim().isEmpty()) continue;
            out.add(value.trim().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private void saveOwned(UUID uuid) {
        if (uuid == null) return;
        if (!playersFolder.exists()) playersFolder.mkdirs();

        File file = fileFor(uuid);
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("tags", new ArrayList<>(owned(uuid, ownedTags)));
        yaml.set("cosmetics", new ArrayList<>(owned(uuid, ownedCosmetics)));
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Coinshop save failed · " + uuid, ex);
        }
    }

    private File fileFor(UUID uuid) {
        return new File(playersFolder, uuid.toString() + ".yml");
    }

    private static CoinshopOfferType parseType(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return CoinshopOfferType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
