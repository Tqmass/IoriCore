package us.iorilabs.bukkit.menu.api.yaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.ConfigurationSection;
import us.iorilabs.bukkit.menu.enums.MenuClickType;

@Getter
@Setter
@Accessors(fluent = true)
public final class YamlMenuItemDef {

    private String key = "";
    private List<Integer> slots = Collections.emptyList();
    private String material = "STONE";
    private String materialFallback = "";
    private short data;
    private int customModelData = -1;
    private int amount = 1;
    private String name = "";
    private String nameKey = "";
    private List<String> lore = Collections.emptyList();
    private String loreKey = "";
    private boolean glow;
    private List<String> enchants = Collections.emptyList();
    private List<String> flags = Collections.emptyList();
    private String skullOwner = "";
    private String skullTexture = "";
    private boolean skullSelf;
    private List<YamlRequirement> viewRequirements = Collections.emptyList();
    private List<YamlRequirement> clickRequirements = Collections.emptyList();
    private List<YamlRequirement> stateRequirements = Collections.emptyList();
    private Map<String, YamlMenuItemDef> states = Collections.emptyMap();
    private String itemSound = "";
    private Map<MenuClickType, List<YamlAction>> actions = Collections.emptyMap();
    private List<YamlAction> denyActions = Collections.emptyList();

    public static YamlMenuItemDef from(String key, ConfigurationSection section) {
        YamlMenuItemDef def = new YamlMenuItemDef();
        if (section == null) return def;
        def.key(key == null ? "" : key);
        Object slotRaw = section.contains("SLOTS") ? section.get("SLOTS") : section.get("SLOT");
        def.slots(YamlSlotParser.parse(slotRaw));
        def.material(section.getString("MATERIAL", "STONE"));
        def.materialFallback(section.getString("MATERIAL-FALLBACK", section.getString("FALLBACK", "")));
        def.data((short) section.getInt("DATA", 0));
        def.customModelData(section.getInt("CMD", section.getInt("CUSTOM-MODEL-DATA", -1)));
        def.amount(Math.max(1, section.getInt("AMOUNT", 1)));
        def.name(section.getString("NAME", ""));
        def.nameKey(section.getString("NAME-KEY", ""));
        def.lore(section.getStringList("LORE"));
        def.loreKey(section.getString("LORE-KEY", ""));
        def.glow(section.getBoolean("GLOW", false));
        def.enchants(section.getStringList("ENCHANTS"));
        def.flags(section.getStringList("FLAGS"));
        def.skullOwner(section.getString("SKULL.OWNER", section.getString("SKULL-OWNER", "")));
        def.skullTexture(section.getString("SKULL.TEXTURE", section.getString("SKULL-TEXTURE", "")));
        def.skullSelf(section.getBoolean("SKULL.SELF", section.getBoolean("SKULL-SELF", false)));
        def.viewRequirements(YamlRequirement.parseList(section.getList("VIEW-REQUIREMENTS")));
        def.clickRequirements(YamlRequirement.parseList(section.getList("CLICK-REQUIREMENTS")));
        def.stateRequirements(YamlRequirement.parseList(section.getList("REQUIREMENTS")));
        def.itemSound(section.getString("SOUND", ""));
        ConfigurationSection statesSection = section.getConfigurationSection("STATES");
        if (statesSection != null) {
            Map<String, YamlMenuItemDef> stateMap = new LinkedHashMap<>();
            for (String stateKey : statesSection.getKeys(false)) {
                ConfigurationSection st = statesSection.getConfigurationSection(stateKey);
                if (st == null) continue;
                stateMap.put(stateKey.toUpperCase(Locale.ROOT), YamlMenuItemDef.from(stateKey, st));
            }
            def.states(stateMap);
        }
        def.actions(parseActions(section.get("ACTIONS")));
        def.denyActions(parseActionList(section.get("DENY-ACTIONS")));
        return def;
    }

    public static YamlMenuItemDef fromTemplate(ConfigurationSection section) {
        return from("TEMPLATE", section);
    }

