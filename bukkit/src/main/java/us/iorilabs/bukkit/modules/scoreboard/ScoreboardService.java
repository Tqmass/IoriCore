package us.iorilabs.bukkit.modules.scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import us.iorilabs.api.hook.ApolloHook;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.nametag.NametagModule;
import us.iorilabs.bukkit.modules.nametag.NametagService;
import us.iorilabs.bukkit.modules.settings.PlayerSettings;
import us.iorilabs.bukkit.modules.settings.PlayerSettingsModule;
import us.iorilabs.bukkit.modules.settings.PlayerSettingsStore;
import us.iorilabs.bukkit.modules.staffmode.StaffModeModule;
import us.iorilabs.bukkit.modules.staffmode.StaffModeService;
import us.iorilabs.bukkit.util.BoardPlaceholders;
import us.iorilabs.bukkit.util.DisplayYaml;
import us.iorilabs.bukkit.util.PlaceholderApiBridge;
import us.iorilabs.bukkit.util.ProtocolProbe;
import us.iorilabs.proxy.utilities.ProtocolVersions;

public final class ScoreboardService {

    private static final String OBJECTIVE = "ioricore";
    private static final String TEAM_PREFIX = "icSb";
    private static final int MAX_LINES = 15;
    private static final int MODERN_TEAM_PROTOCOL = 393;
    private static final int HEX_LENGTH = 14;

    private final IoriCorePlugin plugin;
    private final FileConfiguration layout;
    private final int updateTicks;
    private final boolean disableLunarNumbers;
    private final String defaultStyle;
    private final List<String> styleIds;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, List<String>> rendered = new HashMap<>();
    private final Map<UUID, String> titles = new HashMap<>();
    private BukkitTask task;
    private BukkitTask animTask;
    private long animationTicks;
    private final boolean animated;
    private final long refreshPeriod;

