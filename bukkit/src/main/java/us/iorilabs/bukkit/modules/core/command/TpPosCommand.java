package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.proxy.commands.CommandSource;

public final class TpPosCommand extends EssentialsCommand {

    private final EssentialsState state;

    public TpPosCommand(IoriCorePlugin plugin, EssentialsState state) {
        super(plugin, "tppos", Collections.singletonList("teleportpos"), "ioricore.essentials.tppos");
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.tppos")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (args.length < 3) {
            sendUsage(source);
            return;
        }

        Double x = parseCoord(args[0], player.getLocation().getX());
        Double y = parseCoord(args[1], player.getLocation().getY());
        Double z = parseCoord(args[2], player.getLocation().getZ());
        if (x == null || y == null || z == null) {
            send(source, "ESSENTIALS.TPPOS.INVALID");
            return;
        }

        World world = player.getWorld();
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        int index = 3;
        if (args.length > index) {
            World named = Bukkit.getWorld(args[index]);
            if (named != null) {
                world = named;
                index++;
            }
        }
        if (args.length > index) {
            try {
                yaw = Float.parseFloat(args[index++]);
            } catch (NumberFormatException ex) {
                send(source, "ESSENTIALS.TPPOS.INVALID");
                return;
            }
        }
        if (args.length > index) {
            try {
                pitch = Float.parseFloat(args[index]);
            } catch (NumberFormatException ex) {
                send(source, "ESSENTIALS.TPPOS.INVALID");
                return;
            }
        }

        state.rememberBack(player);
        player.teleport(new Location(world, x, y, z, yaw, pitch));
        send(
            source,
            "ESSENTIALS.TPPOS.SUCCESS",
            Map.of(
                "x",
                String.format("%.1f", x),
                "y",
                String.format("%.1f", y),
                "z",
                String.format("%.1f", z),
                "world",
                world.getName()
            )
        );
    }

    private static Double parseCoord(String raw, double relative) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String token = raw.trim();
        try {
            if (token.startsWith("~")) {
                if (token.length() == 1) return relative;
                return relative + Double.parseDouble(token.substring(1));
            }
            return Double.parseDouble(token);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.TPPOS.USAGE");
    }
}
