package us.iorilabs.bukkit.util;

import java.util.function.ToIntFunction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.proxy.utilities.ProtocolVersions;

public final class ProtocolProbe {

    private static volatile ToIntFunction<Player> resolver;

    private ProtocolProbe() {}

    public static void resolver(ToIntFunction<Player> value) {
        resolver = value;
    }

    public static int protocol(Player player, int fallback) {
        if (player == null) return fallback;
        ToIntFunction<Player> current = resolver;
        if (current == null) return fallback;
        int protocol = current.applyAsInt(player);
        return protocol > 0 ? protocol : fallback;
    }

    public static int serverFallbackProtocol() {
        String version = Bukkit.getBukkitVersion();
        if (version == null) return ProtocolVersions.maxProtocol("1.8.9");
        int dash = version.indexOf('-');
        String mc = dash > 0 ? version.substring(0, dash) : version;
        return ProtocolVersions.maxProtocol(mc);
    }
}
