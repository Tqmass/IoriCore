package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.tpa.TpaService;
import us.iorilabs.proxy.commands.CommandSource;

public final class TpaHereCommand extends EssentialsCommand {

    private final TpaService tpa;

    public TpaHereCommand(IoriCorePlugin plugin, TpaService tpa) {
        super(plugin, "tpahere", Collections.emptyList(), "ioricore.essentials.tpahere");
        this.tpa = tpa;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.tpahere")) return;
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
            send(source, "ESSENTIALS.TPA.SELF");
            return;
        }

        tpa.request(player, target, true);
        send(source, "ESSENTIALS.TPAHERE.SENT", Map.of("player", target.getName()));
        new BukkitCommandSource(target).sendMessage(
            language.raw(target, "ESSENTIALS.TPAHERE.RECEIVED", Map.of("player", player.getName()))
        );
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.TPAHERE.USAGE");
    }
}
