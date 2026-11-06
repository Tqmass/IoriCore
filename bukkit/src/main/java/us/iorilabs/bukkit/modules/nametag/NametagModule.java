package us.iorilabs.bukkit.modules.nametag;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;

@Getter
@Accessors(fluent = true)
public final class NametagModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private NametagService service;
    private NametagListener listener;
    private BukkitTask task;

    public NametagModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.NAMETAG", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.NAMETAG;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        service = new NametagService(plugin);
        listener = new NametagListener(service);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, service::refreshAll, 40L, 100L);
    }

    @Override
    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        service = null;
    }
}
