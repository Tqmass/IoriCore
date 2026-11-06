package us.iorilabs.bukkit.modules.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.home.HomeService;
import us.iorilabs.proxy.commands.CommandSource;

public final class DelHomeCommand extends EssentialsCommand {

    private final HomeService homes;

    public DelHomeCommand(IoriCorePlugin plugin, HomeService homes) {
        super(plugin, "delhome", Collections.singletonList("deletehome"), "ioricore.essentials.delhome");
        this.homes = homes;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.delhome")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        if (!homes.delete(player.getUniqueId(), name)) {
            send(source, "ESSENTIALS.HOME.NOT-FOUND", Map.of("home", name));
            return;
        }
        send(source, "ESSENTIALS.DELHOME.SUCCESS", Map.of("home", name));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.DELHOME.USAGE");
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
