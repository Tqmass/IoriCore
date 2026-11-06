package us.iorilabs.bukkit.modules.customjoins;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;

@Getter
@Accessors(fluent = true)
public final class CustomJoinsModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private CustomJoinsConfig config;
    private CustomJoinsService service;
    private CustomJoinsListener listener;

    public CustomJoinsModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.CUSTOM-JOINS", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.CUSTOM_JOINS;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        config = new CustomJoinsConfig(plugin);
        config.load();
        service = new CustomJoinsService(
            plugin,
            config,
            new CustomJoinsMotdService(plugin, plugin.language()),
            plugin.sounds()
        );
        listener = new CustomJoinsListener(plugin, service);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        service = null;
        config = null;
    }

    @Override
    public void reload() {
        disable();
        enable();
    }
}
