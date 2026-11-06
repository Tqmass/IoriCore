package us.iorilabs.bukkit.modules.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.bukkit.modules.core.home.HomeService;
import us.iorilabs.proxy.commands.CommandSource;

public final class HomeCommand extends EssentialsCommand {

    private final HomeService homes;
    private final EssentialsState state;

    public HomeCommand(IoriCorePlugin plugin, HomeService homes, EssentialsState state) {
        super(plugin, "home", Collections.emptyList(), "ioricore.essentials.home");
        this.homes = homes;
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.home")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        String name = args.length == 0 ? "home" : args[0];
        Location location = homes.get(player.getUniqueId(), name);
        if (location == null || location.getWorld() == null) {
            send(source, "ESSENTIALS.HOME.NOT-FOUND", Map.of("home", name));
            return;
        }

        state.rememberBack(player);
        player.teleport(location);
        EssentialsSounds.play(plugin, player, "TELEPORT");
        send(source, "ESSENTIALS.HOME.SUCCESS", Map.of("home", name.toLowerCase(Locale.ROOT)));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.HOME.USAGE");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        Player player = us.iorilabs.bukkit.modules.core.EssentialsPlayers.player(source);
        if (player == null || args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String name : homes.names(player.getUniqueId())) {
            if (name.startsWith(prefix)) out.add(name);
        }
        return out;
    }
}
