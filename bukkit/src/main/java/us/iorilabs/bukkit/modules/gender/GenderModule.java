package us.iorilabs.bukkit.modules.gender;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.logging.BukkitLogger;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.gender.command.GenderCommand;

@Getter
@Accessors(fluent = true)
public final class GenderModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private GenderService service;
    private GenderListener listener;

    public GenderModule(IoriCorePlugin plugin, FileConfiguration settings, BukkitLogger logger) {
        this.plugin = plugin;
        this.enabledFlag = settings.getBoolean("MODULES.GENDER", false);
    }

    @Override
    public ModuleId id() {
        return ModuleId.GENDER;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        service = new GenderService(plugin);
        service.load();
        BukkitCommandBridge.register(plugin, new GenderCommand(plugin, service));
        listener = new GenderListener(service);
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
