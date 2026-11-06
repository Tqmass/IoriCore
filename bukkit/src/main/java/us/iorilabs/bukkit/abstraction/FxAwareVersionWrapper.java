package us.iorilabs.bukkit.abstraction;

import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.abstraction.VersionWrapper;

public final class FxAwareVersionWrapper implements VersionWrapper {

    private final VersionWrapper delegate;

    public FxAwareVersionWrapper(VersionWrapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public String revision() {
        return delegate.revision();
    }

    @Override
    public boolean supportsAnvilInput() {
        return delegate.supportsAnvilInput();
    }

    @Override
    public void openAnvilInput(
        UUID playerId,
        String title,
        String initial,
        Consumer<String> onComplete,
        Runnable onCancel
    ) {
        delegate.openAnvilInput(playerId, title, initial, onComplete, onCancel);
    }

    @Override
    public void closeAnvilInput(UUID playerId) {
        delegate.closeAnvilInput(playerId);
    }

    @Override
    public void sendLegacyTitle(UUID playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && BukkitNativeFx.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut)) {
            return;
        }
        delegate.sendLegacyTitle(playerId, title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendActionBar(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && BukkitNativeFx.sendActionBar(player, message)) {
            return;
        }
        delegate.sendActionBar(playerId, message);
    }

    @Override
    public void sendTabHeaderFooter(UUID playerId, String header, String footer) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && BukkitNativeFx.sendTabHeaderFooter(player, header, footer)) {
            return;
        }
        delegate.sendTabHeaderFooter(playerId, header, footer);
    }
}
