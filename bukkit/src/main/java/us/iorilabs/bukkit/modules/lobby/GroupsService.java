package us.iorilabs.bukkit.modules.lobby;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import us.iorilabs.api.hook.ApolloHook;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.staffmode.StaffModeModule;
import us.iorilabs.bukkit.modules.staffmode.StaffModeService;
import us.iorilabs.bukkit.ranks.RankDisplay;
import us.iorilabs.bukkit.util.DisplayYaml;

public final class GroupsService {

    public static final String DEFAULT = "__DEFAULT__";
    private static final String ROOT = "GROUPS";

    private final IoriCorePlugin plugin;
    private final List<String> npcPrefixes;
    private FileConfiguration config;
    private List<String> ordered = List.of();
    private BukkitTask task;

    public GroupsService(IoriCorePlugin plugin, List<String> npcPrefixes) {
        this.plugin = plugin;
        this.npcPrefixes = npcPrefixes == null ? List.of() : npcPrefixes;
        reload();
    }

    public void reload() {
        config = DisplayYaml.load(plugin, "groups.yml");
        ordered = buildOrder();
    }

    public boolean enabled() {
        return config.getBoolean(ROOT + ".ENABLED", false);
    }

    public int updateSeconds() {
        return Math.max(1, config.getInt(ROOT + ".UPDATE-SECONDS", 5));
    }

    public boolean apolloEnabled() {
        return config.getBoolean(ROOT + ".APOLLO.ENABLED", true);
    }

    public void start() {
        stop();
        if (!enabled()) return;
        long ticks = updateSeconds() * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshApollo, ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public RankDisplay display(Player player) {
        if (player == null || !enabled()) return null;
        String group = resolveGroup(player);
        String prefix = text(group, "TAB-PREFIX", "TAG-PREFIX");
        String suffix = text(group, "TAB-SUFFIX", "TAG-SUFFIX");
        int priority = config.getInt(rankPath(group) + ".PRIORITY", 99);
        int weight = Math.max(0, 9999 - priority);
        return new RankDisplay(group, group, group, prefix, suffix, "&7", weight);
    }

    public String resolveGroup(Player player) {
        if (player == null) return DEFAULT;
        String mode = config.getString(ROOT + ".RESOLVE-BY", "AUTO").toUpperCase(Locale.ROOT);
        if (!"PERMISSION".equals(mode)) {
            String vault = matchVaultPrimary(player);
            if (vault != null) return vault;
            if ("VAULT".equals(mode)) return DEFAULT;
        }
        for (String group : ordered) {
            String perm = config.getString(rankPath(group) + ".PERMISSION", "");
            if (!perm.isEmpty() && player.hasPermission(perm)) return group;
        }
        return DEFAULT;
    }

    public void refreshApollo() {
        if (!enabled() || !apolloEnabled()) return;
        ApolloHook apollo = plugin.apolloHook();
        if (apollo == null || !apollo.available()) return;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (TabNpcNames.isNpc(online, npcPrefixes)) continue;
            applyGroupGlow(online, apollo);
        }
    }

    public void applyGroupGlow(Player player, ApolloHook apollo) {
        if (player == null || apollo == null) return;
        if (staffModeActive(player)) return;
        String group = resolveGroup(player);
        boolean on = config.getBoolean(rankPath(group) + ".GLOW.ENABLED", false);
        if (!on) {
            apollo.resetGlow(player.getUniqueId());
            return;
        }
        String colorName = config.getString(rankPath(group) + ".GLOW.COLOR", "WHITE");
        java.awt.Color color = LobbyGlowColor.parseNamed(colorName);
        if (color == null) {
            apollo.resetGlow(player.getUniqueId());
            return;
        }
        apollo.overrideGlow(player.getUniqueId(), color.getRGB() & 0xFFFFFF);
    }

    private boolean staffModeActive(Player player) {
        if (plugin.modules() == null || player == null) return false;
        CoreModule module = plugin.modules().get(ModuleId.STAFF_MODE);
        if (!(module instanceof StaffModeModule staff) || !staff.enabled()) return false;
        StaffModeService service = staff.service();
        return service != null && service.enabled(player.getUniqueId());
    }

    private String matchVaultPrimary(Player player) {
        try {
            org.bukkit.plugin.RegisteredServiceProvider<?> vault = Bukkit.getServicesManager().getRegistration(
                Class.forName("net.milkbowl.vault.permission.Permission")
            );
            if (vault == null) return null;
            Object perm = vault.getProvider();
            if (perm == null) return null;
            String primary = (String) perm.getClass().getMethod("getPrimaryGroup", Player.class).invoke(perm, player);
            if (primary == null || primary.isEmpty()) return null;
            for (String group : ordered) {
                if (group.equalsIgnoreCase(primary)) return group;
            }
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }

    private List<String> buildOrder() {
        ConfigurationSection section = config.getConfigurationSection(ROOT + ".RANKS");
        if (section == null) return List.of(DEFAULT);
        List<String> groups = new ArrayList<>(section.getKeys(false));
        groups.sort(Comparator.comparingInt(g -> config.getInt(rankPath(g) + ".PRIORITY", 99)));
        return Collections.unmodifiableList(groups);
    }

    private String rankPath(String group) {
        return ROOT + ".RANKS." + group;
    }

    private String text(String group, String first, String second) {
        String path = rankPath(group) + ".";
        if (config.contains(path + first)) return colored(config.getString(path + first, ""));
        if (config.contains(path + second)) return colored(config.getString(path + second, ""));
        return "";
    }

    private static String colored(String raw) {
        if (raw == null) return "";
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
