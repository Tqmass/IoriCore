package us.iorilabs.bukkit.modules.customjoins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.fx.SoundService;
import us.iorilabs.bukkit.util.BoardPlaceholders;
import us.iorilabs.bukkit.util.ColorUtil;
import us.iorilabs.bukkit.util.DisplayYaml;

@RequiredArgsConstructor
public final class CustomJoinsService {

    private final IoriCorePlugin plugin;
    private final CustomJoinsConfig config;
    private final CustomJoinsMotdService motd;
    private final SoundService sounds;

    public void sendMotd(Player player) {
        motd.send(player);
    }

    public CustomJoinsEntry matchJoin(Player player) {
        return match(player, true);
    }

    public CustomJoinsEntry matchLeave(Player player) {
        return match(player, false);
    }

    public void broadcastJoin(Player player, CustomJoinsEntry entry) {
        if (player == null || entry == null || !entry.joinEnabled()) return;
        broadcast(player, entry.joinMessage(), entry.joinSound());
    }

    public void broadcastLeave(Player player, CustomJoinsEntry entry) {
        if (player == null || entry == null || !entry.leaveEnabled()) return;
        broadcast(player, entry.leaveMessage(), entry.leaveSound());
    }

    private CustomJoinsEntry match(Player player, boolean join) {
        if (player == null) return null;
        List<CustomJoinsEntry> entries = config.entries();
        for (CustomJoinsEntry entry : entries) {
            if (join && !entry.joinEnabled()) continue;
            if (!join && !entry.leaveEnabled()) continue;
            if (entry.permission().isEmpty() || player.hasPermission(entry.permission())) return entry;
        }
        return null;
    }

    private void broadcast(Player player, String template, String soundKey) {
        if (template == null || template.trim().isEmpty()) return;

        Map<String, String> placeholders = new HashMap<>(BoardPlaceholders.of(plugin, player));
        placeholders.put("id", player.getName());
        placeholders.put("player", player.getName());

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(formatLine(online, template, placeholders));
        }
        if (soundKey != null && !soundKey.isEmpty()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                sounds.play(online, soundKey);
            }
        }
    }

    private String formatLine(Player viewer, String template, Map<String, String> placeholders) {
        String prefix = plugin.language().raw(viewer, "PREFIX");
        Map<String, String> merged = new HashMap<>(placeholders);
        merged.put("prefix", prefix);
        merged.put("PREFIX", prefix);
        String value = DisplayYaml.substitute(template, merged).replace("<prefix>", prefix).replace("<PREFIX>", prefix);
        int protocol = viewer == null ? Integer.MAX_VALUE : plugin.language().protocolOf(viewer);
        return ColorUtil.translate(value, protocol);
    }
}
