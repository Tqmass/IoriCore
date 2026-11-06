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
import us.iorilabs.bukkit.modules.core.warp.WarpService;
import us.iorilabs.proxy.commands.CommandSource;

public final class WarpCommand extends EssentialsCommand {

    private final WarpService warps;
    private final EssentialsState state;

    public WarpCommand(IoriCorePlugin plugin, WarpService warps, EssentialsState state) {
        super(plugin, "warp", Collections.singletonList("warps"), "ioricore.essentials.warp");
        this.warps = warps;
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.warp")) return;

        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            if (warps.names().isEmpty()) {
                send(source, "ESSENTIALS.WARP.EMPTY");
                return;
            }
            send(source, "ESSENTIALS.WARP.LIST", Map.of("warps", String.join(", ", warps.names())));
            return;
        }

        Player player = requirePlayer(source);
        if (player == null) return;

        String name = args[0].toLowerCase(Locale.ROOT);
        Location location = warps.get(name);
        if (location == null || location.getWorld() == null) {
            send(source, "ESSENTIALS.WARP.NOT-FOUND", Map.of("warp", name));
            return;
        }

        state.rememberBack(player);
        player.teleport(location);
        EssentialsSounds.play(plugin, player, "TELEPORT");
        send(source, "ESSENTIALS.WARP.SUCCESS", Map.of("warp", name));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.WARP.USAGE");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String name : warps.names()) {
            if (name.startsWith(prefix)) out.add(name);
        }
        return out;
    }
}
