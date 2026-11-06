package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.proxy.commands.CommandSource;

public final class ClearCommand extends EssentialsCommand {

    public ClearCommand(IoriCorePlugin plugin) {
        super(plugin, "clear", Arrays.asList("ci", "clearinventory"), "ioricore.essentials.clear");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.clear")) return;

        Player self = EssentialsPlayers.player(source);
        Player target;
        if (args.length == 0) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.clear.others")) return;
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        target.getInventory().clear();
        target.getInventory().setArmorContents(null);
        EssentialsSounds.play(plugin, target, "SUCCESS");
        send(source, "ESSENTIALS.CLEAR.SUCCESS", Map.of("player", target.getName()));
        if (self == null || !self.getUniqueId().equals(target.getUniqueId())) {
            new BukkitCommandSource(target).sendMessage(language.raw(target, "ESSENTIALS.CLEAR.TARGET"));
        }
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.CLEAR.USAGE");
    }
}
