package us.iorilabs.bukkit.modules.chatformat;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;

public final class ChatFormatModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private ChatFormatService service;
    private ChatFormatListener listener;

    public ChatFormatModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.CHAT-FORMAT", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.CHAT_FORMAT;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        service = new ChatFormatService(plugin, plugin.language(), plugin.vaultHook(), plugin.luckPermsHook());
        listener = new ChatFormatListener(service);
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

    public ChatFormatService service() {
        return service;
    }
}
