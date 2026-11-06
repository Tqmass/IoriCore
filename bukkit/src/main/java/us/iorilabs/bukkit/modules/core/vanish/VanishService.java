package us.iorilabs.bukkit.modules.core.vanish;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.staffmode.StaffModeModule;
import us.iorilabs.bukkit.modules.staffmode.StaffModeRedisBridge;
import us.iorilabs.bukkit.modules.staffmode.StaffModeService;

@RequiredArgsConstructor
public final class VanishService {

    private static final String SEE_PERM = "ioricore.vanish.see";

    private final IoriCorePlugin plugin;
    private final Set<UUID> vanished = new HashSet<>();

    public boolean vanished(UUID uuid) {
        return uuid != null && vanished.contains(uuid);
    }

    public boolean toggle(Player player) {
        if (player == null) return false;
        boolean next = !vanished(player.getUniqueId());
        setVanished(player, next);
        return next;
    }

    public void setVanished(Player player, boolean enable) {
        setVanished(player, enable, true);
    }

    public void applyRemote(UUID id, boolean enable) {
        if (id == null) return;
        Player player = Bukkit.getPlayer(id);
        if (player == null || !player.isOnline()) return;
        setVanished(player, enable, false);
    }

    public void setVanished(Player player, boolean enable, boolean sync) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        if (vanished(id) == enable) return;

        if (enable) {
            vanished.add(id);
            player.setPlayerListName(" ");
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.equals(player)) continue;
                if (canSeeVanished(other)) continue;
                other.hidePlayer(player);
            }
        } else {
            vanished.remove(id);
            restoreListName(player);
            for (Player other : Bukkit.getOnlinePlayers()) {
                other.showPlayer(player);
            }
        }
        syncStaffMode(player, enable);
        if (sync) publish(id, enable);
    }

    public void handleJoin(Player joining) {
        if (joining == null) return;
        boolean viewer = canSeeVanished(joining);
        for (UUID vanishedId : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(vanishedId);
            if (vanishedPlayer == null || vanishedPlayer.equals(joining)) continue;
            if (viewer) {
                joining.showPlayer(vanishedPlayer);
            } else {
                joining.hidePlayer(vanishedPlayer);
            }
        }
        if (vanished.contains(joining.getUniqueId())) {
            joining.setPlayerListName(" ");
            if (!viewer) {
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (other.equals(joining)) continue;
                    if (canSeeVanished(other)) continue;
                    other.hidePlayer(joining);
                }
            }
        }
    }

    public void cleanup(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        if (!vanished.remove(id)) return;
        restoreListName(player);
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(player);
        }
        syncStaffMode(player, false);
    }

    private void syncStaffMode(Player player, boolean enable) {
        if (plugin.modules() == null) return;
        StaffModeModule module = plugin.modules().get(ModuleId.STAFF_MODE);
        if (module == null || !module.enabled()) return;
        StaffModeService service = module.service();
        if (service == null) return;
        service.onVanishChanged(player, enable);
    }

    private void publish(UUID id, boolean vanishedNow) {
        StaffModeRedisBridge bridge = plugin.staffModeSync();
        if (bridge != null) bridge.publishVanish(id, vanishedNow);
    }

    private static boolean canSeeVanished(Player player) {
        if (player == null) return false;
        return (
            player.hasPermission(SEE_PERM) ||
            player.hasPermission("ioricore.staff") ||
            player.hasPermission("ioricore.admin")
        );
    }

    private static void restoreListName(Player player) {
        if (player == null) return;
        String name = player.getDisplayName();
        if (name == null || name.isEmpty()) name = player.getName();
        String plain = org.bukkit.ChatColor.stripColor(name);
        if (plain.length() > 16) plain = plain.substring(0, 16);
        player.setPlayerListName(plain);
    }
}
