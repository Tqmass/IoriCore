package us.iorilabs.bukkit.modules.chatlogs;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;

public final class ChatLogsModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final FileConfiguration settings;
    private final boolean enabledFlag;
    private ChatLogService service;
    private ChatLogListener listener;

    public ChatLogsModule(IoriCorePlugin plugin, FileConfiguration settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.enabledFlag = settings.getBoolean("MODULES.CHAT-LOGS", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.CHAT_LOGS;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        service = new ChatLogService(plugin);
        service.load(settings);
        listener = new ChatLogListener(service);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (service != null) {
            service.shutdown();
            service = null;
        }
    }

    public ChatLogService service() {
        return service;
    }
}
