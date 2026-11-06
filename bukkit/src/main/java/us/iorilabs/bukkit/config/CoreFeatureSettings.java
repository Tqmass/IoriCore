package us.iorilabs.bukkit.config;

import org.bukkit.configuration.file.FileConfiguration;

public final class CoreFeatureSettings {

    private CoreFeatureSettings() {}

    public static boolean getBoolean(FileConfiguration settings, String key, boolean defaultValue) {
        if (settings == null) return defaultValue;
        String corePath = "CORE." + key;
        if (settings.contains(corePath)) return settings.getBoolean(corePath, defaultValue);
        return settings.getBoolean("ESSENTIALS." + key, defaultValue);
    }

    public static int getInt(FileConfiguration settings, String key, int defaultValue) {
        if (settings == null) return defaultValue;
        String corePath = "CORE." + key;
        if (settings.contains(corePath)) return settings.getInt(corePath, defaultValue);
        return settings.getInt("ESSENTIALS." + key, defaultValue);
    }

    public static long getLong(FileConfiguration settings, String key, long defaultValue) {
        if (settings == null) return defaultValue;
        String corePath = "CORE." + key;
        if (settings.contains(corePath)) return settings.getLong(corePath, defaultValue);
        return settings.getLong("ESSENTIALS." + key, defaultValue);
    }

    public static double getDouble(FileConfiguration settings, String key, double defaultValue) {
        if (settings == null) return defaultValue;
        String corePath = "CORE." + key;
        if (settings.contains(corePath)) return settings.getDouble(corePath, defaultValue);
        return settings.getDouble("ESSENTIALS." + key, defaultValue);
    }
}
