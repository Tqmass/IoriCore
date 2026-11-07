package us.iorilabs.bukkit.modules.lobby;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.menu.api.Button;
import us.iorilabs.bukkit.menu.api.Menu;
import us.iorilabs.bukkit.menu.enums.MenuVariant;
import us.iorilabs.bukkit.util.ColorUtil;
import us.iorilabs.bukkit.util.LegacyMaterials;
import us.iorilabs.bukkit.util.ProtocolProbe;
import us.iorilabs.proxy.utilities.ProtocolVersions;

public final class LobbyMenus {

    private final IoriCorePlugin plugin;
    private final LobbyGlowService glow;

    public LobbyMenus(IoriCorePlugin plugin, LobbyGlowService glow) {
        this.plugin = plugin;
        this.glow = glow;
    }

    public void openGlow(Player player) {
        new GlowMenu(load(player, "glow")).openMenu(player);
    }

    private YamlConfiguration load(Player player, String id) {
        MenuVariant variant = ProtocolVersions.isLegacy(ProtocolProbe.protocol(player, plugin.fallbackProtocol()))
            ? MenuVariant.LEGACY
            : MenuVariant.MODERN;
        String path = "menus/" + variant.folder() + "/" + id + ".yml";
        File file = new File(plugin.getDataFolder(), path);
        us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, path);
        return YamlConfiguration.loadConfiguration(file);
    }

    private final class GlowMenu extends Menu {

        private final YamlConfiguration yaml;

        private GlowMenu(YamlConfiguration yaml) {
            this.yaml = yaml;
            updateAfterClick(true);
        }

        @Override
        public String getTitle(Player player) {
            return text(yaml.getString("TITLE", "&8Glow"), player);
        }

        @Override
        public int getSize() {
            int size = yaml.getInt("SIZE", 45);
            size = Math.max(9, Math.min(54, size));
            return ((size + 8) / 9) * 9;
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            putFiller(buttons, yaml.getConfigurationSection("FILLER"), player);
            putItems(buttons, yaml.getConfigurationSection("ITEMS"), player);
            ConfigurationSection colors = yaml.getConfigurationSection("COLORS");
            List<Integer> slots = LobbyMenuSlots.from(colors);
            int index = 0;
            for (LobbyGlowColor color : LobbyGlowColor.values()) {
                if (index >= slots.size()) break;
                buttons.put(slots.get(index++), new GlowColorButton(colors, color));
            }
            return buttons;
        }

        private final class GlowColorButton extends Button {

            private final ConfigurationSection section;
            private final LobbyGlowColor color;

            private GlowColorButton(ConfigurationSection section, LobbyGlowColor color) {
                this.section = section;
                this.color = color;
            }

            @Override
            @SuppressWarnings("deprecation")
            public ItemStack getButtonItem(Player player) {
                boolean locked = !glow.hasColor(player, color.key());
                boolean selected = !locked && color.key().equals(glow.selectedKey(player));
                Map<String, String> ph = Map.of("%color%", color.display());
                String name;
                List<String> lore;
                if (locked) {
                    name = section.getString("LOCKED.NAME", section.getString("NAME", "%color%"));
                    lore = section.getStringList("LOCKED.LORE");
                } else if (selected) {
                    name = section.getString("SELECTED.NAME", section.getString("NAME", "%color%"));
                    lore = section.getStringList("SELECTED.LORE");
                } else {
                    name = section.getString("NAME", "%color%");
                    lore = section.getStringList("LORE");
                }
                Material material = LegacyMaterials.of(section.getString("MATERIAL", "WOOL"));
                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(replace(name, ph, player));
                if (lore != null && !lore.isEmpty()) {
                    meta.setLore(
                        lore
                            .stream()
                            .map(line -> replace(line, ph, player))
                            .filter(line -> !ChatColor.stripColor(line).isBlank())
                            .toList()
                    );
                }
                item.setItemMeta(meta);
                item.setDurability(color.woolData());
                return item;
            }

            @Override
            public void clicked(Player player, int slot, ClickType click, int hotbarButton) {
                if (!glow.hasColor(player, color.key())) {
                    String msg = section.getString("LOCKED.MESSAGE", "");
                    if (msg != null && !msg.isBlank()) player.sendMessage(replace(msg, Map.of(), player));
                    return;
                }
                if (plugin.apolloHook() == null || !plugin.apolloHook().available()) {
                    player.sendMessage(
                        ColorUtil.translate(
                            "&cApollo no está disponible.",
                            ProtocolProbe.protocol(player, plugin.fallbackProtocol())
                        )
                    );
                    return;
                }
                glow.setSelected(player, color);
                player.sendMessage(
                    ColorUtil.translate(
                        "&aGlow: " + color.display(),
                        ProtocolProbe.protocol(player, plugin.fallbackProtocol())
                    )
                );
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType click) {
                return true;
            }
        }
    }

    private void putFiller(Map<Integer, Button> buttons, ConfigurationSection filler, Player player) {
        if (filler == null || !filler.getBoolean("ENABLED", true)) return;
        ItemStack stack = stackFrom(filler, player, Map.of());
        for (int slot : LobbyMenuSlots.from(filler)) {
            buttons.put(slot, simple(stack));
        }
    }

    private void putItems(Map<Integer, Button> buttons, ConfigurationSection items, Player player) {
        if (items == null) return;
        for (String key : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(key);
            if (item == null) continue;
            ItemStack stack = stackFrom(item, player, Map.of("%player%", player.getName()));
            String action = item.getString("ACTION", "NONE").toUpperCase(Locale.ROOT);
            String command = item.getString("COMMAND", "").replaceFirst("^/", "");
            for (int slot : LobbyMenuSlots.from(item)) {
                buttons.put(slot, actionButton(stack, action, command));
            }
        }
    }

    private Button actionButton(ItemStack stack, String action, String command) {
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return stack.clone();
            }

            @Override
            public void clicked(Player player, int slot, ClickType click, int hotbarButton) {
                if ("CLOSE".equals(action)) player.closeInventory();
                if ("GLOW-OFF".equals(action)) glow.reset(player);
                if ("COMMAND".equals(action) && !command.isEmpty()) player.performCommand(command);
            }
        };
    }

    private static Button simple(ItemStack stack) {
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return stack.clone();
            }
        };
    }

    @SuppressWarnings("deprecation")
    private ItemStack stackFrom(ConfigurationSection section, Player player, Map<String, String> extra) {
        String materialName = section == null ? "STONE" : section.getString("MATERIAL", "STONE");
        Material material = LegacyMaterials.of(materialName);
        ItemStack item = new ItemStack(material, 1);
        if (section != null && section.contains("DATA")) item.setDurability((short) section.getInt("DATA", 0));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(replace(section == null ? " " : section.getString("NAME", " "), extra, player));
        List<String> lore = section == null ? List.of() : section.getStringList("LORE");
        if (!lore.isEmpty()) meta.setLore(
            lore
                .stream()
                .map(line -> replace(line, extra, player))
                .toList()
        );
        item.setItemMeta(meta);
        return item;
    }

    private String text(String input, Player player) {
        return replace(input, Map.of("%player%", player.getName()), player);
    }

    private String replace(String input, Map<String, String> extra, Player player) {
        if (input == null) return "";
        String out = input.replace("{PLAYER}", player.getName()).replace("%player%", player.getName());
        for (Map.Entry<String, String> entry : extra.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        int protocol = ProtocolProbe.protocol(player, plugin.fallbackProtocol());
        return ColorUtil.translate(out, protocol);
    }
}
