package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.bukkit.modules.core.vanish.VanishService;
import us.iorilabs.proxy.commands.CommandSource;

public final class VanishCommand extends EssentialsCommand {

    private final VanishService vanish;

    public VanishCommand(IoriCorePlugin plugin, VanishService vanish) {
        super(plugin, "vanish", Collections.singletonList("v"), "ioricore.essentials.vanish");
        this.vanish = vanish;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.vanish")) return;

        Player self = EssentialsPlayers.player(source);
        Player target;
        if (args.length == 0) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.vanish.others")) return;
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        boolean enabled = vanish.toggle(target);
        EssentialsSounds.play(plugin, target, "SUCCESS");
        send(
            source,
            enabled ? "ESSENTIALS.VANISH.ENABLED" : "ESSENTIALS.VANISH.DISABLED",
            Map.of("player", target.getName())
        );
        if (self == null || !self.getUniqueId().equals(target.getUniqueId())) {
            new BukkitCommandSource(target).sendMessage(
                language.raw(target, enabled ? "ESSENTIALS.VANISH.TARGET-ENABLED" : "ESSENTIALS.VANISH.TARGET-DISABLED")
            );
        }
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.VANISH.USAGE");
    }
}
