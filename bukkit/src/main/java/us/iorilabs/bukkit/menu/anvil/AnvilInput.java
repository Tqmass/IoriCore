package us.iorilabs.bukkit.menu.anvil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import us.iorilabs.abstraction.VersionWrapper;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class AnvilInput {

    private static final AtomicBoolean UNSUPPORTED_LOGGED = new AtomicBoolean();

    private AnvilInput() {}

    public static boolean open(
        IoriCorePlugin plugin,
        Player player,
        String title,
        String initial,
        Consumer<String> onComplete,
        Runnable onCancel
    ) {
        if (plugin == null || player == null || onComplete == null) return false;

        VersionWrapper wrapper = plugin.abstraction();
        if (wrapper == null || !wrapper.supportsAnvilInput()) {
            if (UNSUPPORTED_LOGGED.compareAndSet(false, true)) {
                String revision = wrapper == null ? "null" : wrapper.revision();
                plugin.getLogger().warning("Anvil input unsupported · " + revision);
            }
            return false;
        }

        wrapper.openAnvilInput(
            player.getUniqueId(),
            title == null ? "" : title,
            initial == null ? "" : initial,
            onComplete,
            onCancel
        );
        return true;
    }

    public static void close(IoriCorePlugin plugin, Player player) {
        if (plugin == null || player == null) return;
        VersionWrapper wrapper = plugin.abstraction();
        if (wrapper != null) wrapper.closeAnvilInput(player.getUniqueId());
    }
}
