package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.proxy.commands.CommandSource;

public final class BackCommand extends EssentialsCommand {

    private final EssentialsState state;

    public BackCommand(IoriCorePlugin plugin, EssentialsState state) {
        super(plugin, "back", Collections.emptyList(), "ioricore.essentials.back");
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.back")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        Location location = state.takeBack(player.getUniqueId());
        if (location == null || location.getWorld() == null) {
            send(source, "ESSENTIALS.BACK.NONE");
            return;
        }

        player.teleport(location);
        send(source, "ESSENTIALS.BACK.SUCCESS");
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.BACK.USAGE");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
