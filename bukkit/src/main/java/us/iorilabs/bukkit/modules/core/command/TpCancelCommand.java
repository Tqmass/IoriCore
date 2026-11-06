package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.tpa.TpaRequest;
import us.iorilabs.bukkit.modules.core.tpa.TpaService;
import us.iorilabs.proxy.commands.CommandSource;

public final class TpCancelCommand extends EssentialsCommand {

    private final TpaService tpa;

    public TpCancelCommand(IoriCorePlugin plugin, TpaService tpa) {
        super(plugin, "tpcancel", Arrays.asList("tpacancel", "tpc"), "ioricore.essentials.tpcancel");
        this.tpa = tpa;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.tpcancel")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        TpaRequest cancelled = tpa.cancelOutgoing(player.getUniqueId());
        if (cancelled == null) {
            send(source, "ESSENTIALS.TPCANCEL.NONE");
            return;
        }

        send(source, "ESSENTIALS.TPCANCEL.SUCCESS");
        Player target = org.bukkit.Bukkit.getPlayer(cancelled.target());
        if (target != null) {
            new BukkitCommandSource(target).sendMessage(
                language.raw(target, "ESSENTIALS.TPCANCEL.NOTIFY", Map.of("player", player.getName()))
            );
        }
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.TPCANCEL.USAGE");
    }
}
