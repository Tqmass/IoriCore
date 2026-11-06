package us.iorilabs.bukkit.modules.staffmode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import us.iorilabs.api.hook.ApolloHook;
import us.iorilabs.api.hook.NoopApolloHook;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuDefinition;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;
import us.iorilabs.bukkit.modules.core.CoreFeaturesModule;
import us.iorilabs.bukkit.modules.core.vanish.VanishService;
import us.iorilabs.bukkit.modules.staffmode.freeze.FreezeService;
import us.iorilabs.bukkit.modules.staffmode.inventory.StaffInventoryStore;
import us.iorilabs.bukkit.modules.staffmode.inventory.StaffSnapshot;
import us.iorilabs.bukkit.modules.staffmode.invsee.StaffInvseeService;
import us.iorilabs.bukkit.util.LegacyMaterials;

@Getter
@Accessors(fluent = true)
public final class StaffModeService {

    public static final String TEAM_NAME = "icStaff";
    public static final String BUILD_PERM = "ioricore.staffmode.build";

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final YamlMenuService yamlMenus;
    private final StaffModeConfig config;
    private final FreezeService freeze;
    private final StaffInventoryStore store;
    private final StaffClickGuard clicks;
    private final StaffInvseeService invsee;
    private final Set<UUID> staffMode = new HashSet<>();
    private final Set<UUID> invulnerable = new HashSet<>();
    private final Map<UUID, StaffSnapshot> snapshots = new HashMap<>();
    private VanishService fallbackVanish;
    private ApolloHook apollo = new NoopApolloHook();

    public StaffModeService(
        IoriCorePlugin plugin,
        CoreLanguage language,
        YamlMenuService yamlMenus,
        StaffModeConfig config
    ) {
        this.plugin = plugin;
        this.language = language;
        this.yamlMenus = yamlMenus;
        this.config = config;
        this.freeze = new FreezeService(plugin, language, config);
        this.store = new StaffInventoryStore(plugin);
        this.clicks = new StaffClickGuard(config.getLong("CLICK-COOLDOWN-MS", 150L));
        this.invsee = new StaffInvseeService(plugin, language);
    }

    public void apollo(ApolloHook apollo) {
        this.apollo = apollo == null ? new NoopApolloHook() : apollo;
    }

    public boolean enabled(UUID uuid) {
        return uuid != null && staffMode.contains(uuid);
    }

    public boolean vanished(UUID uuid) {
        return vanish().vanished(uuid);
    }

    public boolean frozen(UUID uuid) {
        return freeze.frozen(uuid);
    }

    public boolean invulnerable(UUID uuid) {
        return uuid != null && invulnerable.contains(uuid);
    }

    public boolean restricted(Player player) {
        return player != null && enabled(player.getUniqueId()) && !player.hasPermission(BUILD_PERM);
    }

    public VanishService vanish() {
        if (plugin.modules() != null) {
            CoreFeaturesModule essentials = plugin.modules().get(ModuleId.CORE);
            if (essentials != null && essentials.enabled() && essentials.vanish() != null) return essentials.vanish();
        }
        if (fallbackVanish == null) fallbackVanish = new VanishService(plugin);
        return fallbackVanish;
    }

    public boolean toggle(Player player) {
        if (player == null) return false;
        if (enabled(player.getUniqueId())) {
            disable(player);
            return false;
        }
        enable(player);
        return true;
    }

    public void enable(Player player) {
        if (player == null || enabled(player.getUniqueId())) return;

        UUID id = player.getUniqueId();
        StaffSnapshot snapshot = StaffSnapshot.capture(player, vanished(id));
        snapshots.put(id, snapshot);
        store.save(id, snapshot);
        staffMode.add(id);

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        for (PotionEffect active : player.getActivePotionEffects()) {
            player.removePotionEffect(active.getType());
        }
        applyGameMode(player);
        player.setFoodLevel(20);
        applyHotbar(player);
        applyNametag(player, true);
        applyFlight(player, true);
        applyNightVision(player, true);
        applyInvulnerability(player, true);
        if (config.getBoolean("VANISH-ON-ENTER", true)) vanish().setVanished(player, true);
        refreshVanishItem(player, vanished(id));
        applyGlow(player, true);

        player.sendMessage(language.raw("STAFF-MODE.ENABLED"));
    }

