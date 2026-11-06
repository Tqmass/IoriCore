package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.proxy.commands.CommandSource;

public final class PingCommand extends EssentialsCommand {

    public PingCommand(IoriCorePlugin plugin) {
        super(plugin, "ping", Collections.emptyList(), "ioricore.essentials.ping");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.ping")) return;

        Player target;
        if (args.length == 0) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.ping.others")) return;
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        send(
            source,
            "ESSENTIALS.PING.SUCCESS",
            Map.of("player", target.getName(), "ping", String.valueOf(safePing(target)))
        );
    }

    private static int safePing(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object ping = handle.getClass().getField("ping").get(handle);
            if (ping instanceof Integer) return (Integer) ping;
        } catch (ReflectiveOperationException ignored) {}
        try {
            Object ping = player.getClass().getMethod("getPing").invoke(player);
            if (ping instanceof Integer) return (Integer) ping;
        } catch (ReflectiveOperationException ignored) {}
        return 0;
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.PING.USAGE");
    }
}
