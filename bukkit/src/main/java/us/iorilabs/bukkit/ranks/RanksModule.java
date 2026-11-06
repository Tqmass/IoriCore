package us.iorilabs.bukkit.ranks;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.ranks.command.GrantCommand;
import us.iorilabs.bukkit.ranks.command.GrantsCommand;
import us.iorilabs.bukkit.ranks.command.UngrantCommand;

@Getter
@Accessors(fluent = true)
public final class RanksModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private RankService ranks;
    private RankListener listener;
    private RankRedisBridge redisBridge;

    public RanksModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.RANKS", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.RANKS;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        ranks = new RankService(plugin, plugin.storage(), plugin.settings());
        ranks.luckPerms(plugin.luckPermsHook());
        ranks.redis(plugin.redis());
        ranks.load();
        ranks.ensureLuckPermsMode();
        ranks.start();

        listener = new RankListener(ranks);
        Bukkit.getPluginManager().registerEvents(listener, plugin);

        if (plugin.redis() != null) {
            redisBridge = new RankRedisBridge(ranks, plugin.redis());
            redisBridge.register();
        }

        BukkitCommandBridge.register(plugin, new RanksCommand(plugin, plugin.language(), ranks));
        BukkitCommandBridge.register(plugin, new GrantCommand(plugin.language(), ranks));
        BukkitCommandBridge.register(plugin, new UngrantCommand(plugin.language(), ranks));
        BukkitCommandBridge.register(plugin, new GrantsCommand(plugin.language(), ranks));
    }

    @Override
    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (ranks != null) {
            ranks.stop();
            ranks = null;
        }
        redisBridge = null;
    }
}
