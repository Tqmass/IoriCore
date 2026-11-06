package us.iorilabs.abstraction;

import java.util.UUID;
import java.util.function.Consumer;

public interface VersionWrapper {
    String revision();

    default boolean supportsAnvilInput() {
        return false;
    }

    default void openAnvilInput(UUID playerId, String title, String initial, Consumer<String> onComplete) {
        openAnvilInput(playerId, title, initial, onComplete, null);
    }

    default void openAnvilInput(
        UUID playerId,
        String title,
        String initial,
        Consumer<String> onComplete,
        Runnable onCancel
    ) {}

    default void closeAnvilInput(UUID playerId) {}

    default void sendLegacyTitle(UUID playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut) {}

    default void sendActionBar(UUID playerId, String message) {}

    default void sendTabHeaderFooter(UUID playerId, String header, String footer) {}
}
