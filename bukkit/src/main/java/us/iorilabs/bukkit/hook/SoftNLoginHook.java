package us.iorilabs.bukkit.hook;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import us.iorilabs.api.hook.NLoginHook;

public final class SoftNLoginHook implements NLoginHook {

    private final boolean present;
    private Object api;
    private Class<?> identityClass;

    public SoftNLoginHook(Logger logger, boolean moduleEnabled) {
        if (!moduleEnabled) {
            this.present = false;
            return;
        }

        this.present = Bukkit.getPluginManager().getPlugin("nLogin") != null;
        if (present) {
            logger.fine("nLogin detected (soft-hook)");
            tryLoadApi();
        }
    }

    private void tryLoadApi() {
        try {
            Class<?> apiClass = Class.forName("com.nickuc.login.api.nLoginAPI");
            api = apiClass.getMethod("getApi").invoke(null);
            identityClass = Class.forName("com.nickuc.login.api.types.Identity");
        } catch (ReflectiveOperationException ignored) {
            api = null;
            identityClass = null;
        }
    }

    @Override
    public boolean available() {
        return present && api != null;
    }

    @Override
    public boolean isAuthenticated(UUID playerId, String playerName) {
        if (!available()) return false;
        try {
            Object identity = identityOf(playerId, playerName);
            if (identity != null && identityClass != null) {
                Object result = api.getClass().getMethod("isAuthenticated", identityClass).invoke(api, identity);
                if (result instanceof Boolean) return (Boolean) result;
            }
            if (playerName != null) {
                Object result = api.getClass().getMethod("isAuthenticated", String.class).invoke(api, playerName);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (ReflectiveOperationException ignored) {}
        return false;
    }

    @Override
    public boolean isPremium(UUID playerId, String playerName) {
        if (!available()) return false;
        try {
            Object identity = identityOf(playerId, playerName);
            if (identity != null && identityClass != null) {
                Object optional = api.getClass().getMethod("getAccount", identityClass).invoke(api, identity);
                if (optional instanceof Optional) {
                    Optional<?> accountOpt = (Optional<?>) optional;
                    if (accountOpt.isPresent()) {
                        Object account = accountOpt.get();
                        Object type = account.getClass().getMethod("getAccountType").invoke(account);
                        return type != null && String.valueOf(type).toUpperCase().contains("PREMIUM");
                    }
                }
            }
            if (playerName != null) {
                Object result = api.getClass().getMethod("isPremium", String.class).invoke(api, playerName);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (ReflectiveOperationException ignored) {}
        return false;
    }

    @Override
    public boolean changePassword(UUID playerId, String playerName, String newPassword) {
        if (newPassword == null || newPassword.isEmpty() || !available()) return false;
        try {
            Object identity = identityOf(playerId, playerName);
            if (identity != null && identityClass != null) {
                Object result = api
                    .getClass()
                    .getMethod("changePassword", identityClass, String.class)
                    .invoke(api, identity, newPassword);
                return result instanceof Boolean && (Boolean) result;
            }
            if (playerName != null) {
                Object result = api
                    .getClass()
                    .getMethod("changePassword", String.class, String.class)
                    .invoke(api, playerName, newPassword);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (ReflectiveOperationException ignored) {}
        return false;
    }

    private Object identityOf(UUID playerId, String playerName) {
        if (identityClass == null) return null;
        try {
            if (playerName != null && !playerName.isEmpty()) {
                try {
                    return identityClass.getMethod("ofKnownName", String.class).invoke(null, playerName);
                } catch (NoSuchMethodException ignored) {}
            }
            if (playerId != null) {
                try {
                    return identityClass.getMethod("ofUniqueId", UUID.class).invoke(null, playerId);
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }
}
