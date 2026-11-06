package us.iorilabs.bukkit.modules.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public final class WorldManagementListener implements Listener {

    private final boolean disableMobSpawn;
    private final boolean disableWeather;
    private final boolean disableExplosions;
    private final boolean disableHunger;
    private final boolean disableFallDamage;
    private final boolean disableFireSpread;
    private final boolean disableLeafDecay;
    private final boolean disableBlockBreak;
    private final boolean disableBlockPlace;

    public WorldManagementListener(FileConfiguration settings) {
        this.disableMobSpawn = settings.getBoolean("WORLD.DISABLE-MOB-SPAWN", false);
        this.disableWeather = settings.getBoolean("WORLD.DISABLE-WEATHER", false);
        this.disableExplosions = settings.getBoolean("WORLD.DISABLE-EXPLOSIONS", false);
        this.disableHunger = settings.getBoolean("WORLD.DISABLE-HUNGER", false);
        this.disableFallDamage = settings.getBoolean("WORLD.DISABLE-FALL-DAMAGE", false);
        this.disableFireSpread = settings.getBoolean("WORLD.DISABLE-FIRE-SPREAD", false);
        this.disableLeafDecay = settings.getBoolean("WORLD.DISABLE-LEAF-DECAY", false);
        this.disableBlockBreak = settings.getBoolean("WORLD.DISABLE-BLOCK-BREAK", false);
        this.disableBlockPlace = settings.getBoolean("WORLD.DISABLE-BLOCK-PLACE", false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!disableMobSpawn) return;
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent event) {
        if (disableWeather && event.toWeatherState()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onThunder(ThunderChangeEvent event) {
        if (disableWeather && event.toThunderState()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (!disableExplosions) return;
        event.setCancelled(true);
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!disableHunger) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (event.getFoodLevel() < player.getFoodLevel()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (!disableFallDamage) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        if (disableLeafDecay) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (disableFireSpread) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (disableFireSpread) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (disableFireSpread) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!disableBlockBreak) return;
        if (event.getPlayer() != null && event.getPlayer().hasPermission("ioricore.world.bypass")) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!disableBlockPlace) return;
        if (event.getPlayer() != null && event.getPlayer().hasPermission("ioricore.world.bypass")) return;
        event.setCancelled(true);
    }
}
