package us.iorilabs.bukkit.hook;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import us.iorilabs.api.hook.LiteBansHook;

public final class SoftLiteBansHook implements LiteBansHook {

    private final boolean present;
    private Object database;

    public SoftLiteBansHook(Logger logger, boolean moduleEnabled) {
        if (!moduleEnabled) {
            this.present = false;
            return;
        }

        this.present = Bukkit.getPluginManager().getPlugin("LiteBans") != null;
        if (present) {
            logger.fine("LiteBans detected (soft-hook)");
            tryLoadApi();
        }
    }

    private void tryLoadApi() {
        try {
            Class<?> databaseClass = Class.forName("litebans.api.Database");
            database = databaseClass.getMethod("get").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            database = null;
        }
    }

    @Override
    public boolean available() {
        return present && database != null;
    }

    @Override
    public boolean isBanned(UUID playerId) {
        if (playerId == null || !available()) return false;
        try {
            Object result = database
                .getClass()
                .getMethod("isPlayerBanned", UUID.class, String.class)
                .invoke(database, playerId, null);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    @Override
    public boolean isMuted(UUID playerId) {
        if (playerId == null || !available()) return false;
        try {
            Object result = database
                .getClass()
                .getMethod("isPlayerMuted", UUID.class, String.class)
                .invoke(database, playerId, null);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    @Override
    public Optional<String> activeBanReason(UUID playerId) {
        if (playerId == null || !available()) return Optional.empty();
        return Optional.empty();
    }
}
