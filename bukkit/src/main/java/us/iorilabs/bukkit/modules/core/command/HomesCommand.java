package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.home.HomeService;
import us.iorilabs.proxy.commands.CommandSource;

public final class HomesCommand extends EssentialsCommand {

    private final HomeService homes;

    public HomesCommand(IoriCorePlugin plugin, HomeService homes) {
        super(plugin, "homes", Collections.emptyList(), "ioricore.essentials.homes");
        this.homes = homes;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.homes")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        Set<String> names = homes.names(player.getUniqueId());
        if (names.isEmpty()) {
            send(source, "ESSENTIALS.HOMES.EMPTY");
            return;
        }
        send(source, "ESSENTIALS.HOMES.LIST", Map.of("homes", String.join(", ", names)));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.HOMES.USAGE");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
