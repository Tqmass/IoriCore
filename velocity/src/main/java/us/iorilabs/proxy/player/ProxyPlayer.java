package us.iorilabs.proxy.player;

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public interface ProxyPlayer {
    UUID uuid();

    String name();

    String currentServer();

    boolean hasPermission(String permission);

    void sendMessage(String miniOrLegacy);

    default void sendMessage(Component component) {
        sendMessage(
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component)
        );
    }

    void connect(String serverName);

    default int protocolVersion() {
        return Integer.MAX_VALUE;
    }

    default int ping() {
        return -1;
    }

    default Optional<String> ipAddress() {
        return Optional.empty();
    }

    default Optional<String> clientBrand() {
        return Optional.empty();
    }
}