    public void disable(Player player) {
        disable(player, false);
    }

    public void onVanishChanged(Player player, boolean vanishedNow) {
        if (player == null || !enabled(player.getUniqueId())) return;
        applyNametag(player, true);
        refreshVanishItem(player, vanishedNow);
    }

    public void toggleVanish(Player player) {
        if (player == null) return;
        boolean next = !vanished(player.getUniqueId());
        vanish().setVanished(player, next);
        player.sendMessage(language.raw(next ? "STAFF-MODE.VANISH-ON" : "STAFF-MODE.VANISH-OFF"));
    }

    public void toggleFreeze(Player staff, Player target) {
        freeze.toggle(staff, target);
    }

    public void openStaffHub(Player staff) {
        if (staff == null || yamlMenus == null) return;
        String menuId = config.getString("STAFF-COMMAND.MENU-ID", "staff-players");
        yamlMenus.open(staff, menuId == null || menuId.isBlank() ? "staff-players" : menuId.trim());
    }

    public void openPlayers(Player staff) {
        if (staff == null || yamlMenus == null) return;
        yamlMenus.open(staff, "staff-players");
    }

    public void openInspect(Player staff, Player target) {
        if (staff == null || target == null || yamlMenus == null) return;
        yamlMenus.open(staff, "staff-inspect", Map.of("target", target.getName(), "player", target.getName()));
        staff.sendMessage(language.raw("STAFF-MODE.INSPECT", Map.of("player", target.getName())));
    }

    public void openInvsee(Player staff, Player target) {
        if (staff == null || target == null) return;
        invsee.open(staff, target);
    }

    public void phaseThrough(Player player) {
        if (player == null) return;
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        for (int distance = 1; distance <= 5; distance++) {
            Location probe = eye.clone().add(direction.clone().multiply(distance));
            Block feet = probe.getBlock();
            Block head = feet.getRelative(0, 1, 0);
            Block ground = feet.getRelative(0, -1, 0);
            if (!passable(feet) || !passable(head) || passable(ground)) continue;

            Location dest = feet.getLocation().add(0.5D, 0.0D, 0.5D);
            dest.setYaw(player.getLocation().getYaw());
            dest.setPitch(player.getLocation().getPitch());
            player.teleport(dest);
            player.sendMessage(language.raw("STAFF-MODE.THRU"));
            return;
        }
        player.sendMessage(language.raw("STAFF-MODE.THRU-FAIL"));
    }

