package us.iorilabs.bukkit.modules.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.proxy.commands.CommandSource;

public final class WorldCommand extends EssentialsCommand {

    private final EssentialsState state;

    public WorldCommand(IoriCorePlugin plugin, EssentialsState state) {
        super(plugin, "world", Collections.emptyList(), "ioricore.essentials.world");
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.world")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        World world = Bukkit.getWorld(args[0]);
        if (world == null) {
            send(source, "ESSENTIALS.WORLD.NOT-FOUND", Map.of("world", args[0]));
            return;
        }

        state.rememberBack(player);
        player.teleport(world.getSpawnLocation());
        send(source, "ESSENTIALS.WORLD.SUCCESS", Map.of("world", world.getName()));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.WORLD.USAGE");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(world.getName());
        }
        return out;
    }
}
