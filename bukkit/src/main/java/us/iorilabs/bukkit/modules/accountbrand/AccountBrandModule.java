package us.iorilabs.bukkit.modules.accountbrand;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;

@Getter
@Accessors(fluent = true)
public final class AccountBrandModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private AccountBrandService service;
    private AccountBrandListener listener;

    public AccountBrandModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        boolean moduleOn = plugin.settings().getBoolean("MODULES.ACCOUNT-BRAND", true);
        boolean settingsOn = plugin.settings().getBoolean("ACCOUNT-BRAND.ENABLED", true);
        this.enabledFlag = moduleOn && settingsOn;
    }

    @Override
    public ModuleId id() {
        return ModuleId.ACCOUNT_BRAND;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        boolean geyser = plugin.settings().getBoolean("ACCOUNT-BRAND.HOOKS.GEYSER", true);
        boolean nLogin = plugin.settings().getBoolean("ACCOUNT-BRAND.HOOKS.NLOGIN", true);
        service = new AccountBrandService(plugin, geyser, nLogin);
        listener = new AccountBrandListener(plugin, service);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        service = null;
    }
}
