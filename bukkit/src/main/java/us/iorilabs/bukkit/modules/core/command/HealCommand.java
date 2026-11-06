package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.proxy.commands.CommandSource;

public final class HealCommand extends EssentialsCommand {

    public HealCommand(IoriCorePlugin plugin) {
        super(plugin, "heal", Collections.emptyList(), "ioricore.essentials.heal");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.heal")) return;

        Player self = EssentialsPlayers.player(source);
        Player target;
        if (args.length == 0) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.heal.others")) return;
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        target.setHealth(target.getMaxHealth());
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setFireTicks(0);
        EssentialsSounds.play(plugin, target, "SUCCESS");
        send(source, "ESSENTIALS.HEAL.SUCCESS", Map.of("player", target.getName()));
        if (self == null || !self.getUniqueId().equals(target.getUniqueId())) {
            new BukkitCommandSource(target).sendMessage(language.raw(target, "ESSENTIALS.HEAL.TARGET"));
        }
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.HEAL.USAGE");
    }
}
