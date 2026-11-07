package us.iorilabs.bukkit.modules.tab;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import us.iorilabs.abstraction.VersionWrapper;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.lobby.TabNpcVisibility;
import us.iorilabs.bukkit.modules.nametag.NametagModule;
import us.iorilabs.bukkit.modules.nametag.NametagService;
import us.iorilabs.bukkit.util.BoardPlaceholders;
import us.iorilabs.bukkit.util.DisplayYaml;
import us.iorilabs.bukkit.util.PlaceholderApiBridge;
import us.iorilabs.bukkit.util.ProtocolProbe;
import us.iorilabs.proxy.utilities.ProtocolVersions;

public final class TabService {

    private final IoriCorePlugin plugin;
    private final FileConfiguration layout;
    private final int updateTicks;

    @Getter
    private final boolean hideNpcs;

    @Getter
    private final List<String> npcPrefixes;

    private BukkitTask task;

    public TabService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.layout = DisplayYaml.load(plugin, "tab.yml");
        this.updateTicks = Math.max(20, layout.getInt("UPDATE-TICKS", 40));
        this.hideNpcs = layout.getBoolean("HIDE-NPCS", false);
        this.npcPrefixes = layout.getStringList("NPC-NAME-PREFIXES");
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, updateTicks);
        refreshSorting();
        TabNpcVisibility.refresh(plugin, hideNpcs, npcPrefixes);
        for (Player online : Bukkit.getOnlinePlayers()) {
            apply(online);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            clear(online);
        }
    }

    public void apply(Player player) {
        if (player == null || !player.isOnline()) return;

        int protocol = ProtocolProbe.protocol(player, ProtocolProbe.serverFallbackProtocol());
        boolean legacy = ProtocolVersions.isLegacy(protocol);
        String section = DisplayYaml.sectionKey(legacy);

        Map<String, String> placeholders = BoardPlaceholders.of(plugin, player);
        List<String> headerLines = DisplayYaml.resolveLines(layout, section, "HEADER", placeholders, protocol, plugin);
        List<String> footerLines = DisplayYaml.resolveLines(layout, section, "FOOTER", placeholders, protocol, plugin);

        send(
            player,
            PlaceholderApiBridge.set(player, join(headerLines)),
            PlaceholderApiBridge.set(player, join(footerLines))
        );
    }

    public void clear(Player player) {
        if (player == null) return;
        send(player, "", "");
    }

    public void handleJoin(Player player) {
        if (player == null) return;
        Bukkit.getScheduler().runTaskLater(
            plugin,
            () -> {
                refreshSorting();
                TabNpcVisibility.refresh(plugin, hideNpcs, npcPrefixes);
                apply(player);
            },
            5L
        );
        Bukkit.getScheduler().runTaskLater(plugin, () -> apply(player), 40L);
    }

    private void tick() {
        TabNpcVisibility.refresh(plugin, hideNpcs, npcPrefixes);
        for (Player online : Bukkit.getOnlinePlayers()) {
            apply(online);
        }
    }

    private void refreshSorting() {
        if (plugin.modules() == null) return;
        NametagModule module = plugin.modules().get(ModuleId.NAMETAG);
        if (module == null || !module.enabled()) return;
        NametagService service = module.service();
        if (service != null) service.refreshAll();
    }

    private void send(Player player, String header, String footer) {
        VersionWrapper wrapper = plugin.abstraction();
        if (wrapper == null) return;
        wrapper.sendTabHeaderFooter(player.getUniqueId(), header, footer);
    }

    private static String join(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "";
        StringJoiner joiner = new StringJoiner("\n");
        for (String line : lines) {
            joiner.add(line == null ? "" : line);
        }
        return joiner.toString();
    }
}
