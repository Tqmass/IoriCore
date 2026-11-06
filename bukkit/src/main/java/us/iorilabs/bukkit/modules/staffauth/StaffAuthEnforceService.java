package us.iorilabs.bukkit.modules.staffauth;

import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.redis.CoreRedisEvent;
import us.iorilabs.bukkit.redis.CoreRedisService;

public final class StaffAuthEnforceService {

    private final IoriCorePlugin plugin;
    private final String staffPermission;
    private final int graceTicks;
    private final boolean enforceWithoutProxy;
    private final boolean restrictOnDeny;
    private final boolean requireLocalLogin;
    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();
    private final Set<UUID> restricted = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> pendingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, String> restrictMessage = new ConcurrentHashMap<>();
    private StaffRegService staffReg;

    public StaffAuthEnforceService(IoriCorePlugin plugin, String staffPermission, int graceTicks) {
        this.plugin = plugin;
        this.staffPermission =
            staffPermission == null || staffPermission.trim().isEmpty() ? "ioriproxy.staff" : staffPermission;
        this.graceTicks = graceTicks;
        this.enforceWithoutProxy = plugin.settings().getBoolean("STAFF-AUTH.ENFORCE-WITHOUT-PROXY", false);
        this.restrictOnDeny = plugin
            .settings()
            .getBoolean(
                "STAFF-AUTH.RESTRICT-ON-DENY",
                plugin.settings().getBoolean("STAFF-AUTH.RESTRICT-WHILE-PENDING", true)
            );
        this.requireLocalLogin = plugin.settings().getBoolean("STAFF-AUTH.REQUIRE-LOCAL-LOGIN", true);

        CoreRedisService redis = plugin.redis();
        if (redis != null) {
            redis.subscribe(NetworkEventType.STAFF_AUTH_SESSION, this::onSessionEvent);
        }
    }

    public void bindStaffReg(StaffRegService staffReg) {
        this.staffReg = staffReg;
        if (staffReg != null) staffReg.bindEnforce(this);
    }

    private void onSessionEvent(CoreRedisEvent event) {
        if (event == null || event.data() == null) return;

        JsonObject data = event.data();
        if (!data.has(NetworkEventKeys.UUID) || !data.has(NetworkEventKeys.APPROVED)) return;

        UUID uuid;
        try {
            uuid = UUID.fromString(data.get(NetworkEventKeys.UUID).getAsString());
        } catch (IllegalArgumentException | ClassCastException ex) {
            return;
        }

        boolean ok = data.get(NetworkEventKeys.APPROVED).getAsBoolean();
        Bukkit.getScheduler().runTask(plugin, () -> acceptSession(uuid, ok));
    }

    public void onJoin(Player player) {
        if (player == null || !isStaffCandidate(player)) return;

        UUID uuid = player.getUniqueId();
        cancelPending(uuid);
        if (authenticated.contains(uuid) || (staffReg != null && staffReg.isLoggedIn(uuid))) {
            clearRestrict(player);
            return;
        }

        if (shouldRestrictLocal(uuid)) {
            scheduleRestrict(uuid, "STAFF-AUTH.RESTRICTED-LOCAL");
            return;
        }

        if (!enforceWithoutProxy) return;

        scheduleRestrict(uuid, "STAFF-AUTH.RESTRICTED");
    }

    public void onQuit(UUID uuid) {
        if (uuid == null) return;
        cancelPending(uuid);
        authenticated.remove(uuid);
        restricted.remove(uuid);
        restrictMessage.remove(uuid);
        StaffRegService reg = staffReg;
        if (reg != null) reg.onQuit(uuid);
    }

    public void acceptSession(UUID uuid, boolean ok) {
        if (uuid == null) return;
        cancelPending(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (ok) {
            authenticated.add(uuid);
            if (player != null && player.isOnline()) clearRestrict(player);
            return;
        }
        authenticated.remove(uuid);
        if (!restrictOnDeny) return;
        if (player != null && player.isOnline()) restrict(player, "STAFF-AUTH.RESTRICTED");
    }

    public void acceptLocalLogin(UUID uuid) {
        if (uuid == null) return;
        cancelPending(uuid);
        authenticated.add(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) clearRestrict(player);
    }

    public boolean isRestricted(UUID uuid) {
        return uuid != null && restricted.contains(uuid);
    }

    public void clearAll() {
        for (Integer taskId : pendingTasks.values()) {
            if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        }
        pendingTasks.clear();
        for (UUID uuid : Set.copyOf(restricted)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) clearRestrict(player);
        }
        restricted.clear();
        restrictMessage.clear();
        authenticated.clear();
    }

    private boolean shouldRestrictLocal(UUID uuid) {
        if (!requireLocalLogin || staffReg == null) return false;
        return staffReg.isRegistered(uuid) && !staffReg.isLoggedIn(uuid);
    }

    private void scheduleRestrict(UUID uuid, String messageKey) {
        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(
            plugin,
            () -> {
                pendingTasks.remove(uuid);
                Player online = Bukkit.getPlayer(uuid);
                if (online == null || !online.isOnline()) return;
                if (authenticated.contains(uuid) || (staffReg != null && staffReg.isLoggedIn(uuid))) {
                    clearRestrict(online);
                    return;
                }
                if (shouldRestrictLocal(uuid) || enforceWithoutProxy) {
                    restrict(online, messageKey);
                }
            },
            graceTicks
        );
        pendingTasks.put(uuid, taskId);
    }

    private boolean isStaffCandidate(Player player) {
        if (player.hasPermission(staffPermission)) return true;
        return player.hasPermission("ioricore.staff");
    }

    private void restrict(Player player, String messageKey) {
        restricted.add(player.getUniqueId());
        restrictMessage.put(player.getUniqueId(), messageKey == null ? "STAFF-AUTH.RESTRICTED" : messageKey);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 60 * 60 * 24, 1));
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
        Map<String, String> ph = Collections.singletonMap("player", player.getName());
        String key = restrictMessage.getOrDefault(player.getUniqueId(), "STAFF-AUTH.RESTRICTED");
        for (String line : plugin.language().list(key, ph)) {
            player.sendMessage(line);
        }
    }

    private void clearRestrict(Player player) {
        restricted.remove(player.getUniqueId());
        restrictMessage.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
    }

    private void cancelPending(UUID uuid) {
        Integer taskId = pendingTasks.remove(uuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
    }
}
