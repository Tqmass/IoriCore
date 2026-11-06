package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.proxy.commands.CommandSource;

public final class TimeCommand extends EssentialsCommand {

    public TimeCommand(IoriCorePlugin plugin) {
        super(plugin, "time", Arrays.asList("day", "night"), "ioricore.essentials.time");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.time")) return;

        Player player = EssentialsPlayers.player(source);
        World world = player != null ? player.getWorld() : plugin.getServer().getWorlds().get(0);
        if (world == null) {
            send(source, "ESSENTIALS.WORLD.NOT-FOUND", Map.of("world", "?"));
            return;
        }

        String mode = label == null ? "" : label.toLowerCase(Locale.ROOT);
        if ("time".equals(mode)) {
            if (args.length == 0) {
                sendUsage(source);
                return;
            }
            mode = args[0].toLowerCase(Locale.ROOT);
        }

        long ticks;
        switch (mode) {
            case "day":
                ticks = 1000L;
                break;
            case "night":
                ticks = 13000L;
                break;
            case "noon":
                ticks = 6000L;
                break;
            case "midnight":
                ticks = 18000L;
                break;
            default:
                try {
                    ticks = Long.parseLong(mode);
                } catch (NumberFormatException ex) {
                    send(source, "ESSENTIALS.TIME.INVALID", Map.of("time", mode));
                    return;
                }
        }

        world.setTime(ticks);
        send(source, "ESSENTIALS.TIME.SUCCESS", Map.of("world", world.getName(), "time", String.valueOf(ticks)));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.TIME.USAGE");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (!"time".equalsIgnoreCase(label) || args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new java.util.ArrayList<>();
        for (String option : Arrays.asList("day", "night", "noon", "midnight")) {
            if (option.startsWith(prefix)) out.add(option);
        }
        return out;
    }
}
