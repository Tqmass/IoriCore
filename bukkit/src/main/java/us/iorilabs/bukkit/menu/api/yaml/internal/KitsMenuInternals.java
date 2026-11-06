package us.iorilabs.bukkit.menu.api.yaml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;
import us.iorilabs.bukkit.modules.core.CoreFeaturesModule;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.bukkit.modules.core.kit.KitDefinition;
import us.iorilabs.bukkit.modules.core.kit.KitService;

public final class KitsMenuInternals {

    private KitsMenuInternals() {}

    public static void register(IoriCorePlugin plugin, YamlMenuService menus) {
        menus.dynamics().register("kits.enabled", (player, ctx) -> {
            KitService kits = kits(plugin);
            if (kits == null) return List.of();
            List<Map<String, String>> out = new ArrayList<>();
            for (String name : kits.names()) {
                KitDefinition kit = kits.get(name);
                if (kit == null) continue;
                out.add(placeholders(plugin, kits, player, kit));
            }
            return out;
        });

        menus.internals().register("kits.give", (player, argument, ctx, service) -> {
            KitService kits = kits(plugin);
            if (kits == null) {
                player.sendMessage(plugin.language().rawFor(player, "ESSENTIALS.KIT.FAILED", Map.of("kit", "")));
                return;
            }
            String name =
                argument == null || argument.isEmpty()
                    ? ctx == null
                        ? ""
                        : ctx.getOrDefault("kit", ctx.getOrDefault("id", ""))
                    : argument.trim();
            KitDefinition kit = kits.get(name);
            if (kit == null) {
                player.sendMessage(plugin.language().rawFor(player, "ESSENTIALS.KIT.NOT-FOUND", Map.of("kit", name)));
                return;
            }
            if (
                !player.hasPermission(kit.permission()) &&
                !player.hasPermission("ioricore.kit.*") &&
                !player.hasPermission("ioricore.admin")
            ) {
                player.sendMessage(plugin.language().rawFor(player, "GENERAL.NO-PERMISSION"));
                return;
            }
            long remaining = kits.remainingCooldown(player.getUniqueId(), kit);
            if (remaining > 0L) {
                player.sendMessage(
                    plugin
                        .language()
                        .rawFor(
                            player,
                            "ESSENTIALS.KIT.COOLDOWN",
                            Map.of("kit", kit.name(), "time", formatSeconds(remaining))
                        )
                );
                return;
            }
            if (!kits.give(player, kit)) {
                player.sendMessage(
                    plugin.language().rawFor(player, "ESSENTIALS.KIT.FAILED", Map.of("kit", kit.name()))
                );
                return;
            }
            EssentialsSounds.play(plugin, player, "SUCCESS");
            player.sendMessage(plugin.language().rawFor(player, "ESSENTIALS.KIT.SUCCESS", Map.of("kit", kit.name())));
            player.closeInventory();
        });
    }

    private static Map<String, String> placeholders(
        IoriCorePlugin plugin,
        KitService kits,
        org.bukkit.entity.Player player,
        KitDefinition kit
    ) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", kit.name());
        placeholders.put("kit", kit.name());
        placeholders.put("name", kit.name());
        placeholders.put("permission", kit.permission());
        long remaining = kits.remainingCooldown(player.getUniqueId(), kit);
        placeholders.put("cooldown", formatSeconds(remaining));
        placeholders.put("ready", remaining <= 0L ? "true" : "false");
        boolean allowed =
            player.hasPermission(kit.permission()) ||
            player.hasPermission("ioricore.kit.*") ||
            player.hasPermission("ioricore.admin");
        placeholders.put("allowed", allowed ? "true" : "false");
        return placeholders;
    }

    private static String formatSeconds(long millis) {
        if (millis <= 0L) return "0s";
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        if (seconds < 60L) return seconds + "s";
        long minutes = seconds / 60L;
        if (minutes < 60L) return minutes + "m";
        return minutes / 60L + "h";
    }

    private static KitService kits(IoriCorePlugin plugin) {
        if (plugin.modules() == null) return null;
        CoreFeaturesModule module = plugin.modules().get(ModuleId.CORE);
        return module == null ? null : module.kits();
    }
}
