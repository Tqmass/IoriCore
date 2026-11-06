package us.iorilabs.bukkit.menu.api.yaml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.bukkit.menu.enums.FillMode;
import us.iorilabs.bukkit.menu.enums.MenuVariant;
import us.iorilabs.bukkit.util.LegacyMaterials;

@Getter
@Setter
@Accessors(fluent = true)
public final class YamlMenuDefinition {

    private String id = "";
    private MenuVariant variant = MenuVariant.MODERN;
    private String title = "&8Menu";
    private String titleKey = "";
    private int rows = 3;
    private String type = "CHEST";
    private boolean border;
    private FillMode fillMode = FillMode.NONE;
    private YamlMenuItemDef filler;
    private YamlMenuItemDef accentFiller;
    private List<Integer> accentSlots = Collections.emptyList();
    private Map<String, YamlMenuItemDef> items = Collections.emptyMap();
    private String dynamicSource = "";
    private List<Integer> dynamicSlots = Collections.emptyList();
    private YamlMenuItemDef dynamicTemplate;
    private String parentId = "";
    private String themeName = "";
    private int pagePrev = -1;
    private int pageNext = -1;
    private int pageInfo = -1;
    private String soundOpen = "";
    private String soundClick = "";
    private String soundDeny = "";
    private int updateInterval;
    private YamlConfiguration raw = new YamlConfiguration();

    public static YamlMenuDefinition from(YamlConfiguration yaml, MenuVariant variant, String fallbackId) {
        YamlMenuDefinition def = new YamlMenuDefinition();
        if (yaml == null) return def;
        def.raw(yaml);
        def.variant(variant == null ? MenuVariant.MODERN : variant);
        String id = yaml.getString("ID", fallbackId == null ? "" : fallbackId);
        def.id(id == null ? "" : id.trim().toLowerCase(Locale.ROOT));
        def.title(yaml.getString("TITLE", "&8Menu"));
        def.titleKey(yaml.getString("TITLE-KEY", ""));
        def.rows(Math.max(1, Math.min(6, yaml.getInt("ROWS", 3))));
        def.type(yaml.getString("TYPE", "CHEST"));
        def.parentId(normalizeId(yaml.getString("PARENT", "")));
        def.themeName(yaml.getString("THEME", "").trim());
        def.border(yaml.getBoolean("BORDER", yaml.getBoolean("BORDER-PATTERN", false)));
        boolean fillAll = yaml.getBoolean("FILL", false);
        def.fillMode(fillAll ? FillMode.FILL : def.border() ? FillMode.BORDER : FillMode.NONE);
        if (yaml.contains("FILL-MODE")) def.fillMode(FillMode.parse(yaml.getString("FILL-MODE"), def.fillMode()));

        ConfigurationSection fillerSection = yaml.getConfigurationSection("FILLER");
        if (fillerSection != null) def.filler(YamlMenuItemDef.from("FILLER", fillerSection));

        ConfigurationSection accent = yaml.getConfigurationSection("ACCENT-FILLER");
        if (accent != null) {
            def.accentFiller(YamlMenuItemDef.from("ACCENT", accent));
            def.accentSlots(YamlSlotParser.parse(accent.get("SLOTS")));
        }

        Map<String, YamlMenuItemDef> items = new LinkedHashMap<>();
        ConfigurationSection itemsSection = yaml.getConfigurationSection("ITEMS");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection item = itemsSection.getConfigurationSection(key);
                if (item == null) continue;
                if (!item.contains("SLOT") && !item.contains("SLOTS")) {
                    int slot = yaml.getInt("SLOTS." + key, -1);
                    if (slot >= 0) item.set("SLOT", slot);
                }
                items.put(key.toUpperCase(Locale.ROOT), YamlMenuItemDef.from(key, item));
            }
        }
        def.items(items);

        ConfigurationSection dynamic = yaml.getConfigurationSection("DYNAMIC");
        if (dynamic != null) {
            def.dynamicSource(dynamic.getString("SOURCE", ""));
            Object slots = dynamic.contains("SLOTS") ? dynamic.get("SLOTS") : yaml.get("CONTENT-SLOTS");
            def.dynamicSlots(YamlSlotParser.parse(slots));
            ConfigurationSection template = dynamic.getConfigurationSection("TEMPLATE");
            if (template != null) def.dynamicTemplate(YamlMenuItemDef.fromTemplate(template));
        } else if (yaml.contains("CONTENT-SLOTS")) {
            def.dynamicSlots(YamlSlotParser.parse(yaml.get("CONTENT-SLOTS")));
        }

        ConfigurationSection pagination = yaml.getConfigurationSection("PAGINATION");
        if (pagination != null) {
            def.pagePrev(pagination.getInt("PREV", -1));
            def.pageNext(pagination.getInt("NEXT", -1));
            def.pageInfo(pagination.getInt("INFO", -1));
        }

        ConfigurationSection sound = yaml.getConfigurationSection("SOUND");
        if (sound != null) {
            def.soundOpen(sound.getString("OPEN", ""));
            def.soundClick(sound.getString("CLICK", ""));
            def.soundDeny(sound.getString("DENY", ""));
        }
        def.updateInterval(Math.max(0, yaml.getInt("UPDATE-INTERVAL", 0)));
        return def;
    }

    private static String normalizeId(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public int size() {
        return rows * 9;
    }

    public YamlMenuItemDef item(String key) {
        if (key == null || items == null) return null;
        return items.get(key.toUpperCase(Locale.ROOT));
    }

    public int slot(String key, int fallback) {
        YamlMenuItemDef item = item(key);
        if (item != null && item.slots() != null && !item.slots().isEmpty()) return item.slots().get(0);
        return raw == null ? fallback : raw.getInt("SLOTS." + key, fallback);
    }

    public String itemMaterial(String key, String fallback) {
        YamlMenuItemDef item = item(key);
        if (item == null) return fallback;
        String material = item.material();
        return material == null || material.trim().isEmpty() ? fallback : material;
    }

    public short itemData(String key, short fallback) {
        YamlMenuItemDef item = item(key);
        return item == null ? fallback : item.data();
    }

    public org.bukkit.Material resolveMaterial(String key, org.bukkit.Material fallback) {
        YamlMenuItemDef item = item(key);
        if (item == null) return fallback;
        org.bukkit.Material primary = LegacyMaterials.of(item.materialKey());
        if (primary != org.bukkit.Material.STONE || fallback == null) return primary;
        if (item.materialFallback() != null && !item.materialFallback().trim().isEmpty()) {
            return LegacyMaterials.of(item.materialFallback(), fallback.name());
        }
        return fallback;
    }

    public boolean inventoryMenu() {
        return !"HOTBAR".equalsIgnoreCase(type);
    }
}
