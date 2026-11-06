package us.iorilabs.bukkit.menu.api.yaml.internal;

import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;

public final class MenuYamlBootstrap {

    private MenuYamlBootstrap() {}

    public static void register(IoriCorePlugin plugin, YamlMenuService menus) {
        if (plugin == null || menus == null) return;
        SettingsMenuInternals.register(plugin, menus);
        RanksMenuInternals.register(plugin, menus);
        CoinshopMenuInternals.register(plugin, menus);
        KitsMenuInternals.register(plugin, menus);
        StaffMenuInternals.register(plugin, menus);
        PunishMenuInternals.register(plugin, menus);
        TagsMenuInternals.register(plugin, menus);
        GenderMenuInternals.register(plugin, menus);
    }
}
