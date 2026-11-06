package us.iorilabs.bukkit.modules.coinshop;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.coinshop.command.CoinshopCommand;

@Getter
@Accessors(fluent = true)
public final class CoinshopModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private CoinshopService service;
    private Listener quitListener;

    public CoinshopModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.COINSHOP", false);
    }

    @Override
    public ModuleId id() {
        return ModuleId.COINSHOP;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        service = new CoinshopService(plugin);
        service.load();

        BukkitCommandBridge.register(plugin, new CoinshopCommand(plugin));

        quitListener = new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (service != null) service.unload(event.getPlayer().getUniqueId());
            }
        };
        Bukkit.getPluginManager().registerEvents(quitListener, plugin);
    }

    @Override
    public void disable() {
        if (quitListener != null) {
            HandlerList.unregisterAll(quitListener);
            quitListener = null;
        }
        service = null;
    }
}
