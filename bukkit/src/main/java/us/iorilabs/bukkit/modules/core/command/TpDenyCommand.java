package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.tpa.TpaRequest;
import us.iorilabs.bukkit.modules.core.tpa.TpaService;
import us.iorilabs.proxy.commands.CommandSource;

public final class TpDenyCommand extends EssentialsCommand {

    private final TpaService tpa;

    public TpDenyCommand(IoriCorePlugin plugin, TpaService tpa) {
        super(plugin, "tpdeny", Collections.singletonList("tpno"), "ioricore.essentials.tpdeny");
        this.tpa = tpa;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.tpdeny")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        TpaRequest request = tpa.take(player.getUniqueId());
        if (request == null) {
            send(source, "ESSENTIALS.TPA.NONE");
            return;
        }

        Player requester = tpa.requesterOnline(request);
        send(source, "ESSENTIALS.TPDENY.SUCCESS");
        if (requester != null) {
            new BukkitCommandSource(requester).sendMessage(
                language.raw(requester, "ESSENTIALS.TPDENY.NOTIFY", Map.of("player", player.getName()))
            );
        }
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.TPDENY.USAGE");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
