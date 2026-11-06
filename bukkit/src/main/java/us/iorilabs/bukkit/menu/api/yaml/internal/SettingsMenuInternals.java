package us.iorilabs.bukkit.menu.api.yaml.internal;

import java.util.Locale;
import java.util.Map;
import us.iorilabs.api.hook.NLoginHook;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.menu.anvil.AnvilInput;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;
import us.iorilabs.bukkit.modules.settings.PlayerSettings;
import us.iorilabs.bukkit.modules.settings.PlayerSettingsModule;
import us.iorilabs.bukkit.modules.settings.PlayerSettingsStore;

public final class SettingsMenuInternals {

    private SettingsMenuInternals() {}

    public static void register(IoriCorePlugin plugin, YamlMenuService menus) {
        menus.internals().registerEnricher("settings", (player, menuId, ctx) -> {
            PlayerSettingsStore store = store(plugin);
            if (store == null) return;
            PlayerSettings settings = store.get(player.getUniqueId());
            CoreLanguage language = plugin.language();
            ctx.put("lang", settings.language() == null ? language.language() : settings.language());
            ctx.put("value-sounds", language.bool(player, settings.sounds()));
            ctx.put("value-particles", language.bool(player, settings.particles()));
            ctx.put("value-premium", language.bool(player, settings.premiumDisplay()));
            ctx.put("value", language.bool(player, settings.sounds()));
            if (menuId.endsWith("particles")) ctx.put("value", language.bool(player, settings.particles()));
            if (menuId.endsWith("sounds")) ctx.put("value", language.bool(player, settings.sounds()));
        });

        menus.internals().register("settings.toggle-sounds", (player, argument, ctx, service) -> {
            PlayerSettingsStore store = store(plugin);
            if (store == null) return;
            PlayerSettings data = store.get(player.getUniqueId());
            data.sounds(!data.sounds());
            store.save(data);
            player.sendMessage(
                plugin
                    .language()
                    .rawFor(
                        player,
                        "SETTINGS.TOGGLED-SOUNDS",
                        Map.of("value", plugin.language().bool(player, data.sounds()))
                    )
            );
            service.refresh(player);
        });

        menus.internals().register("settings.toggle-particles", (player, argument, ctx, service) -> {
            PlayerSettingsStore store = store(plugin);
            if (store == null) return;
            PlayerSettings data = store.get(player.getUniqueId());
            data.particles(!data.particles());
            store.save(data);
            player.sendMessage(
                plugin
                    .language()
                    .rawFor(
                        player,
                        "SETTINGS.TOGGLED-PARTICLES",
                        Map.of("value", plugin.language().bool(player, data.particles()))
                    )
            );
            service.refresh(player);
        });

        menus.internals().register("settings.toggle-premium", (player, argument, ctx, service) -> {
            PlayerSettingsStore store = store(plugin);
            if (store == null) return;
            PlayerSettings data = store.get(player.getUniqueId());
            data.premiumDisplay(!data.premiumDisplay());
            store.save(data);
            player.sendMessage(
                plugin
                    .language()
                    .rawFor(
                        player,
                        "SETTINGS.TOGGLED-PREMIUM",
                        Map.of("value", plugin.language().bool(player, data.premiumDisplay()))
                    )
            );
            service.refresh(player);
        });

        menus.internals().register("settings.set-lang", (player, argument, ctx, service) -> {
            PlayerSettingsStore store = store(plugin);
            if (store == null) return;
            String lang = argument == null ? "" : argument.trim().toLowerCase(Locale.ROOT);
            if (!"en".equals(lang) && !"es".equals(lang)) return;
            PlayerSettings settings = store.get(player.getUniqueId());
            settings.language(lang);
            store.save(settings);
            player.sendMessage(plugin.language().rawFor(player, "SETTINGS.TOGGLED-LANG", Map.of("lang", lang)));
            service.refresh(player);
        });

        menus.internals().register("settings.password", (player, argument, ctx, service) -> {
            NLoginHook nLogin = plugin.nLoginHook();
            CoreLanguage language = plugin.language();
            if (nLogin == null || !nLogin.available()) {
                player.sendMessage(language.rawFor(player, "SETTINGS.PASSWORD-UNAVAILABLE"));
                return;
            }
            player.closeInventory();
            boolean opened = AnvilInput.open(
                plugin,
                player,
                language.plain((org.bukkit.command.CommandSender) player, "SETTINGS.PASSWORD-ANVIL-TITLE"),
                "",
                input -> {
                    if (input == null || input.trim().length() < 4) {
                        player.sendMessage(language.rawFor(player, "SETTINGS.PASSWORD-INVALID"));
                        return;
                    }
                    boolean ok = nLogin.changePassword(player.getUniqueId(), player.getName(), input.trim());
                    player.sendMessage(
                        language.rawFor(player, ok ? "SETTINGS.PASSWORD-CHANGED" : "SETTINGS.PASSWORD-FAILED")
                    );
                },
                () -> player.sendMessage(language.rawFor(player, "SETTINGS.PASSWORD-CANCELLED"))
            );
            if (!opened) player.sendMessage(language.rawFor(player, "SETTINGS.PASSWORD-UNAVAILABLE"));
        });
    }

    private static PlayerSettingsStore store(IoriCorePlugin plugin) {
        if (plugin.modules() == null) return null;
        PlayerSettingsModule module = plugin.modules().get(ModuleId.PLAYER_SETTINGS);
        return module == null ? null : module.store();
    }
}
