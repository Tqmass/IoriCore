package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.proxy.commands.CommandSource;

public final class SpeedCommand extends EssentialsCommand {

    public SpeedCommand(IoriCorePlugin plugin) {
        super(plugin, "speed", Collections.emptyList(), "ioricore.essentials.speed");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.speed")) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        float speed;
        try {
            speed = Float.parseFloat(args[0]);
        } catch (NumberFormatException ex) {
            send(source, "ESSENTIALS.SPEED.INVALID", Map.of("speed", args[0]));
            return;
        }
        if (speed < 0f || speed > 10f) {
            send(source, "ESSENTIALS.SPEED.INVALID", Map.of("speed", args[0]));
            return;
        }

        Player target;
        if (args.length == 1) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.speed.others")) return;
            target = EssentialsPlayers.findOnline(source, args[1]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[1]));
                return;
            }
        }

        float bukkit = Math.max(0.1f, Math.min(1f, speed / 10f));
        if (target.isFlying()) target.setFlySpeed(bukkit);
        else target.setWalkSpeed(bukkit);
        send(
            source,
            "ESSENTIALS.SPEED.SUCCESS",
            Map.of("player", target.getName(), "speed", String.valueOf((int) speed))
        );
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.SPEED.USAGE");
    }
}
