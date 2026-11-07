package us.iorilabs.bukkit.modules.scoreboard;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.scoreboard.command.ScoreboardCommand;

@Getter
@Accessors(fluent = true)
public final class ScoreboardModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private ScoreboardService service;
    private ScoreboardListener listener;

    public ScoreboardModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.SCOREBOARD", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.SCOREBOARD;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        service = new ScoreboardService(plugin);
        BukkitCommandBridge.register(plugin, new ScoreboardCommand(plugin, service));
        listener = new ScoreboardListener(service);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        service.start();
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

    @Override
    public void reload() {
        if (service != null) service.reload();
    }
}
