package us.iorilabs.bukkit.language;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.util.ProtocolProbe;

@FunctionalInterface
public interface ProtocolResolver {
    int protocol(Player player);

    static ProtocolResolver viaVersionOrServer() {
        int server = ProtocolProbe.serverFallbackProtocol();
        try {
            Class<?> via = Class.forName("com.viaversion.viaversion.api.Via");
            Object api = via.getMethod("getAPI").invoke(null);
            Method version = api.getClass().getMethod("getPlayerVersion", UUID.class);
            return player -> {
                if (player == null) return server;
                try {
                    Object value = version.invoke(api, player.getUniqueId());
                    return value instanceof Integer && (Integer) value > 0 ? (Integer) value : server;
                } catch (ReflectiveOperationException ex) {
                    return server;
                }
            };
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return player -> server;
        }
    }
}
