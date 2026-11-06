package us.iorilabs.bukkit.modules.staffmode;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import us.iorilabs.api.hook.NoopApolloHook;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.staffmode.command.StaffChatCommand;
import us.iorilabs.bukkit.modules.staffmode.command.StaffCommand;
import us.iorilabs.bukkit.modules.staffmode.command.StaffModeCommand;
import us.iorilabs.bukkit.modules.staffmode.freeze.FreezeListener;

@Getter
@Accessors(fluent = true)
public final class StaffModeModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private StaffModeConfig config;
    private StaffModeService service;
    private StaffChatService staffChat;
    private StaffModeListener listener;
    private StaffChatListener staffChatListener;
    private FreezeListener freezeListener;
    private BukkitTask freezeTask;

    public StaffModeModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.STAFF-MODE", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.STAFF_MODE;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        config = StaffModeConfig.load(plugin);
        service = new StaffModeService(plugin, plugin.language(), plugin.yamlMenus(), config);
        service.apollo(plugin.apolloHook());
        staffChat = new StaffChatService(plugin, config);

        BukkitCommandBridge.register(plugin, new StaffModeCommand(plugin.language(), service, staffChat));
        BukkitCommandBridge.register(plugin, new StaffCommand(plugin, plugin.language(), service, staffChat));
        BukkitCommandBridge.register(plugin, new StaffChatCommand(plugin.language(), staffChat));

        listener = new StaffModeListener(service, staffChat, plugin.language());
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        staffChatListener = new StaffChatListener(staffChat);
        Bukkit.getPluginManager().registerEvents(staffChatListener, plugin);
        Bukkit.getPluginManager().registerEvents(service.invsee(), plugin);
        freezeListener = new FreezeListener(service.freeze(), plugin.language());
        Bukkit.getPluginManager().registerEvents(freezeListener, plugin);

        long interval = Math.max(5L, config.getLong("FREEZE-ACTIONBAR-TICKS", 20L));
        freezeTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            () -> service.freeze().tickActionBars(),
            interval,
            interval
        );
    }

    @Override
    public void disable() {
        if (freezeTask != null) {
            freezeTask.cancel();
            freezeTask = null;
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (staffChatListener != null) {
            HandlerList.unregisterAll(staffChatListener);
            staffChatListener = null;
        }
        staffChat = null;
        config = null;
        if (freezeListener != null) {
            HandlerList.unregisterAll(freezeListener);
            freezeListener = null;
        }
        if (service != null) {
            HandlerList.unregisterAll(service.invsee());
            service.shutdown();
            service.apollo(new NoopApolloHook());
            service = null;
        }
    }
}
