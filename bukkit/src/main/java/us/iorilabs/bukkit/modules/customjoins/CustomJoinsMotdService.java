package us.iorilabs.bukkit.modules.customjoins;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.core.vanish.VanishVisibility;
import us.iorilabs.bukkit.util.LegacyMaterials;
import us.iorilabs.bukkit.util.PictureLoginFace;
import us.iorilabs.proxy.utilities.ProtocolVersions;

@RequiredArgsConstructor
public final class CustomJoinsMotdService {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;

    public void send(Player player) {
        if (player == null || !player.isOnline()) return;

        int protocol = language.protocolOf(player);
        boolean legacy = ProtocolVersions.isLegacy(protocol);
        Map<String, String> placeholders = basePlaceholders(player);

        if (legacy || !plugin.settings().getBoolean("CUSTOM-JOINS.PICTURE-LOGIN", true)) {
            String listKey = legacy ? "JOIN-MOTD.LEGACY" : "JOIN-MOTD.MODERN";
            for (String line : language.resolveList(language.languageOf(player), listKey, placeholders, protocol)) {
                player.sendMessage(line);
            }
            maybeGiveHead(player, placeholders, null);
            return;
        }

        String template = plugin.settings().getString("CUSTOM-JOINS.SKIN-URL", "https://crafthead.net/helm/%player%/8");
        String url = template
            .replace("%player%", player.getName())
            .replace("%uuid%", player.getUniqueId().toString())
            .replace("%pname%", player.getName());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            BufferedImage image = null;
            try {
                image = PictureLoginFace.fetch(url);
            } catch (Exception ignored) {}
            BufferedImage face = image;
            Bukkit.getScheduler().runTask(plugin, () -> deliverModern(player, face, url, placeholders, protocol));
        });
    }

    private void deliverModern(
        Player player,
        BufferedImage image,
        String url,
        Map<String, String> placeholders,
        int protocol
    ) {
        if (!player.isOnline()) return;

        placeholders.put("url", url);
        List<String> side = language.resolveList(
            language.languageOf(player),
            "JOIN-MOTD.PICTURE",
            placeholders,
            protocol
        );
        if (side.isEmpty()) {
            side = language.resolveList(language.languageOf(player), "JOIN-MOTD.MODERN", placeholders, protocol);
        }

        if (image != null) {
            boolean hex = protocol >= 735;
            for (String line : PictureLoginFace.compose(image, side, hex)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            }
        } else {
            for (String line : language.resolveList(
                language.languageOf(player),
                "JOIN-MOTD.MODERN",
                placeholders,
                protocol
            )) {
                player.sendMessage(line);
            }
        }
        maybeGiveHead(player, placeholders, url);
    }

    @SuppressWarnings("deprecation")
    private void maybeGiveHead(Player player, Map<String, String> placeholders, String url) {
        if (!plugin.settings().getBoolean("CUSTOM-JOINS.GIVE-HEAD", false)) return;
        if (url != null) placeholders.put("url", url);

        ItemStack head = new ItemStack(LegacyMaterials.playerHead(), 1, (short) 3);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwner(player.getName());
        }
        if (meta != null) {
            meta.setDisplayName(language.raw(player, "JOIN-MOTD.HEAD-NAME", placeholders));
            meta.setLore(language.list(player, "JOIN-MOTD.HEAD-LORE", placeholders));
            head.setItemMeta(meta);
        }
        player.getInventory().addItem(head);
    }

    private Map<String, String> basePlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("online", String.valueOf(VanishVisibility.visibleOnlineCount(plugin, player)));
        placeholders.put("server", plugin.settings().getString("SETTINGS.SERVER-NAME", "lobby"));
        placeholders.put("brand", "RoyalCTW");
        boolean bedrock = plugin.geyserHook() != null && plugin.geyserHook().isBedrock(player.getUniqueId());
        placeholders.put("bedrock", language.bool(player, bedrock));
        return placeholders;
    }
}
