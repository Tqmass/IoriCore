package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.proxy.commands.CommandSource;

public final class TopCommand extends EssentialsCommand {

    private final EssentialsState state;

    public TopCommand(IoriCorePlugin plugin, EssentialsState state) {
        super(plugin, "top", Collections.emptyList(), "ioricore.essentials.top");
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.top")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        state.rememberBack(player);
        Block highest = player.getWorld().getHighestBlockAt(player.getLocation());
        Location dest = highest.getLocation().add(0.5, 1.0, 0.5);
        dest.setYaw(player.getLocation().getYaw());
        dest.setPitch(player.getLocation().getPitch());
        player.teleport(dest);
        send(source, "ESSENTIALS.TOP.SUCCESS");
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.TOP.USAGE");
    }
}
