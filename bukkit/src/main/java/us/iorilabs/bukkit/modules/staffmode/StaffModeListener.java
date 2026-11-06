package us.iorilabs.bukkit.modules.staffmode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.staffmode.freeze.FreezeService;
import us.iorilabs.bukkit.util.LegacyMaterials;

@RequiredArgsConstructor
public final class StaffModeListener implements Listener {

    private final StaffModeService staffMode;
    private final StaffChatService staffChat;
    private final CoreLanguage language;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        staffMode.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        staffMode.handleQuit(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (staffMode.invulnerable(event.getEntity().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player damager = damager(event);
        if (damager != null && staffMode.enabled(damager.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        if (staffMode.enabled(event.getTarget().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (staffMode.enabled(event.getEntity().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!staffMode.enabled(event.getEntity().getUniqueId())) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        cancelIfRestricted(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        cancelIfRestricted(event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (staffMode.enabled(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onPickup(PlayerPickupItemEvent event) {
        if (staffMode.enabled(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (staffMode.enabled(event.getWhoClicked().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (staffMode.enabled(event.getWhoClicked().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        cancelIfRestricted(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    @SuppressWarnings("deprecation")
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player staff = event.getPlayer();
        if (!staffMode.enabled(staff.getUniqueId())) return;
        if (!StaffClickGuard.mainHand(event)) {
            event.setCancelled(true);
            return;
        }

        ItemStack hand = staff.getItemInHand();
        boolean freezeItem = isHotbar(staff, hand, "FREEZE", LegacyMaterials.of("PACKED_ICE", "ICE"));
        boolean inspectItem = !freezeItem && isHotbar(staff, hand, "INSPECT", LegacyMaterials.of("CHEST"));
        if ((freezeItem || inspectItem) && event.getRightClicked() instanceof Player) {
            event.setCancelled(true);
            if (!staffMode.clicks().tryUse(staff.getUniqueId())) return;
            Player target = (Player) event.getRightClicked();
            if (freezeItem) freeze(staff, target);
            else inspect(staff, target);
            return;
        }
        cancelIfRestricted(staff, event);
    }

    @EventHandler(priority = EventPriority.LOW)
    @SuppressWarnings("deprecation")
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!staffMode.enabled(player.getUniqueId())) return;
        if (!StaffClickGuard.mainHand(event)) {
            event.setCancelled(true);
            return;
        }

        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            String key = hotbarKey(player, player.getItemInHand());
            if (key != null) {
                event.setCancelled(true);
                if (staffMode.clicks().tryUse(player.getUniqueId())) useHotbar(player, key);
                return;
            }
        }
        if (action != Action.RIGHT_CLICK_AIR && action != Action.LEFT_CLICK_AIR) cancelIfRestricted(player, event);
    }

    private void useHotbar(Player player, String key) {
        switch (key) {
            case "VANISH":
                if (!allowed(player, "ioricore.staffmode.vanish")) return;
                staffMode.toggleVanish(player);
                return;
            case "RANDOM-TP":
                if (!allowed(player, "ioricore.staffmode.randomtp")) return;
                randomTeleport(player);
                return;
            case "PLAYERS":
                if (!allowed(player, "ioricore.staffmode.players")) return;
                staffMode.openPlayers(player);
                return;
            case "THRU":
                if (!allowed(player, "ioricore.staffmode.thru")) return;
                staffMode.phaseThrough(player);
                return;
            case "STAFF-CHAT":
                staffChat.toggle(player);
                return;
            case "EXIT":
                staffMode.disable(player);
                return;
            default:
        }
    }

    private void freeze(Player staff, Player target) {
        if (!allowed(staff, FreezeService.STAFF_PERM)) return;
        staffMode.toggleFreeze(staff, target);
    }

    private void inspect(Player staff, Player target) {
        if (!allowed(staff, "ioricore.staffmode.inspect")) return;
        staffMode.openInspect(staff, target);
    }

    private void randomTeleport(Player player) {
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (staffMode.vanished(online.getUniqueId()) || staffMode.enabled(online.getUniqueId())) continue;
            candidates.add(online);
        }
        if (candidates.isEmpty()) {
            player.sendMessage(language.raw("STAFF-MODE.NO-TARGET"));
            return;
        }
        Player target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        player.teleport(target.getLocation());
        player.sendMessage(language.raw("STAFF-MODE.RANDOM-TP", Map.of("player", target.getName())));
    }

    private boolean allowed(Player player, String permission) {
        if (player.hasPermission(permission) || player.hasPermission("ioricore.admin")) return true;
        player.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
        return false;
    }

    private void cancelIfRestricted(Player player, Cancellable event) {
        if (staffMode.restricted(player)) event.setCancelled(true);
    }

    private String hotbarKey(Player player, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        if (isHotbar(player, stack, "VANISH", LegacyMaterials.of("GRAY_DYE", "INK_SACK"))) return "VANISH";
        if (isHotbar(player, stack, "VANISH-ON", LegacyMaterials.of("LIME_DYE", "INK_SACK"), "VANISH")) return "VANISH";
        if (isHotbar(player, stack, "RANDOM-TP", LegacyMaterials.of("COMPASS"))) return "RANDOM-TP";
        if (isHotbar(player, stack, "PLAYERS", LegacyMaterials.of("PAPER"))) return "PLAYERS";
        if (isHotbar(player, stack, "THRU", LegacyMaterials.of("ENDER_PEARL"))) return "THRU";
        if (isHotbar(player, stack, "STAFF-CHAT", LegacyMaterials.of("BOOK"))) return "STAFF-CHAT";
        if (isHotbar(player, stack, "EXIT", LegacyMaterials.of("REDSTONE"))) return "EXIT";
        return null;
    }

    private boolean isHotbar(Player player, ItemStack stack, String key, Material fallback) {
        return isHotbar(player, stack, key, fallback, key);
    }

    private boolean isHotbar(Player player, ItemStack stack, String itemKey, Material fallback, String langKey) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        if (!staffMode.hotbarEnabled(player, langKey)) return false;
        Material expected = staffMode.hotbarMaterial(player, itemKey, fallback);
        if (stack.getType() != expected && stack.getType() != fallback) return false;
        if (!stack.hasItemMeta() || !stack.getItemMeta().hasDisplayName()) return false;
        String display = stack.getItemMeta().getDisplayName();
        String prefix = "STAFF-MODE.HOTBAR." + langKey;
        return display.equals(language.raw(prefix + ".NAME")) || display.equals(language.raw(prefix + ".NAME-ON"));
    }

    private static Player damager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) return (Player) event.getDamager();
        if (!(event.getDamager() instanceof Projectile)) return null;
        Object shooter = ((Projectile) event.getDamager()).getShooter();
        return shooter instanceof Player ? (Player) shooter : null;
    }
}
