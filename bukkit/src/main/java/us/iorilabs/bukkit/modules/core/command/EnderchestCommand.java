package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.proxy.commands.CommandSource;

public final class EnderchestCommand extends EssentialsCommand {

    public EnderchestCommand(IoriCorePlugin plugin) {
        super(plugin, "enderchest", Arrays.asList("ec", "echest"), "ioricore.essentials.enderchest");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.enderchest")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        Player target = player;
        if (args.length > 0) {
            if (deny(source, "ioricore.essentials.enderchest.others")) return;
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        player.openInventory(target.getEnderChest());
        send(source, "ESSENTIALS.ENDERCHEST.SUCCESS", Map.of("player", target.getName()));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.ENDERCHEST.USAGE");
    }
}
