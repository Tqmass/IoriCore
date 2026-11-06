package us.iorilabs.bukkit.modules.nametag;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.staffmode.StaffModeModule;
import us.iorilabs.bukkit.modules.staffmode.StaffModeService;
import us.iorilabs.bukkit.ranks.RankDisplay;
import us.iorilabs.bukkit.ranks.RankService;
import us.iorilabs.bukkit.ranks.RanksModule;
import us.iorilabs.bukkit.util.ProtocolProbe;
import us.iorilabs.proxy.utilities.ProtocolVersions;

@RequiredArgsConstructor
public final class NametagService {

    private static final String TEAM_PREFIX = "icR";

    private final IoriCorePlugin plugin;

    public void refresh(Player player) {
        if (player == null || !player.isOnline()) return;
        if (isStaffTagged(player)) return;

        RankDisplay display = displayOf(player);
        String prefix = display == null ? "" : colored(display.prefix());
        String suffix = display == null ? "" : colored(display.suffix());
        int limit = limitFor(player);
        prefix = truncate(prefix, limit);
        suffix = truncate(suffix, limit);
        String teamName = teamName(display);

        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        apply(main, player, teamName, prefix, suffix);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null || board.equals(main)) continue;
            apply(board, player, teamName, prefix, suffix);
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    public void syncBoard(Scoreboard board) {
        if (board == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isStaffTagged(player)) continue;
            RankDisplay display = displayOf(player);
            String prefix = display == null ? "" : colored(display.prefix());
            String suffix = display == null ? "" : colored(display.suffix());
            int limit = limitFor(player);
            apply(board, player, teamName(display), truncate(prefix, limit), truncate(suffix, limit));
        }
    }

    public void clear(Player player) {
        if (player == null) return;
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        remove(main, player);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null || board.equals(main)) continue;
            remove(board, player);
        }
    }

    private RankDisplay displayOf(Player player) {
        if (plugin.modules() == null) return null;
        CoreModule module = plugin.modules().get(ModuleId.RANKS);
        if (!(module instanceof RanksModule) || !module.enabled()) return null;
        RankService ranks = ((RanksModule) module).ranks();
        return ranks == null ? null : ranks.display(player);
    }

    private boolean isStaffTagged(Player player) {
        if (plugin.modules() == null) return false;
        CoreModule module = plugin.modules().get(ModuleId.STAFF_MODE);
        if (!(module instanceof StaffModeModule) || !module.enabled()) return false;
        StaffModeService service = ((StaffModeModule) module).service();
        if (service == null || !service.enabled(player.getUniqueId())) return false;

        Scoreboard board = player.getScoreboard();
        if (board == null) board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(StaffModeService.TEAM_NAME);
        return team != null && team.hasPlayer(player);
    }

    private int limitFor(Player player) {
        int protocol = ProtocolProbe.protocol(player, plugin.fallbackProtocol());
        return ProtocolVersions.isLegacy(protocol) ? 16 : 64;
    }

    @SuppressWarnings("deprecation")
    private void apply(Scoreboard board, Player player, String teamName, String prefix, String suffix) {
        if (board == null || player == null) return;
        remove(board, player);
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        if (!prefix.equals(team.getPrefix())) team.setPrefix(prefix);
        try {
            if (!suffix.equals(team.getSuffix())) team.setSuffix(suffix);
        } catch (NoSuchMethodError ignored) {}
        if (!team.hasPlayer(player)) team.addPlayer(player);
    }

    @SuppressWarnings("deprecation")
    private static void remove(Scoreboard board, Player player) {
        if (board == null || player == null) return;
        for (Team team : board.getTeams()) {
            if (team == null || !team.getName().startsWith(TEAM_PREFIX)) continue;
            if (team.hasPlayer(player)) team.removePlayer(player);
        }
    }

    private static String teamName(RankDisplay display) {
        int weight = display == null ? 0 : Math.max(0, Math.min(9999, display.weight()));
        String key = display == null || display.key() == null ? "def" : display.key();
        String shortKey = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (shortKey.length() > 4) shortKey = shortKey.substring(0, 4);
        if (shortKey.isEmpty()) shortKey = "def";
        String inverted = String.format(Locale.ROOT, "%04d", 9999 - weight);
        String name = TEAM_PREFIX + inverted + shortKey;
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    private static String colored(String input) {
        if (input == null || input.isEmpty()) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }
}
