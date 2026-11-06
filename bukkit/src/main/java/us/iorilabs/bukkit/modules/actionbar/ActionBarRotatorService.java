package us.iorilabs.bukkit.modules.actionbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.fx.ActionBarService;
import us.iorilabs.bukkit.util.BoardPlaceholders;

public final class ActionBarRotatorService {

    private final IoriCorePlugin plugin;
    private final ActionBarService actionBars;
    private final int intervalTicks;
    private final List<String> messages;
    private int index;
    private BukkitTask task;

    public ActionBarRotatorService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.actionBars = plugin.actionBars();
        int seconds = Math.max(1, plugin.settings().getInt("ACTION-BAR.INTERVAL", 8));
        this.intervalTicks = seconds * 20;
        List<String> configured = plugin.settings().getStringList("ACTION-BAR.MESSAGES");
        if (configured == null || configured.isEmpty()) {
            this.messages = Collections.emptyList();
        } else {
            this.messages = Collections.unmodifiableList(new ArrayList<>(configured));
        }
    }

    public void start() {
        if (messages.isEmpty()) return;
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private String formatLine(Player viewer, String template, Map<String, String> placeholders) {
        if (template.contains("&") || template.contains("<")) {
            String line = template;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                line = line.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return ChatColor.translateAlternateColorCodes('&', line);
        }
        return plugin.language().raw(viewer, template, placeholders);
    }

    private void tick() {
        if (messages.isEmpty()) return;
        String template = messages.get(index);
        index = (index + 1) % messages.size();
        if (template == null || template.trim().isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) continue;
            Map<String, String> placeholders = BoardPlaceholders.of(plugin, player);
            actionBars.send(player, formatLine(player, template, placeholders));
        }
    }
}