    public ScoreboardService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.layout = DisplayYaml.load(plugin, "scoreboard.yml");
        this.updateTicks = Math.max(20, layout.getInt("UPDATE-TICKS", 40));
        this.disableLunarNumbers = layout.getBoolean("DISABLE-LUNAR-NUMBERS", true);
        String defaultRaw = layout.getString("DEFAULT-SCOREBOARD-STYLE", layout.getString("DEFAULT-STYLE", ""));
        this.defaultStyle = normalizeStyle(defaultRaw);
        this.styleIds = collectStyleIds(layout, defaultStyle);
        this.animated =
            DisplayYaml.hasGlobalAnimations(layout) ||
            sectionAnimated(layout, "MODERN") ||
            sectionAnimated(layout, "LEGACY");
        this.refreshPeriod = animated ? 1L : updateTicks;
    }

    public void start() {
        stop();
        animationTicks = 0L;
        animTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> animationTicks++, 1L, 1L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, refreshPeriod);
        for (Player online : Bukkit.getOnlinePlayers()) {
            apply(online);
        }
    }

    public void stop() {
        if (animTask != null) {
            animTask.cancel();
            animTask = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            clear(online);
        }
        boards.clear();
        rendered.clear();
        titles.clear();
        animationTicks = 0L;
    }

    public void apply(Player player) {
        if (player == null) return;
        if (!prefEnabled(player.getUniqueId())) {
            clear(player);
            return;
        }

        UUID id = player.getUniqueId();
        Scoreboard board = boards.get(id);
        boolean created = board == null;
        if (created) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            boards.put(id, board);
            syncTeams(board);
        }
        render(player, board);
        if (player.getScoreboard() != board) player.setScoreboard(board);
        applyLunar(player);
    }

    public void clear(Player player) {
        if (player == null) return;
        forget(player.getUniqueId());
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        if (main != null) player.setScoreboard(main);
    }

    public boolean toggle(Player player) {
        if (player == null) return false;
        PlayerSettingsStore store = settingsStore();
        if (store == null) {
            boolean next = !boards.containsKey(player.getUniqueId());
            if (next) apply(player);
            else clear(player);
            return next;
        }
        PlayerSettings settings = store.get(player.getUniqueId());
        boolean next = !settings.scoreboard();
        settings.scoreboard(next);
        store.save(settings);
        if (next) apply(player);
        else clear(player);
        return next;
    }

    public boolean setStyle(Player player, String style) {
        if (player == null) return false;
        String normalized = normalizeStyle(style);
        if (!styleIds.contains(normalized)) return false;
        PlayerSettingsStore store = settingsStore();
        if (store != null) {
            PlayerSettings settings = store.get(player.getUniqueId());
            settings.scoreboardStyle(normalized);
            settings.scoreboard(true);
            store.save(settings);
        }
        rendered.remove(player.getUniqueId());
        titles.remove(player.getUniqueId());
        apply(player);
        return true;
    }

    public List<String> styles() {
        return new ArrayList<>(styleIds);
    }

    public String styleOf(Player player) {
        if (player == null) return defaultStyle;
        PlayerSettingsStore store = settingsStore();
        if (store == null) return defaultStyle;
        String style = store.get(player.getUniqueId()).scoreboardStyle();
        String normalized = normalizeStyle(style);
        return styleIds.contains(normalized) ? normalized : defaultStyle;
    }

    public void handleJoin(Player player) {
        if (player == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> apply(player), 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> apply(player), 40L);
    }

    public void handleQuit(Player player) {
        if (player == null) return;
        forget(player.getUniqueId());
    }

    private void tick() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!prefEnabled(online.getUniqueId())) {
                if (boards.containsKey(online.getUniqueId())) clear(online);
                continue;
            }
            apply(online);
        }
    }

    private void render(Player player, Scoreboard board) {
        int protocol = ProtocolProbe.protocol(player, ProtocolProbe.serverFallbackProtocol());
        boolean legacy = ProtocolVersions.isLegacy(protocol);
        boolean wide =
            protocol >= MODERN_TEAM_PROTOCOL && ProtocolProbe.serverFallbackProtocol() >= MODERN_TEAM_PROTOCOL;
        int partLimit = wide ? 64 : 16;
        String section = DisplayYaml.sectionKey(legacy);
        String style = styleOf(player);

        Map<String, String> placeholders = BoardPlaceholders.of(plugin, player);
        String title = DisplayYaml.resolveTitle(layout, section, style, animationTicks, placeholders, protocol, plugin);
        title = PlaceholderApiBridge.set(player, title);
        title = safeCut(title, wide ? 128 : 32);

        Objective objective = board.getObjective(OBJECTIVE);
        if (objective == null) {
            objective = board.registerNewObjective(OBJECTIVE, "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        UUID id = player.getUniqueId();
        if (!title.equals(titles.get(id))) {
            objective.setDisplayName(title);
            titles.put(id, title);
        }

        List<String> lines = DisplayYaml.resolveScoreboardLines(
            layout,
            section,
            style,
            animationTicks,
            placeholders,
            protocol,
            plugin
        );
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            lines.set(i, PlaceholderApiBridge.set(player, line));
        }
        String vaultPrefix = placeholders.getOrDefault("vault_prefix", "");
        int max = Math.min(lines.size(), MAX_LINES);
        List<String> previous = rendered.computeIfAbsent(id, key -> new ArrayList<>());
        boolean resized = previous.size() != max;

        for (int i = 0; i < max; i++) {
            String line = lines.get(i) == null ? "" : lines.get(i).replace("%vault_prefix%", vaultPrefix);
            String colored = line;
            Team team = board.getTeam(TEAM_PREFIX + i);
            if (team == null) {
                team = board.registerNewTeam(TEAM_PREFIX + i);
                team.addEntry(entry(i));
                resized = true;
            }
            if (i >= previous.size() || !colored.equals(previous.get(i)) || resized) {
                applyLine(team, colored, partLimit);
            }
            if (resized) objective.getScore(entry(i)).setScore(max - i);
            if (i < previous.size()) previous.set(i, colored);
            else previous.add(colored);
        }

        for (int i = max; i < previous.size(); i++) {
            board.resetScores(entry(i));
            Team team = board.getTeam(TEAM_PREFIX + i);
            if (team != null) team.unregister();
        }
        while (previous.size() > max) previous.remove(previous.size() - 1);
    }

    private void forget(UUID id) {
        boards.remove(id);
        rendered.remove(id);
        titles.remove(id);
    }

    private void syncTeams(Scoreboard board) {
        syncStaffTeam(board);
        syncNametags(board);
    }

    private void syncStaffTeam(Scoreboard board) {
        if (plugin.modules() == null) return;
        StaffModeModule module = plugin.modules().get(ModuleId.STAFF_MODE);
        if (module == null || !module.enabled()) return;
        StaffModeService service = module.service();
        if (service != null) service.syncBoard(board);
    }

    private void syncNametags(Scoreboard board) {
        if (plugin.modules() == null) return;
        NametagModule module = plugin.modules().get(ModuleId.NAMETAG);
        if (module == null || !module.enabled()) return;
        NametagService service = module.service();
        if (service != null) service.syncBoard(board);
    }

    private static String entry(int index) {
        return ChatColor.values()[index].toString() + ChatColor.RESET;
    }

    private static void applyLine(Team team, String colored, int limit) {
        String prefix = safeCut(colored, limit);
        String rest = colored.substring(prefix.length());
        String suffix = rest.isEmpty() ? "" : safeCut(ChatColor.getLastColors(prefix) + rest, limit);
        if (!prefix.equals(team.getPrefix())) team.setPrefix(prefix);
        if (!suffix.equals(team.getSuffix())) team.setSuffix(suffix);
    }

    private static String safeCut(String value, int limit) {
        if (value == null) return "";
        if (value.length() <= limit) return value;
        int cut = limit;
        if (value.charAt(cut - 1) == ChatColor.COLOR_CHAR) cut--;
        int hex = value.lastIndexOf(ChatColor.COLOR_CHAR + "x", cut - 1);
        if (hex >= 0 && hex + HEX_LENGTH > cut) cut = hex;
        return value.substring(0, Math.max(0, cut));
    }

    private boolean prefEnabled(UUID uuid) {
        PlayerSettingsStore store = settingsStore();
        if (store == null) return true;
        return store.get(uuid).scoreboard();
    }

    private PlayerSettingsStore settingsStore() {
        if (plugin.modules() == null) return null;
        PlayerSettingsModule module = plugin.modules().get(ModuleId.PLAYER_SETTINGS);
        if (module == null || !module.enabled()) return null;
        return module.store();
    }

    private void applyLunar(Player player) {
        if (!disableLunarNumbers) return;
        ApolloHook apollo = plugin.apolloHook();
        if (apollo == null || !apollo.available()) return;
        apollo.setNumbersDisabled(player.getUniqueId(), true);
    }

    private static String normalizeStyle(String style) {
        if (style == null || style.trim().isEmpty()) return "lobby";
        return style.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
    }

    private static boolean sectionAnimated(FileConfiguration config, String sectionKey) {
        if (config.getBoolean(sectionKey + ".TITLE-ANIMATED.ENABLED", false)) return true;
        return config.getBoolean(sectionKey + ".FOOTER-ANIMATED.ENABLED", false);
    }

    private static List<String> collectStyleIds(FileConfiguration layout, String defaultStyle) {
        List<String> ids = new ArrayList<>();
        ConfigurationSection styles = layout.getConfigurationSection("STYLES");
        if (styles != null) {
            for (String key : styles.getKeys(false)) {
                String normalized = normalizeStyle(key);
                if (!normalized.isEmpty() && !ids.contains(normalized)) ids.add(normalized);
            }
        }
        if (ids.isEmpty()) {
            List<String> configured = layout.getStringList("STYLE-IDS");
            if (configured != null) {
                for (String id : configured) {
                    String normalized = normalizeStyle(id);
                    if (!normalized.isEmpty() && !ids.contains(normalized)) ids.add(normalized);
                }
            }
        }
        if (ids.isEmpty() && defaultStyle != null && !defaultStyle.isEmpty()) ids.add(defaultStyle);
        return ids;
    }
}
