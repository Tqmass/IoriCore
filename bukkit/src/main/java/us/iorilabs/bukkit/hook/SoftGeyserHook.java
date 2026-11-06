package us.iorilabs.bukkit.hook;

import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import us.iorilabs.api.hook.GeyserHook;

public final class SoftGeyserHook implements GeyserHook {

    private final boolean present;
    private Object geyserApi;
    private Object floodgateApi;

    public SoftGeyserHook(Logger logger, boolean moduleEnabled) {
        if (!moduleEnabled) {
            this.present = false;
            return;
        }
        this.present =
            Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null ||
            Bukkit.getPluginManager().getPlugin("floodgate") != null;
        if (present) {
            logger.fine("Geyser/Floodgate detected (soft-hook)");
            tryLoadApi();
        }
    }

    private void tryLoadApi() {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            geyserApi = apiClass.getMethod("api").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            geyserApi = null;
        }
        try {
            Class<?> floodgateClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgateApi = floodgateClass.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            floodgateApi = null;
        }
    }

    @Override
    public boolean available() {
        return present && (geyserApi != null || floodgateApi != null);
    }

    @Override
    public boolean isBedrock(UUID playerId) {
        if (playerId == null || !available()) return false;
        if (floodgateApi != null) {
            try {
                Object result = floodgateApi
                    .getClass()
                    .getMethod("isFloodgatePlayer", UUID.class)
                    .invoke(floodgateApi, playerId);
                if (result instanceof Boolean && (Boolean) result) return true;
            } catch (ReflectiveOperationException ignored) {}
        }
        if (geyserApi != null) {
            try {
                Object connection = geyserApi
                    .getClass()
                    .getMethod("connectionByUuid", UUID.class)
                    .invoke(geyserApi, playerId);
                return connection != null;
            } catch (ReflectiveOperationException ignored) {}
        }
        return false;
    }
}
