package us.iorilabs.bukkit.menu.api.yaml.internal;

import java.util.Map;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;
import us.iorilabs.bukkit.modules.gender.GenderKind;
import us.iorilabs.bukkit.modules.gender.GenderModule;
import us.iorilabs.bukkit.modules.gender.GenderService;

public final class GenderMenuInternals {

    private GenderMenuInternals() {}

    public static void register(IoriCorePlugin plugin, YamlMenuService menus) {
        menus.internals().registerEnricher("gender", (player, menuId, ctx) -> {
            GenderService service = service(plugin);
            if (service == null || player == null) return;
            ctx.put("current", service.display(player));
        });

        menus.internals().register("gender.set", (player, argument, ctx, service) -> {
            GenderService gender = service(plugin);
            if (gender == null || player == null) return;
            GenderKind kind = GenderKind.parse(argument);
            if (kind == GenderKind.UNKNOWN) return;
            if (!gender.set(player, kind)) {
                player.sendMessage(plugin.language().rawFor(player, "GENDER.FAILED"));
                return;
            }
            plugin.sounds().play(player, "SUCCESS");
            player.sendMessage(
                plugin.language().rawFor(player, "GENDER.SET", Map.of("gender", gender.display(player)))
            );
            player.closeInventory();
        });

        menus.internals().register("gender.clear", (player, argument, ctx, service) -> {
            GenderService gender = service(plugin);
            if (gender == null || player == null) return;
            if (!gender.clear(player)) {
                player.sendMessage(plugin.language().rawFor(player, "GENDER.FAILED"));
                return;
            }
            plugin.sounds().play(player, "SUCCESS");
            player.sendMessage(plugin.language().rawFor(player, "GENDER.CLEARED"));
            player.closeInventory();
        });
    }

    private static GenderService service(IoriCorePlugin plugin) {
        if (plugin.modules() == null) return null;
        GenderModule module = plugin.modules().get(ModuleId.GENDER);
        return module == null ? null : module.service();
    }
}
