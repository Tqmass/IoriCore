package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.proxy.commands.CommandSource;

public final class GodCommand extends EssentialsCommand {

    private final EssentialsState state;

    public GodCommand(IoriCorePlugin plugin, EssentialsState state) {
        super(plugin, "god", Collections.emptyList(), "ioricore.essentials.god");
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.god")) return;

        Player self = EssentialsPlayers.player(source);
        Player target;
        if (args.length == 0) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.god.others")) return;
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        boolean enabled = state.toggleGod(target.getUniqueId());
        EssentialsSounds.play(plugin, target, "SUCCESS");
        send(
            source,
            enabled ? "ESSENTIALS.GOD.ENABLED" : "ESSENTIALS.GOD.DISABLED",
            Map.of("player", target.getName())
        );
        if (self == null || !self.getUniqueId().equals(target.getUniqueId())) {
            new BukkitCommandSource(target).sendMessage(
                language.raw(target, enabled ? "ESSENTIALS.GOD.TARGET-ENABLED" : "ESSENTIALS.GOD.TARGET-DISABLED")
            );
        }
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.GOD.USAGE");
    }
}
