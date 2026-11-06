package us.iorilabs.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemBuilder {

    private final ItemStack stack;
    private final List<String> lore = new ArrayList<>();
    private String name;
    private boolean glow;
    private boolean hideFlags = true;

    public ItemBuilder(Material material) {
        this(new ItemStack(material == null ? Material.STONE : material));
    }

    public ItemBuilder(ItemStack base) {
        this.stack = base.clone();
    }

    public ItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        lore.clear();
        if (lines != null) lore.addAll(lines);
        return this;
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(Math.max(1, amount));
        return this;
    }

    @SuppressWarnings("deprecation")
    public ItemBuilder data(int data) {
        stack.setDurability((short) data);
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public ItemBuilder hideFlags(boolean hideFlags) {
        this.hideFlags = hideFlags;
        return this;
    }

    public ItemStack build() {
        ItemStack result = stack.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return result;

        if (name != null) meta.setDisplayName(color(name));
        if (!lore.isEmpty()) {
            List<String> colored = new ArrayList<>(lore.size());
            for (String line : lore) colored.add(color(line));
            meta.setLore(colored);
        }
        if (hideFlags) meta.addItemFlags(ItemFlag.values());
        if (glow) meta.addEnchant(Enchantment.DURABILITY, 1, true);

        result.setItemMeta(meta);
        return result;
    }

    private static String color(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }
}
