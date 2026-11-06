package us.iorilabs.bukkit.menu.api.yaml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;
import us.iorilabs.bukkit.modules.coinshop.CoinshopModule;
import us.iorilabs.bukkit.modules.coinshop.CoinshopService;
import us.iorilabs.bukkit.modules.tags.Tag;
import us.iorilabs.bukkit.modules.tags.TagService;
import us.iorilabs.bukkit.modules.tags.TagsModule;

public final class TagsMenuInternals {

    private TagsMenuInternals() {}

    public static void register(IoriCorePlugin plugin, YamlMenuService menus) {
        menus.dynamics().register("tags.all", (player, ctx) -> rows(plugin, player, tags(plugin), false));
        menus.dynamics().register("tags.owned", (player, ctx) -> rows(plugin, player, tags(plugin), true));

        menus.internals().register("tags.equip", (player, argument, ctx, service) -> {
            TagService tagService = tags(plugin);
            if (tagService == null || player == null) return;
            String id =
                argument == null || argument.trim().isEmpty()
                    ? ctx == null
                        ? ""
                        : ctx.getOrDefault("id", "")
                    : argument.trim();
            Tag tag = tagService.get(id);
            if (tag == null) {
                player.sendMessage(plugin.language().rawFor(player, "TAGS.UNKNOWN", Map.of("id", id)));
                return;
            }
            if (!tagService.canUse(player, tag)) {
                player.sendMessage(plugin.language().rawFor(player, "GENERAL.NO-PERMISSION"));
                return;
            }
            if (!tagService.equip(player, tag.id())) {
                player.sendMessage(plugin.language().rawFor(player, "TAGS.EQUIP-FAILED"));
                return;
            }
            plugin.sounds().play(player, "SUCCESS");
            player.sendMessage(
                plugin.language().rawFor(player, "TAGS.EQUIPPED-SET", Map.of("id", tag.id(), "display", tag.display()))
            );
            service.refresh(player);
        });
    }

    private static List<Map<String, String>> rows(
        IoriCorePlugin plugin,
        org.bukkit.entity.Player player,
        TagService tagService,
        boolean ownedOnly
    ) {
        if (tagService == null || player == null) return List.of();
        Tag equipped = tagService.equipped(player.getUniqueId());
        List<Map<String, String>> out = new ArrayList<>();
        for (Tag tag : ownedOnly ? tagService.all() : tagService.visibleFor(player)) {
            if (!tagService.canUse(player, tag)) continue;
            if (ownedOnly && !ownsTag(plugin, player.getUniqueId(), tag.id())) continue;
            out.add(placeholders(plugin, player, tag, equipped));
        }
        return out;
    }

    private static Map<String, String> placeholders(
        IoriCorePlugin plugin,
        org.bukkit.entity.Player player,
        Tag tag,
        Tag equipped
    ) {
        boolean active = equipped != null && equipped.id().equals(tag.id());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", tag.id());
        placeholders.put("display", tag.display());
        placeholders.put("preview", tag.fragment());
        placeholders.put("glow", active ? "true" : "false");
        placeholders.put(
            "status",
            plugin.language().rawFor(player, active ? "TAGS.MENU.STATUS-EQUIPPED" : "TAGS.MENU.STATUS-CLICK")
        );
        return placeholders;
    }

    private static TagService tags(IoriCorePlugin plugin) {
        if (plugin.modules() == null) return null;
        TagsModule module = plugin.modules().get(ModuleId.TAGS);
        return module == null ? null : module.service();
    }

    private static boolean ownsTag(IoriCorePlugin plugin, UUID uuid, String tagId) {
        if (plugin.modules() == null || uuid == null || tagId == null) return false;
        CoinshopModule module = plugin.modules().get(ModuleId.COINSHOP);
        if (module == null || !module.enabled()) return false;
        CoinshopService shop = module.service();
        return shop != null && shop.ownsTag(uuid, tagId);
    }
}