    @SuppressWarnings("unchecked")
    private static Map<MenuClickType, List<YamlAction>> parseActions(Object raw) {
        if (raw == null) return Collections.emptyMap();
        Map<MenuClickType, List<YamlAction>> out = new EnumMap<>(MenuClickType.class);
        if (raw instanceof List) {
            out.put(MenuClickType.ANY, parseActionList(raw));
            return out;
        }
        if (raw instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) raw;
            for (String key : section.getKeys(false)) {
                MenuClickType click = MenuClickType.parse(key);
                if (click == null) click = MenuClickType.ANY;
                out.put(click, parseActionList(section.get(key)));
            }
            return out;
        }
        if (raw instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
                MenuClickType click = MenuClickType.parse(String.valueOf(entry.getKey()));
                if (click == null) click = MenuClickType.ANY;
                out.put(click, parseActionList(entry.getValue()));
            }
            return out;
        }
        if (raw instanceof String) {
            YamlAction action = YamlAction.parse((String) raw);
            if (action != null) out.put(MenuClickType.ANY, Collections.singletonList(action));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<YamlAction> parseActionList(Object raw) {
        if (raw == null) return Collections.emptyList();
        List<YamlAction> out = new ArrayList<>();
        if (raw instanceof String) {
            YamlAction action = YamlAction.parse((String) raw);
            if (action != null) out.add(action);
            return out;
        }
        if (raw instanceof List) {
            for (Object entry : (List<?>) raw) {
                if (entry == null) continue;
                YamlAction action = YamlAction.parse(String.valueOf(entry));
                if (action != null) out.add(action);
            }
            return out;
        }
        return out;
    }

    public List<YamlAction> actionsFor(MenuClickType click) {
        if (actions == null || actions.isEmpty()) return Collections.emptyList();
        List<YamlAction> exact = actions.get(click);
        if (exact != null && !exact.isEmpty()) return exact;
        List<YamlAction> any = actions.get(MenuClickType.ANY);
        return any == null ? Collections.emptyList() : any;
    }

    public YamlMenuItemDef copyWithPlaceholders(Map<String, String> values) {
        YamlMenuItemDef copy = new YamlMenuItemDef();
        copy.key(key);
        copy.slots(slots);
        copy.material(YamlPlaceholders.apply(material, null, values));
        copy.materialFallback(YamlPlaceholders.apply(materialFallback, null, values));
        copy.data(data);
        copy.customModelData(customModelData);
        copy.amount(amount);
        copy.name(name);
        copy.nameKey(nameKey);
        copy.lore(lore == null ? Collections.emptyList() : new ArrayList<>(lore));
        copy.loreKey(loreKey);
        String glowFlag = values == null ? null : values.get("glow");
        if (glowFlag != null && !glowFlag.isEmpty()) {
            copy.glow("true".equalsIgnoreCase(glowFlag) || "1".equals(glowFlag));
        } else {
            copy.glow(glow);
        }
        copy.enchants(enchants);
        copy.flags(flags);
        copy.skullOwner(skullOwner);
        copy.skullTexture(skullTexture);
        copy.skullSelf(skullSelf);
        copy.viewRequirements(viewRequirements);
        copy.clickRequirements(clickRequirements);
        copy.stateRequirements(stateRequirements);
        copy.states(states);
        copy.itemSound(itemSound);
        Map<MenuClickType, List<YamlAction>> mapped = new EnumMap<>(MenuClickType.class);
        if (actions != null) {
            for (Map.Entry<MenuClickType, List<YamlAction>> entry : actions.entrySet()) {
                List<YamlAction> rewritten = new ArrayList<>();
                for (YamlAction action : entry.getValue()) {
                    rewritten.add(
                        new YamlAction(action.type(), YamlPlaceholders.apply(action.argument(), null, values))
                    );
                }
                mapped.put(entry.getKey(), rewritten);
            }
        }
        copy.actions(mapped);
        List<YamlAction> denies = new ArrayList<>();
        if (denyActions != null) {
            for (YamlAction action : denyActions) {
                denies.add(new YamlAction(action.type(), YamlPlaceholders.apply(action.argument(), null, values)));
            }
        }
        copy.denyActions(denies);
        return copy;
    }

    public Map<String, Object> debug() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", key);
        map.put("slots", slots);
        map.put("material", material);
        return map;
    }

    public boolean hasSlot(int slot) {
        return slots != null && slots.contains(slot);
    }

    public String materialKey() {
        return material == null ? "STONE" : material.trim().toUpperCase(Locale.ROOT);
    }
}
