package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.proxy.commands.CommandSource;

public final class FlyCommand extends EssentialsCommand {

    public FlyCommand(IoriCorePlugin plugin) {
        super(plugin, "fly", Collections.emptyList(), "ioricore.essentials.fly");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.fly")) return;

        Player self = EssentialsPlayers.player(source);
        Player target;
        if (args.length == 0) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.fly.others")) return;
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        boolean enable = !target.getAllowFlight();
        target.setAllowFlight(enable);
        if (!enable) target.setFlying(false);
        EssentialsSounds.play(plugin, target, "SUCCESS");
        String key = enable ? "ESSENTIALS.FLY.ENABLED" : "ESSENTIALS.FLY.DISABLED";
        send(source, key, Map.of("player", target.getName()));
        if (self == null || !self.getUniqueId().equals(target.getUniqueId())) {
            new BukkitCommandSource(target).sendMessage(
                language.raw(target, enable ? "ESSENTIALS.FLY.TARGET-ENABLED" : "ESSENTIALS.FLY.TARGET-DISABLED")
            );
        }
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.FLY.USAGE");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        return super.suggest(source, label, args);
    }
}
