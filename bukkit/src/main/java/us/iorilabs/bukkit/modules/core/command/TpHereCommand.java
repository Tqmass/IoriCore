package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.proxy.commands.CommandSource;

public final class TpHereCommand extends EssentialsCommand {

    private final EssentialsState state;

    public TpHereCommand(IoriCorePlugin plugin, EssentialsState state) {
        super(plugin, "tphere", Collections.emptyList(), "ioricore.essentials.tphere");
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.tphere")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        Player target = EssentialsPlayers.findOnline(source, args[0]);
        if (target == null) {
            send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
            return;
        }

        state.rememberBack(target);
        target.teleport(player.getLocation());
        send(source, "ESSENTIALS.TPHERE.SUCCESS", Map.of("player", target.getName()));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.TPHERE.USAGE");
    }
}
