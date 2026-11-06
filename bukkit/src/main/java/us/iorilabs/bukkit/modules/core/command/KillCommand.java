package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.proxy.commands.CommandSource;

public final class KillCommand extends EssentialsCommand {

    public KillCommand(IoriCorePlugin plugin) {
        super(plugin, "kill", Collections.singletonList("suicide"), "ioricore.essentials.kill");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.kill")) return;

        Player target;
        if (args.length == 0) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.kill.others")) return;
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        target.setHealth(0);
        send(source, "ESSENTIALS.KILL.SUCCESS", Map.of("player", target.getName()));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.KILL.USAGE");
    }
}
