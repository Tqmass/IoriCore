package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.ignore.IgnoreService;
import us.iorilabs.proxy.commands.CommandSource;

public final class IgnoreCommand extends EssentialsCommand {

    private final IgnoreService ignore;

    public IgnoreCommand(IoriCorePlugin plugin, IgnoreService ignore) {
        super(plugin, "ignore", Collections.emptyList(), "ioricore.essentials.ignore");
        this.ignore = ignore;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.ignore")) return;
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
        if (target.getUniqueId().equals(player.getUniqueId())) {
            send(source, "ESSENTIALS.IGNORE.SELF");
            return;
        }

        boolean nowIgnored = ignore.toggle(player.getUniqueId(), target.getUniqueId());
        send(
            source,
            nowIgnored ? "ESSENTIALS.IGNORE.ADDED" : "ESSENTIALS.IGNORE.REMOVED",
            Map.of("player", target.getName())
        );
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.IGNORE.USAGE");
    }
}
