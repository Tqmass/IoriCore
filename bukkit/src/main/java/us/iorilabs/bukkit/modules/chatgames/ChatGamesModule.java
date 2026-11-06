package us.iorilabs.bukkit.modules.chatgames;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.chatgames.command.ChatGamesCommand;

@Getter
@Accessors(fluent = true)
public final class ChatGamesModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private ChatGamesService service;
    private ChatGamesListener listener;

    public ChatGamesModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.CHAT-GAMES", false);
    }

    @Override
    public ModuleId id() {
        return ModuleId.CHAT_GAMES;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "data/chatgames.yml");
        service = new ChatGamesService(plugin);
        service.load();
        service.startScheduler();

        listener = new ChatGamesListener(plugin, service);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        BukkitCommandBridge.register(plugin, new ChatGamesCommand(plugin, service));
    }

    @Override
    public void disable() {
        if (service != null) {
            service.stopScheduler();
            service = null;
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
    }
}
