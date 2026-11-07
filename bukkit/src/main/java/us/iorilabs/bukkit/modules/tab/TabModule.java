package us.iorilabs.bukkit.modules.tab;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.tab.command.TabCommand;

@Getter
@Accessors(fluent = true)
public final class TabModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private TabService service;
    private TabListener listener;

    public TabModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.TAB", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.TAB;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        service = new TabService(plugin);
        listener = new TabListener(service);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        BukkitCommandBridge.register(plugin, new TabCommand(plugin, service));
        service.start();
    }

    @Override
    public void reload() {
        if (service != null) service.reload();
    }

    @Override
    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (service != null) {
            service.stop();
            service = null;
        }
    }
}
