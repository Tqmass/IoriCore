package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.proxy.commands.CommandSource;

public final class TpCommand extends EssentialsCommand {

    private final EssentialsState state;

    public TpCommand(IoriCorePlugin plugin, EssentialsState state) {
        super(plugin, "tp", Collections.singletonList("teleport"), "ioricore.essentials.tp");
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.tp")) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        if (args.length == 1) {
            Player player = requirePlayer(source);
            if (player == null) return;
            Player target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
            state.rememberBack(player);
            player.teleport(target.getLocation());
            send(source, "ESSENTIALS.TP.SUCCESS", Map.of("player", target.getName()));
            return;
        }

        if (deny(source, "ioricore.essentials.tp.others")) return;
        Player from = EssentialsPlayers.findOnline(source, args[0]);
        Player to = EssentialsPlayers.findOnline(source, args[1]);
        if (from == null) {
            send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
            return;
        }
        if (to == null) {
            send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[1]));
            return;
        }
        state.rememberBack(from);
        from.teleport(to.getLocation());
        send(source, "ESSENTIALS.TP.OTHERS", Map.of("from", from.getName(), "to", to.getName()));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.TP.USAGE");
    }
}
