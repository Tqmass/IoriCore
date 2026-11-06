package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.proxy.commands.CommandSource;

public final class GetPosCommand extends EssentialsCommand {

    public GetPosCommand(IoriCorePlugin plugin) {
        super(plugin, "getpos", Arrays.asList("coords", "position"), "ioricore.essentials.getpos");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.getpos")) return;

        Player target;
        if (args.length == 0) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.getpos.others")) return;
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        Location loc = target.getLocation();
        String world = loc.getWorld() == null ? "?" : loc.getWorld().getName();
        send(
            source,
            "ESSENTIALS.GETPOS.SUCCESS",
            Map.of(
                "player",
                target.getName(),
                "world",
                world,
                "x",
                String.format("%.1f", loc.getX()),
                "y",
                String.format("%.1f", loc.getY()),
                "z",
                String.format("%.1f", loc.getZ()),
                "yaw",
                String.format("%.1f", loc.getYaw()),
                "pitch",
                String.format("%.1f", loc.getPitch())
            )
        );
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.GETPOS.USAGE");
    }
}
