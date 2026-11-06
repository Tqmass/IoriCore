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

public final class WeatherCommand extends EssentialsCommand {

    public WeatherCommand(IoriCorePlugin plugin) {
        super(plugin, "weather", Arrays.asList("sun", "rain", "thunder"), "ioricore.essentials.weather");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.weather")) return;

        Player player = EssentialsPlayers.player(source);
        World world = player != null ? player.getWorld() : plugin.getServer().getWorlds().get(0);
        if (world == null) {
            send(source, "ESSENTIALS.WORLD.NOT-FOUND", Map.of("world", "?"));
            return;
        }

        String mode = label == null ? "" : label.toLowerCase(Locale.ROOT);
        if ("weather".equals(mode)) {
            if (args.length == 0) {
                sendUsage(source);
                return;
            }
            mode = args[0].toLowerCase(Locale.ROOT);
        }

        switch (mode) {
            case "sun":
            case "clear":
                world.setStorm(false);
                world.setThundering(false);
                send(source, "ESSENTIALS.WEATHER.SUN", Map.of("world", world.getName()));
                break;
            case "rain":
                world.setStorm(true);
                world.setThundering(false);
                send(source, "ESSENTIALS.WEATHER.RAIN", Map.of("world", world.getName()));
                break;
            case "thunder":
            case "storm":
                world.setStorm(true);
                world.setThundering(true);
                send(source, "ESSENTIALS.WEATHER.THUNDER", Map.of("world", world.getName()));
                break;
            default:
                send(source, "ESSENTIALS.WEATHER.INVALID", Map.of("weather", mode));
        }
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.WEATHER.USAGE");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (!"weather".equalsIgnoreCase(label)) return Collections.emptyList();
        if (args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new java.util.ArrayList<>();
        for (String option : Arrays.asList("sun", "clear", "rain", "thunder", "storm")) {
            if (option.startsWith(prefix)) out.add(option);
        }
        return out;
    }
}
