package us.iorilabs.bukkit.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.iorilabs.bukkit.util.LegacyMaterials;

@Getter
@Accessors(fluent = true)
public final class MenuFiller {

    private final Material material;
    private final short data;
    private final String name;
    private final boolean glow;
    private final List<Integer> slots;

    public MenuFiller(Material material, String name, boolean glow, List<Integer> slots) {
        this(material, (short) 0, name, glow, slots);
    }

    public MenuFiller(Material material, short data, String name, boolean glow, List<Integer> slots) {
        this.material = material == null ? LegacyMaterials.grayPane() : material;
        this.data = data;
        this.name = name == null ? " " : name;
        this.glow = glow;
        this.slots =
            slots == null
                ? Collections.<Integer>emptyList()
                : Collections.unmodifiableList(new ArrayList<Integer>(slots));
    }

    public static MenuFiller from(ConfigurationSection section, MenuFiller fallback) {
        if (section == null) return fallback;

        Material material = parseMaterial(section.getString("MATERIAL"), fallback.material());
        short data = (short) section.getInt("DATA", fallback.data());
        String name = section.getString("NAME", fallback.name());
        boolean glow = section.getBoolean("GLOW", fallback.glow());
        List<Integer> slots = section.getIntegerList("SLOTS");
        if (slots == null || slots.isEmpty()) slots = fallback.slots();

        return new MenuFiller(material, data, name, glow, slots);
    }

    @SuppressWarnings("deprecation")
    public ItemStack build() {
        ItemStack stack = new ItemStack(material, 1, data);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MenuColors.legacy(name));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            if (glow) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static Material parseMaterial(String raw, Material fallback) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        Material resolved = LegacyMaterials.of(raw.trim().toUpperCase());
        if (resolved == Material.STONE && fallback != null) return fallback;
        return resolved;
    }
}
