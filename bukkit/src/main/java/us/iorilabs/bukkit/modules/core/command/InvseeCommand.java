package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.proxy.commands.CommandSource;

public final class InvseeCommand extends EssentialsCommand {

    public InvseeCommand(IoriCorePlugin plugin) {
        super(plugin, "invsee", Collections.emptyList(), "ioricore.essentials.invsee");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.invsee")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (args.length < 1) {
            sendUsage(source);
            return;
        }

        Player target = EssentialsPlayers.findOnline(source, args[0]);
        if (target == null) {
            send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            send(source, "ESSENTIALS.INVSEE.SELF");
            return;
        }

        player.openInventory(target.getInventory());
        send(source, "ESSENTIALS.INVSEE.SUCCESS", Map.of("player", target.getName()));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.INVSEE.USAGE");
    }
}
