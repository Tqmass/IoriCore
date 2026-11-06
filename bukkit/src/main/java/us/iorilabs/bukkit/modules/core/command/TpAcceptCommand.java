package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.bukkit.modules.core.tpa.TpaRequest;
import us.iorilabs.bukkit.modules.core.tpa.TpaService;
import us.iorilabs.proxy.commands.CommandSource;

public final class TpAcceptCommand extends EssentialsCommand {

    private final TpaService tpa;
    private final EssentialsState state;

    public TpAcceptCommand(IoriCorePlugin plugin, TpaService tpa, EssentialsState state) {
        super(plugin, "tpaccept", Collections.singletonList("tpyes"), "ioricore.essentials.tpaccept");
        this.tpa = tpa;
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.tpaccept")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        TpaRequest request = tpa.take(player.getUniqueId());
        if (request == null) {
            send(source, "ESSENTIALS.TPA.NONE");
            return;
        }

        Player requester = tpa.requesterOnline(request);
        if (requester == null) {
            send(source, "ESSENTIALS.TPA.OFFLINE");
            return;
        }

        if (request.here()) {
            state.rememberBack(player);
            player.teleport(requester.getLocation());
        } else {
            state.rememberBack(requester);
            requester.teleport(player.getLocation());
        }

        send(source, "ESSENTIALS.TPACCEPT.SUCCESS", Map.of("player", requester.getName()));
        new BukkitCommandSource(requester).sendMessage(
            language.raw(requester, "ESSENTIALS.TPACCEPT.NOTIFY", Map.of("player", player.getName()))
        );
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.TPACCEPT.USAGE");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
