package us.iorilabs.bukkit.modules.core;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import us.iorilabs.bukkit.modules.core.home.HomeService;
import us.iorilabs.bukkit.modules.core.spawn.SpawnService;
import us.iorilabs.bukkit.modules.core.tpa.TpaService;
import us.iorilabs.bukkit.modules.core.vanish.VanishService;

public final class EssentialsListener implements Listener {

    private final EssentialsState state;
    private final SpawnService spawn;
    private final HomeService homes;
    private final TpaService tpa;
    private final VanishService vanish;
    private final boolean spawnOnJoin;
    private final boolean spawnOnDeath;

    public EssentialsListener(
        EssentialsState state,
        SpawnService spawn,
        HomeService homes,
        TpaService tpa,
        VanishService vanish,
        boolean spawnOnJoin,
        boolean spawnOnDeath
    ) {
        this.state = state;
        this.spawn = spawn;
        this.homes = homes;
        this.tpa = tpa;
        this.vanish = vanish;
        this.spawnOnJoin = spawnOnJoin;
        this.spawnOnDeath = spawnOnDeath;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getFrom() == null) return;
        Player player = event.getPlayer();
        if (player == null) return;
        state.rememberBack(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        state.rememberBack(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (state.god(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        state.applyNick(player);
        if (vanish != null) vanish.handleJoin(player);
        if (spawnOnJoin && spawn.spawn() != null) {
            spawn.teleport(player);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!spawnOnDeath || spawn.spawn() == null) return;
        event.setRespawnLocation(spawn.spawn());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        homes.unload(player.getUniqueId());
        tpa.clear(player.getUniqueId());
        state.clear(player.getUniqueId());
        if (vanish != null) vanish.cleanup(player);
    }
}
