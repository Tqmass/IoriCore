package us.iorilabs.bukkit.modules.customjoins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class CustomJoinsListener implements Listener {

    private final IoriCorePlugin plugin;
    private final CustomJoinsService service;
    private final int motdDelayTicks;

    public CustomJoinsListener(IoriCorePlugin plugin, CustomJoinsService service) {
        this.plugin = plugin;
        this.service = service;
        this.motdDelayTicks = Math.max(1, plugin.settings().getInt("CUSTOM-JOINS.DELAY-TICKS", 20));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        CustomJoinsEntry entry = service.matchJoin(event.getPlayer());
        if (entry != null) {
            event.setJoinMessage(null);
            Bukkit.getScheduler().runTask(plugin, () -> service.broadcastJoin(event.getPlayer(), entry));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoinMotd(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> service.sendMotd(player), motdDelayTicks);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        CustomJoinsEntry entry = service.matchLeave(event.getPlayer());
        if (entry != null) {
            event.setQuitMessage(null);
            service.broadcastLeave(event.getPlayer(), entry);
        }
    }
}
