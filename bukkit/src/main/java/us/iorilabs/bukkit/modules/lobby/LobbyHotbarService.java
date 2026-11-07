package us.iorilabs.bukkit.modules.lobby;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.util.ColorUtil;
import us.iorilabs.bukkit.util.DisplayYaml;
import us.iorilabs.bukkit.util.LegacyMaterials;
import us.iorilabs.bukkit.util.ProtocolProbe;
import us.iorilabs.proxy.utilities.ProtocolVersions;

public final class LobbyHotbarService {

    private final IoriCorePlugin plugin;
    private FileConfiguration config;

    public LobbyHotbarService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        config = DisplayYaml.load(plugin, "hotbar.yml");
    }

    public List<String> keys() {
        return config.getKeys(false).stream().sorted().toList();
    }

    public void give(Player player) {
        if (player == null) return;
        clearSlots(player);
        for (String key : keys()) {
            if (!config.getBoolean(key + ".ENABLED", true)) continue;
            ItemStack item = build(key, player);
            if (item == null) continue;
            int slot = Math.max(0, Math.min(8, config.getInt(key + ".SLOT", 0)));
            player.getInventory().setItem(slot, item);
        }
    }

    public void clearSlots(Player player) {
        for (String key : keys()) {
            if (!config.getBoolean(key + ".ENABLED", true)) continue;
            int slot = Math.max(0, Math.min(8, config.getInt(key + ".SLOT", 0)));
            player.getInventory().setItem(slot, null);
        }
    }

    public boolean matches(Player player, ItemStack stack, String key) {
        if (player == null || stack == null || key == null) return false;
        int slot = Math.max(0, Math.min(8, config.getInt(key + ".SLOT", -1)));
        ItemStack expected = player.getInventory().getItem(slot);
        if (expected == null || expected.getType() == Material.AIR) return false;
        if (expected.getType() != stack.getType()) return false;
        if (!expected.hasItemMeta() || !stack.hasItemMeta()) return expected.getType() == stack.getType();
        String a = expected.getItemMeta().getDisplayName();
        String b = stack.getItemMeta().getDisplayName();
        return a != null && a.equals(b);
    }

    public String commandFor(ItemStack stack, Player player) {
        if (stack == null) return "";
        for (String key : keys()) {
            if (!matches(player, stack, key)) continue;
            return config.getString(key + ".COMMAND", "").replaceFirst("^/", "");
        }
        return "";
    }

    @SuppressWarnings("deprecation")
    private ItemStack build(String key, Player player) {
        String materialName = config.getString(key + ".MATERIAL", "STONE");
        Material material = LegacyMaterials.of(materialName);
        ItemStack item = new ItemStack(material, Math.max(1, config.getInt(key + ".AMOUNT", 1)));
        short data = (short) config.getInt(key + ".DATA", 0);
        if (data > 0) item.setDurability(data);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = apply(config.getString(key + ".NAME", ""), player);
        int protocol = ProtocolProbe.protocol(player, plugin.fallbackProtocol());
        meta.setDisplayName(ColorUtil.translate(name, protocol));
        List<String> lore = config.getStringList(key + ".LORE");
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(
                lore
                    .stream()
                    .map(line -> ColorUtil.translate(apply(line, player), protocol))
                    .toList()
            );
        }
        item.setItemMeta(meta);

        if (meta instanceof SkullMeta skull) {
            String owner = apply(config.getString(key + ".SKULL-OWNER", ""), player);
            if (owner != null && !owner.isEmpty()) skull.setOwner(owner);
            item.setItemMeta(skull);
        }
        if (meta instanceof LeatherArmorMeta leather && config.contains(key + ".COLOR")) {
            String raw = String.valueOf(config.get(key + ".COLOR"));
            java.awt.Color color = parseColor(raw);
            if (color != null) {
                leather.setColor(org.bukkit.Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue()));
                item.setItemMeta(leather);
            }
        }
        return item;
    }

    private static String apply(String input, Player player) {
        if (input == null) return "";
        return input.replace("{PLAYER}", player.getName()).replace("%player%", player.getName());
    }

    private static java.awt.Color parseColor(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String value = raw.trim();
        if (value.startsWith("#")) {
            try {
                return java.awt.Color.decode(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return LobbyGlowColor.parseNamed(value);
    }
}
