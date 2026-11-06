package us.iorilabs.proxy.commands;

import java.util.UUID;
import net.kyori.adventure.text.Component;

public interface CommandSource {
    String name();

    boolean isConsole();

    default boolean isPlayer() {
        return !isConsole();
    }

    boolean hasPermission(String permission);

    void sendMessage(String miniOrLegacy);

    default void sendMessage(Component component) {
        sendMessage(
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component)
        );
    }

    UUID uuidOrNull();
}
