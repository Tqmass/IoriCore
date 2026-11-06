package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.config.CoreFeatureSettings;
import us.iorilabs.bukkit.modules.core.home.HomeService;
import us.iorilabs.proxy.commands.CommandSource;

public final class SetHomeCommand extends EssentialsCommand {

    private final HomeService homes;

    public SetHomeCommand(IoriCorePlugin plugin, HomeService homes) {
        super(plugin, "sethome", Collections.emptyList(), "ioricore.essentials.sethome");
        this.homes = homes;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.sethome")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        String name = args.length == 0 ? "home" : args[0].toLowerCase(Locale.ROOT);
        int max = CoreFeatureSettings.getInt(plugin.settings(), "MAX-HOMES", 3);
        boolean exists = homes.get(player.getUniqueId(), name) != null;
        if (
            !exists &&
            homes.count(player.getUniqueId()) >= max &&
            !source.hasPermission("ioricore.essentials.sethome.unlimited")
        ) {
            send(source, "ESSENTIALS.HOME.LIMIT", Map.of("max", String.valueOf(max)));
            return;
        }

        homes.set(player.getUniqueId(), name, player.getLocation());
        send(source, "ESSENTIALS.SETHOME.SUCCESS", Map.of("home", name));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.SETHOME.USAGE");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
