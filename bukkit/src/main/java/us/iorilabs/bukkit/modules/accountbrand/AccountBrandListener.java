package us.iorilabs.bukkit.modules.accountbrand;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.accountbrand.enums.AccountKind;
import us.iorilabs.bukkit.modules.customjoins.CustomJoinsModule;
import us.iorilabs.bukkit.modules.customjoins.CustomJoinsService;

public final class AccountBrandListener implements Listener {

    private final IoriCorePlugin plugin;
    private final AccountBrandService service;
    private final boolean enabled;
    private final String staffPermission;
    private final String seePermission;

    public AccountBrandListener(IoriCorePlugin plugin, AccountBrandService service) {
        this.plugin = plugin;
        this.service = service;
        this.enabled = plugin.settings().getBoolean("ACCOUNT-BRAND.ENABLED", true);
        FileConfiguration permissions = plugin.permissions();
        this.staffPermission = firstNonBlank(
            permissions == null ? null : permissions.getString("ACCOUNT-BRAND.STAFF"),
            plugin.settings().getString("ACCOUNT-BRAND.STAFF-PERMISSION", "ioricore.staff")
        );
        this.seePermission = firstNonBlank(
            permissions == null ? null : permissions.getString("ACCOUNT-BRAND.SEE"),
            plugin.settings().getString("ACCOUNT-BRAND.SEE-PERMISSION", "ioricore.join.brand")
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onJoinSuppress(PlayerJoinEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        if (vipJoin(player)) return;
        if (isStaff(player)) event.setJoinMessage(null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> broadcastBrand(player), 5L);
    }

    private void broadcastBrand(Player player) {
        if (player == null || !player.isOnline() || vipJoin(player)) return;

        AccountKind kind = service.resolveKind(player);
        Map<String, String> ph = new HashMap<>();
        ph.put("player", player.getName());
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!canSee(staff)) continue;
            ph.put("account", plugin.language().plain(staff, kind.messageKey()));
            staff.sendMessage(plugin.language().raw(staff, "ACCOUNT-BRAND.STAFF-BRAND", ph));
        }
    }

    private boolean vipJoin(Player player) {
        CoreModule module = plugin.modules() == null ? null : plugin.modules().get(ModuleId.CUSTOM_JOINS);
        if (!(module instanceof CustomJoinsModule custom) || !custom.enabled()) return false;
        CustomJoinsService joins = custom.service();
        return joins != null && joins.matchJoin(player) != null;
    }

    private boolean isStaff(Player player) {
        if (player == null) return false;
        if (player.hasPermission("ioricore.admin")) return true;
        return staffPermission != null && !staffPermission.isEmpty() && player.hasPermission(staffPermission);
    }

    private boolean canSee(Player staff) {
        if (staff.hasPermission("ioricore.admin")) return true;
        if (seePermission != null && !seePermission.isEmpty() && staff.hasPermission(seePermission)) return true;
        return staffPermission != null && !staffPermission.isEmpty() && staff.hasPermission(staffPermission);
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) return primary.trim();
        return fallback == null ? "" : fallback.trim();
    }
}
