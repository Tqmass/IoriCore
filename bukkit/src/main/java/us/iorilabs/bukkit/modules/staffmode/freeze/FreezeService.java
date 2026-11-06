package us.iorilabs.bukkit.modules.staffmode.freeze;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.api.punishment.PunishmentType;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.staffmode.StaffModeConfig;
import us.iorilabs.bukkit.modules.staffmode.StaffModeModule;
import us.iorilabs.bukkit.modules.staffmode.StaffModeRedisBridge;
import us.iorilabs.bukkit.punishment.PunishmentService;
import us.iorilabs.bukkit.punishment.PunishmentsModule;
import us.iorilabs.bukkit.util.LegacyMaterials;

public final class FreezeService {

    public static final String BYPASS_PERM = "ioricore.staffmode.freeze.bypass";
    public static final String STAFF_PERM = "ioricore.staffmode.freeze";

    private static final UUID CONSOLE = new UUID(0L, 0L);
    private static final String MESSAGE_TOKEN = "\u0000msg\u0000";

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final StaffModeConfig config;
    private final List<String> allowedCommands = new ArrayList<>();
    private final List<String> disconnectCommands;
    private final Map<UUID, UUID> frozen = new ConcurrentHashMap<>();
    private final Set<UUID> iceHelmet = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ItemStack> helmets = new ConcurrentHashMap<>();

    public FreezeService(IoriCorePlugin plugin, CoreLanguage language, StaffModeConfig config) {
        this.plugin = plugin;
        this.language = language;
        this.config = config;
        for (String raw : config.getStringList("FREEZE.ALLOWED-COMMANDS")) {
            String label = normalizeLabel(raw);
            if (!label.isEmpty()) allowedCommands.add(label);
        }
        this.disconnectCommands = new ArrayList<>(config.getStringList("FREEZE.ON-DISCONNECT"));
    }

    public boolean frozen(UUID id) {
        return id != null && frozen.containsKey(id);
    }

    public void toggle(Player staff, Player target) {
        if (staff == null || target == null) return;
        setFrozen(target, !frozen(target.getUniqueId()), staff);
    }

    public void setFrozen(Player target, boolean enable, Player staff) {
        setFrozen(target, enable, staff, true);
    }

    public void applyRemote(UUID id, boolean enable) {
        if (id == null) return;
        Player target = Bukkit.getPlayer(id);
        if (target == null || !target.isOnline()) return;
        setFrozen(target, enable, null, false);
    }

    public void setFrozen(Player target, boolean enable, Player staff, boolean sync) {
        if (target == null) return;
        UUID id = target.getUniqueId();
        Map<String, String> placeholders = Map.of("player", target.getName());

        if (!enable) {
            if (frozen.remove(id) == null) return;
            clearEffects(target);
            if (staff != null) staff.sendMessage(language.raw("STAFF-MODE.FREEZE-OFF", placeholders));
            target.sendMessage(language.raw("STAFF-MODE.FREEZE-TARGET-OFF"));
            if (sync) publish(id, false);
            return;
        }

        if (frozen(id)) return;
        if (staff != null && !staffInMode(staff)) {
            staff.sendMessage(language.raw("STAFF-MODE.FREEZE-REQUIRE-MODE"));
            return;
        }
        if (target.hasPermission(BYPASS_PERM) || (staff != null && staff.equals(target))) {
            if (staff != null) staff.sendMessage(language.raw("STAFF-MODE.FREEZE-BYPASS", placeholders));
            return;
        }

        frozen.put(id, staff == null ? CONSOLE : staff.getUniqueId());
        applyEffects(target);
        if (staff != null) staff.sendMessage(language.raw("STAFF-MODE.FREEZE-ON", placeholders));
        target.sendMessage(language.raw("STAFF-MODE.FREEZE-TARGET-ON"));
        if (plugin.titles() != null) {
            plugin
                .titles()
                .send(target, language.raw("STAFF-MODE.FREEZE-TITLE"), language.raw("STAFF-MODE.FREEZE-SUBTITLE"));
        }
        if (sync) publish(id, true);
    }

    public void tickActionBars() {
        if (frozen.isEmpty()) return;
        String message = language.raw("STAFF-MODE.FREEZE-ACTIONBAR");
        for (UUID id : frozen.keySet()) {
            Player target = Bukkit.getPlayer(id);
            if (target == null || !target.isOnline()) continue;
            applyEffects(target);
            if (plugin.actionBars() != null) plugin.actionBars().send(target, message);
        }
    }

    public boolean commandAllowed(String commandLine) {
        String label = normalizeLabel(commandLine);
        return !label.isEmpty() && allowedCommands.contains(label);
    }

