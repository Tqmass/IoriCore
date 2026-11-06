package us.iorilabs.bukkit.menu.api.yaml;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.menu.MenuColors;
import us.iorilabs.bukkit.util.LegacyMaterials;

public final class YamlItemFactory {

    private static final Method SET_CUSTOM_MODEL_DATA = resolveCmdSetter();

    private final IoriCorePlugin plugin;

    public YamlItemFactory(IoriCorePlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    public ItemStack build(Player player, YamlMenuItemDef def, Map<String, String> placeholders) {
        return build(player, def, placeholders, null, null);
    }

    @SuppressWarnings("deprecation")
    public ItemStack build(
        Player player,
        YamlMenuItemDef def,
        Map<String, String> placeholders,
        YamlMenuService menus,
        YamlMenuDefinition menuDef
    ) {
        if (def == null) return new ItemStack(Material.STONE);
        YamlMenuItemDef effective = menus != null ? YamlMenuItemState.resolve(player, def, placeholders) : def;
        def = effective;
        Material material = resolveMaterial(def);
        ItemStack stack = new ItemStack(material, Math.max(1, def.amount()), def.data());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        Map<String, String> values = YamlPlaceholders.base(player, placeholders);
        String name = resolveName(player, def, values);
        if (name != null && !name.isEmpty()) meta.setDisplayName(MenuColors.legacy(name));

        List<String> lore = resolveLore(player, def, values, menus);
        if (!lore.isEmpty()) {
            List<String> colored = new ArrayList<>(lore.size());
            for (String line : lore) colored.add(MenuColors.legacy(line));
            meta.setLore(colored);
        }

        applyEnchants(meta, def);
        applyFlags(meta, def);
        if (def.glow() && meta.getEnchants().isEmpty()) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        applyCustomModelData(meta, def.customModelData());
        if (meta instanceof SkullMeta) applySkull(player, (SkullMeta) meta, def, values);

        stack.setItemMeta(meta);
        return stack;
    }

    private Material resolveMaterial(YamlMenuItemDef def) {
        Material primary = LegacyMaterials.of(def.materialKey());
        if (primary != Material.STONE) return primary;
        if (def.materialFallback() != null && !def.materialFallback().trim().isEmpty()) {
            return LegacyMaterials.of(def.materialFallback().trim().toUpperCase(Locale.ROOT));
        }
        return Material.STONE;
    }

    private String resolveName(Player player, YamlMenuItemDef def, Map<String, String> values) {
        CoreLanguage language = plugin.language();
        if (def.nameKey() != null && !def.nameKey().trim().isEmpty() && language != null) {
            return language.rawFor(player, def.nameKey().trim(), values);
        }
        return YamlPlaceholders.apply(def.name(), player, values);
    }

    private List<String> resolveLore(
        Player player,
        YamlMenuItemDef def,
        Map<String, String> values,
        YamlMenuService menus
    ) {
        CoreLanguage language = plugin.language();
        List<String> lore;
        if (def.loreKey() != null && !def.loreKey().trim().isEmpty() && language != null) {
            lore = language.listFor(player, def.loreKey().trim(), values);
        } else {
            lore = YamlPlaceholders.apply(def.lore(), player, values);
        }
        if (lore == null || lore.isEmpty()) return List.of();
        int sepW = menus == null ? 26 : menus.menuSeparatorWidth();
        String sep = "&8&m" + repeatSep(sepW) + "&r";
        List<String> filtered = new ArrayList<>(lore.size());
        for (String line : lore) {
            if (line == null || line.trim().isEmpty()) continue;
            String resolved = line.contains("{sep}") ? line.replace("{sep}", sep) : line;
            filtered.add(resolved);
        }
        return filtered;
    }

    private static String repeatSep(int width) {
        int n = Math.max(1, Math.min(64, width));
        StringBuilder b = new StringBuilder(n);
        for (int i = 0; i < n; i++) b.append(' ');
        return b.toString();
    }

    private void applyEnchants(ItemMeta meta, YamlMenuItemDef def) {
        if (def.enchants() == null) return;
        for (String raw : def.enchants()) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String[] parts = raw.trim().split(":");
            Enchantment enchantment = Enchantment.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
            if (enchantment == null) continue;
            int level = 1;
            if (parts.length > 1) {
                try {
                    level = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {}
            }
            meta.addEnchant(enchantment, Math.max(1, level), true);
        }
    }

    private void applyFlags(ItemMeta meta, YamlMenuItemDef def) {
        if (def.flags() == null || def.flags().isEmpty()) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            return;
        }
        for (String raw : def.flags()) {
            if (raw == null || raw.trim().isEmpty()) continue;
            try {
                meta.addItemFlags(ItemFlag.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @SuppressWarnings("deprecation")
    private void applySkull(Player player, SkullMeta meta, YamlMenuItemDef def, Map<String, String> values) {
        if (def.skullSelf() && player != null) {
            meta.setOwner(player.getName());
            return;
        }
        String owner = YamlPlaceholders.apply(def.skullOwner(), player, values);
        if (owner != null && !owner.trim().isEmpty()) {
            meta.setOwner(owner.trim());
            return;
        }
        String texture = YamlPlaceholders.apply(def.skullTexture(), player, values);
        if (texture == null || texture.trim().isEmpty()) return;
        tryApplyBase64(meta, texture.trim());
    }

    private void tryApplyBase64(SkullMeta meta, String texture) {
        try {
            Object profile = Class.forName("com.mojang.authlib.GameProfile")
                .getConstructor(UUID.class, String.class)
                .newInstance(UUID.randomUUID(), "iori");
            Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
            Object property = Class.forName("com.mojang.authlib.properties.Property")
                .getConstructor(String.class, String.class)
                .newInstance("textures", texture.startsWith("eyJ") ? texture : texture);
            properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);
            Method setter = meta.getClass().getDeclaredMethod("setProfile", profile.getClass());
            setter.setAccessible(true);
            setter.invoke(meta, profile);
        } catch (ReflectiveOperationException ignored) {}
    }

    private void applyCustomModelData(ItemMeta meta, int cmd) {
        if (cmd < 0 || SET_CUSTOM_MODEL_DATA == null) return;
        try {
            SET_CUSTOM_MODEL_DATA.invoke(meta, cmd);
        } catch (ReflectiveOperationException ignored) {}
    }

    private static Method resolveCmdSetter() {
        try {
            return ItemMeta.class.getMethod("setCustomModelData", Integer.class);
        } catch (NoSuchMethodException ignored) {
            try {
                return ItemMeta.class.getMethod("setCustomModelData", int.class);
            } catch (NoSuchMethodException missing) {
                return null;
            }
        }
    }
}