    public void handleJoin(Player joining) {
        if (joining == null) return;
        if (fallbackVanish != null) fallbackVanish.handleJoin(joining);
        syncBoard(joining.getScoreboard());

        UUID id = joining.getUniqueId();
        store.load(id, snapshot -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null || !online.isOnline() || enabled(id)) return;
            snapshot.restore(online, config.getBoolean("RESTORE-LOCATION", true));
            store.delete(id);
            online.sendMessage(language.raw("STAFF-MODE.INVENTORY-RESTORED"));
        });
    }

    public void handleQuit(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        if (enabled(id)) disable(player, true);
        if (fallbackVanish != null) fallbackVanish.cleanup(player);
        clicks.forget(id);
        removeFromTeams(player);
    }

    public void syncBoard(Scoreboard board) {
        if (board == null || staffMode.isEmpty()) return;
        if (!staffNametagEnabled()) return;
        String prefix = truncate(language.raw("STAFF-MODE.NAMETAG-PREFIX"), 16);
        for (UUID id : staffMode) {
            Player staff = Bukkit.getPlayer(id);
            if (staff != null) syncStaffTeam(board, staff, prefix);
        }
    }

    public void shutdown() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (enabled(online.getUniqueId())) disable(online);
        }
        freeze.shutdown();
        store.shutdown();
        staffMode.clear();
        invulnerable.clear();
        snapshots.clear();
    }

    public boolean hotbarEnabled(Player player, String key) {
        YamlMenuDefinition def = staffDefinition(player);
        if (def == null || def.raw() == null || key == null) return true;
        return def.raw().getBoolean("ITEMS." + key + ".ENABLED", true);
    }

    public Material hotbarMaterial(Player player, String key, Material fallback) {
        YamlMenuDefinition def = staffDefinition(player);
        return def == null ? fallback : def.resolveMaterial(key, fallback);
    }

    private void disable(Player player, boolean quitting) {
        if (player == null || !enabled(player.getUniqueId())) return;

        UUID id = player.getUniqueId();
        staffMode.remove(id);
        StaffSnapshot snapshot = snapshots.remove(id);
        applyGlow(player, false);
        applyNametag(player, false);
        applyInvulnerability(player, false);
        if (!quitting) vanish().setVanished(player, snapshot != null && snapshot.vanished());

        if (snapshot != null) {
            snapshot.restore(player, config.getBoolean("RESTORE-LOCATION", true));
            store.delete(id);
        } else {
            player.getInventory().clear();
            applyNightVision(player, false);
        }

        if (!quitting) player.sendMessage(language.raw("STAFF-MODE.DISABLED"));
    }

    private void applyGameMode(Player player) {
        String raw = config.getString("GAMEMODE", "NONE");
        if (raw == null || raw.trim().isEmpty() || "NONE".equalsIgnoreCase(raw.trim())) return;
        try {
            player.setGameMode(GameMode.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {}
    }

    private void applyHotbar(Player player) {
        giveHotbar(player, "RANDOM-TP", 0, LegacyMaterials.of("COMPASS"));
        giveHotbar(player, "FREEZE", 1, LegacyMaterials.of("PACKED_ICE", "ICE"));
        giveHotbar(player, "INSPECT", 2, LegacyMaterials.of("CHEST"));
        giveHotbar(player, "PLAYERS", 3, LegacyMaterials.of("PAPER"));
        giveHotbar(player, "THRU", 5, LegacyMaterials.of("ENDER_PEARL"));
        giveHotbar(player, "STAFF-CHAT", 6, LegacyMaterials.of("BOOK"));
        giveHotbar(player, "EXIT", 8, LegacyMaterials.of("REDSTONE"));
    }

    @SuppressWarnings("deprecation")
    private void giveHotbar(Player player, String key, int defaultSlot, Material fallback) {
        if (!hotbarEnabled(player, key)) return;
        YamlMenuDefinition def = staffDefinition(player);
        int slot = def == null ? defaultSlot : def.slot(key, defaultSlot);
        if (slot < 0 || slot >= 9) return;
        Material material = def == null ? fallback : def.resolveMaterial(key, fallback);
        short data = def == null ? 0 : def.itemData(key, (short) 0);
        ItemStack stack = new ItemStack(material, 1, data);
        decorate(stack, "STAFF-MODE.HOTBAR." + key + ".NAME", "STAFF-MODE.HOTBAR." + key + ".LORE");
        player.getInventory().setItem(slot, stack);
    }

    @SuppressWarnings("deprecation")
    private void refreshVanishItem(Player player, boolean vanishedNow) {
        if (!hotbarEnabled(player, "VANISH")) return;
        YamlMenuDefinition def = staffDefinition(player);
        int slot = def == null ? 4 : def.slot("VANISH", 4);
        if (slot < 0 || slot >= 9) return;
        String itemKey = vanishedNow ? "VANISH-ON" : "VANISH";
        Material fallback = vanishedNow
            ? LegacyMaterials.of("LIME_DYE", "INK_SACK")
            : LegacyMaterials.of("GRAY_DYE", "INK_SACK");
        short fallbackData = vanishedNow ? (short) 10 : (short) 8;
        Material material = def == null ? fallback : def.resolveMaterial(itemKey, fallback);
        short data = def == null ? fallbackData : def.itemData(itemKey, fallbackData);
        ItemStack stack = new ItemStack(material, 1, data);
        String nameKey = vanishedNow ? "STAFF-MODE.HOTBAR.VANISH.NAME-ON" : "STAFF-MODE.HOTBAR.VANISH.NAME";
        String loreKey = vanishedNow ? "STAFF-MODE.HOTBAR.VANISH.LORE-ON" : "STAFF-MODE.HOTBAR.VANISH.LORE";
        decorate(stack, nameKey, loreKey);
        player.getInventory().setItem(slot, stack);
    }

    private YamlMenuDefinition staffDefinition(Player player) {
        if (yamlMenus == null) return null;
        if (player != null) return yamlMenus.definition(player, "staff-mode");
        return yamlMenus.definition(yamlMenus.variant(null), "staff-mode");
    }

    private void decorate(ItemStack stack, String nameKey, String loreKey) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(language.raw(nameKey));
        List<String> lore = language.list(loreKey);
        if (lore.isEmpty() && loreKey.endsWith("-ON")) lore = language.list(loreKey.substring(0, loreKey.length() - 3));
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    private boolean staffNametagEnabled() {
        return config.getBoolean("NAMETAG", true) && apollo.available();
    }

    private void applyNametag(Player player, boolean enable) {
        if (enable && !staffNametagEnabled()) return;
        if (enable) {
            String prefix = language.raw("STAFF-MODE.NAMETAG-PREFIX");
            player.setDisplayName(prefix + player.getName());
            player.setPlayerListName(truncate(prefix + player.getName(), 16));
            syncStaffTeamAll(player, truncate(prefix, 16));
            return;
        }
        removeFromTeams(player);
        player.setDisplayName(player.getName());
        restoreListName(player);
    }

    private void syncStaffTeamAll(Player staff, String prefix) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        syncStaffTeam(main, staff, prefix);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null || board.equals(main)) continue;
            syncStaffTeam(board, staff, prefix);
        }
    }

    @SuppressWarnings("deprecation")
    private void syncStaffTeam(Scoreboard board, Player staff, String prefix) {
        if (board == null || staff == null) return;
        Team team = board.getTeam(TEAM_NAME);
        if (team == null) team = board.registerNewTeam(TEAM_NAME);
        if (!prefix.equals(team.getPrefix())) team.setPrefix(prefix);
        if (!team.hasPlayer(staff)) team.addPlayer(staff);
    }

    private void removeFromTeams(Player player) {
        if (player == null) return;
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        removeFromTeam(main, player);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null || board.equals(main)) continue;
            removeFromTeam(board, player);
        }
    }

    @SuppressWarnings("deprecation")
    private static void removeFromTeam(Scoreboard board, Player player) {
        Team team = board.getTeam(TEAM_NAME);
        if (team != null && team.hasPlayer(player)) team.removePlayer(player);
    }

    private void applyFlight(Player player, boolean enable) {
        if (!enable || !config.getBoolean("FLIGHT", true)) return;
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    private void applyNightVision(Player player, boolean enable) {
        if (!config.getBoolean("NIGHT-VISION", true)) return;
        if (enable) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false), true);
            return;
        }
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    private void applyInvulnerability(Player player, boolean enable) {
        UUID id = player.getUniqueId();
        if (enable && config.getBoolean("INVULNERABLE", true)) {
            invulnerable.add(id);
            return;
        }
        invulnerable.remove(id);
    }

    private void applyGlow(Player player, boolean enable) {
        if (apollo.available()) {
            if (enable) {
                apollo.overrideGlow(player.getUniqueId(), Integer.valueOf(0x55FFFF));
                apollo.setNumbersDisabled(player.getUniqueId(), true);
            } else {
                apollo.resetGlow(player.getUniqueId());
                apollo.setNumbersDisabled(player.getUniqueId(), false);
            }
            return;
        }
        if (enable && !config.getBoolean("NIGHT-VISION", true)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false), true);
            player.sendMessage(language.raw("STAFF-MODE.GLOW-FALLBACK"));
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean passable(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        return type == Material.AIR || !type.isSolid();
    }

    private static void restoreListName(Player player) {
        String plain = ChatColor.stripColor(player.getDisplayName());
        if (plain == null || plain.isEmpty()) plain = player.getName();
        player.setPlayerListName(truncate(plain, 16));
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        String cut = value.substring(0, max);
        return cut.endsWith(String.valueOf(ChatColor.COLOR_CHAR)) ? cut.substring(0, max - 1) : cut;
    }
}