    public void sendIsolatedChat(Player sender, String message) {
        String line = language
            .raw("STAFF-MODE.FREEZE-CHAT", Map.of("player", sender.getName(), "message", MESSAGE_TOKEN))
            .replace(MESSAGE_TOKEN, message == null ? "" : message);
        sender.sendMessage(line);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender)) continue;
            if (isStaff(online)) online.sendMessage(line);
        }
    }

    public void handleQuit(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        UUID staffId = frozen.get(id);
        if (staffId == null) return;
        clearEffects(player);

        String staffName = staffName(staffId);
        Map<String, String> placeholders = Map.of("player", player.getName(), "staff", staffName);
        String alert = language.raw("STAFF-MODE.FREEZE-QUIT", placeholders);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (isStaff(online)) online.sendMessage(alert);
        }

        if (config.getBoolean("FREEZE.ON-DISCONNECT-AUTOBAN", true)) {
            applyDisconnectAutoban(player, staffId, staffName);
        }

        for (String command : disconnectCommands) {
            if (command == null || command.trim().isEmpty()) continue;
            String resolved = command.replace("<player>", player.getName()).replace("<staff>", staffName).trim();
            if (resolved.startsWith("/")) resolved = resolved.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }

        frozen.remove(id);
    }

    private void applyDisconnectAutoban(Player player, UUID staffId, String staffName) {
        if (player == null) return;
        PunishmentsModule module = plugin.modules() == null ? null : plugin.modules().get(ModuleId.PUNISHMENTS);
        if (module == null || module.punishments() == null) return;

        String reason = language.plain("STAFF-MODE.FREEZE-DISCONNECT");
        if (reason == null || reason.trim().isEmpty()) reason = "FREEZE-DISCONNECT";

        String durationRaw = config.getString("FREEZE.ON-DISCONNECT-BAN-DURATION", "");
        long durationMs = PunishmentService.parseDurationMillis(durationRaw == null ? "" : durationRaw.trim());
        PunishmentType type = durationMs > 0L ? PunishmentType.TEMPBAN : PunishmentType.BAN;
        long expiresAt = durationMs > 0L ? System.currentTimeMillis() + durationMs : 0L;

        module
            .punishments()
            .punish(type, player.getUniqueId(), player.getName(), staffId, staffName, reason, expiresAt, true);
    }

    public void handleJoin(Player player) {
        if (player == null) return;
        if (!frozen(player.getUniqueId())) return;
        applyEffects(player);
        player.sendMessage(language.raw("STAFF-MODE.FREEZE-TARGET-ON"));
        if (plugin.titles() != null) {
            plugin
                .titles()
                .send(player, language.raw("STAFF-MODE.FREEZE-TITLE"), language.raw("STAFF-MODE.FREEZE-SUBTITLE"));
        }
    }

    public void shutdown() {
        for (UUID id : new ArrayList<>(frozen.keySet())) {
            Player target = Bukkit.getPlayer(id);
            if (target != null) clearEffects(target);
        }
        frozen.clear();
        iceHelmet.clear();
        helmets.clear();
    }

    public static boolean isStaff(Player player) {
        return player != null && (player.hasPermission(STAFF_PERM) || player.hasPermission("ioricore.admin"));
    }

    private boolean staffInMode(Player staff) {
        if (!config.getBoolean("FREEZE.REQUIRE-STAFF-MODE", true)) return true;
        if (plugin.modules() == null) return true;
        StaffModeModule module = plugin.modules().get(ModuleId.STAFF_MODE);
        return module != null && module.service() != null && module.service().enabled(staff.getUniqueId());
    }

    private static String staffName(UUID staffId) {
        if (CONSOLE.equals(staffId)) return "CONSOLE";
        OfflinePlayer staff = Bukkit.getOfflinePlayer(staffId);
        return staff.getName() == null ? staffId.toString() : staff.getName();
    }

    private static String normalizeLabel(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        int space = trimmed.indexOf(' ');
        if (space >= 0) trimmed = trimmed.substring(0, space);
        int colon = trimmed.indexOf(':');
        if (colon >= 0) trimmed = trimmed.substring(colon + 1);
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private void applyEffects(Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 6, false), true);
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false), true);
        if (config.getBoolean("FREEZE.BLIND", true)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false), true);
        }
        if (config.getBoolean("FREEZE.ICE-HELMET", true)) {
            UUID id = target.getUniqueId();
            if (iceHelmet.add(id)) {
                ItemStack current = target.getInventory().getHelmet();
                if (current != null) helmets.put(id, current.clone());
                target.getInventory().setHelmet(new ItemStack(LegacyMaterials.of("PACKED_ICE", "ICE")));
            }
        }
    }

    private void clearEffects(Player target) {
        target.removePotionEffect(PotionEffectType.SLOW);
        target.removePotionEffect(PotionEffectType.JUMP);
        target.removePotionEffect(PotionEffectType.BLINDNESS);
        UUID id = target.getUniqueId();
        if (iceHelmet.remove(id)) target.getInventory().setHelmet(helmets.remove(id));
    }

    private void publish(UUID id, boolean frozenNow) {
        StaffModeRedisBridge bridge = plugin.staffModeSync();
        if (bridge != null) bridge.publishFreeze(id, frozenNow);
    }
}
