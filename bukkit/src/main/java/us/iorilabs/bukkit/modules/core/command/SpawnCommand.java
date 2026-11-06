package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.bukkit.modules.core.spawn.SpawnService;
import us.iorilabs.proxy.commands.CommandSource;

public final class SpawnCommand extends EssentialsCommand {

    private final SpawnService spawn;
    private final EssentialsState state;

    public SpawnCommand(IoriCorePlugin plugin, SpawnService spawn, EssentialsState state) {
        super(plugin, "spawn", Collections.emptyList(), "ioricore.essentials.spawn");
        this.spawn = spawn;
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.spawn")) return;

        Player target;
        if (args.length == 0) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.spawn.others")) return;
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        if (spawn.spawn() == null) {
            send(source, "ESSENTIALS.SPAWN.NOT-SET");
            return;
        }

        state.rememberBack(target);
        spawn.teleport(target);
        EssentialsSounds.play(plugin, target, "TELEPORT");
        send(source, "ESSENTIALS.SPAWN.SUCCESS", Map.of("player", target.getName()));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.SPAWN.USAGE");
    }
}
