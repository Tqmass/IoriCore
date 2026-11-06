package us.iorilabs.bukkit.modules.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.config.CoreFeatureSettings;
import us.iorilabs.bukkit.modules.core.vanish.VanishVisibility;
import us.iorilabs.proxy.commands.CommandSource;

public final class NearCommand extends EssentialsCommand {

    public NearCommand(IoriCorePlugin plugin) {
        super(plugin, "near", Collections.emptyList(), "ioricore.essentials.near");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.near")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        double radius = CoreFeatureSettings.getDouble(plugin.settings(), "NEAR-RADIUS", 100.0D);
        if (args.length >= 1) {
            try {
                radius = Double.parseDouble(args[0]);
            } catch (NumberFormatException ex) {
                send(source, "ESSENTIALS.NEAR.INVALID", Map.of("radius", args[0]));
                return;
            }
        }
        if (radius < 1.0D || radius > 500.0D) {
            send(source, "ESSENTIALS.NEAR.INVALID", Map.of("radius", String.valueOf(radius)));
            return;
        }

        List<String> found = new ArrayList<>();
        for (Player online : player.getWorld().getPlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            if (!VanishVisibility.canSee(plugin, player, online)) continue;
            double distance = online.getLocation().distance(player.getLocation());
            if (distance <= radius) {
                found.add(online.getName() + " (" + (int) distance + "m)");
            }
        }

        if (found.isEmpty()) {
            send(source, "ESSENTIALS.NEAR.EMPTY", Map.of("radius", String.valueOf((int) radius)));
            return;
        }
        send(
            source,
            "ESSENTIALS.NEAR.LIST",
            Map.of("radius", String.valueOf((int) radius), "players", String.join(", ", found))
        );
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.NEAR.USAGE");
    }
}
