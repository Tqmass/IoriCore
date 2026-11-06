package us.iorilabs.bukkit.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.configuration.file.FileConfiguration;
import us.iorilabs.api.redis.RedisEnvelope;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class RedisSettings {

    private final boolean enabled;
    private final String host;
    private final int port;
    private final int database;
    private final int timeoutMs;
    private final boolean ssl;
    private final String keyPrefix;
    private final String channel;
    private final boolean authEnabled;
    private final String username;
    private final String password;

    public static RedisSettings from(FileConfiguration settings) {
        return fromRedisFile(null, settings);
    }

    public static RedisSettings fromRedisFile(FileConfiguration redisFile, FileConfiguration settingsFallback) {
        FileConfiguration src = redisFile != null && redisFile.contains("ENABLED") ? redisFile : settingsFallback;

        if (src == null) {
            return new RedisSettings(
                false,
                "127.0.0.1",
                6379,
                0,
                2000,
                false,
                RedisEnvelope.DEFAULT_KEY_PREFIX,
                RedisEnvelope.DEFAULT_CHANNEL,
                false,
                "",
                ""
            );
        }

        boolean nested = src.contains("REDIS.ENABLED") || src.contains("REDIS");
        String p = nested ? "REDIS." : "";

        return new RedisSettings(
            src.getBoolean(p + "ENABLED", false),
            src.getString(p + "HOST", "127.0.0.1"),
            src.getInt(p + "PORT", 6379),
            Math.max(0, src.getInt(p + "DATABASE", 0)),
            Math.max(500, src.getInt(p + "TIMEOUT-MS", 2000)),
            src.getBoolean(p + "SSL", false),
            nonBlank(src.getString(p + "KEY-PREFIX"), RedisEnvelope.DEFAULT_KEY_PREFIX),
            nonBlank(src.getString(p + "CHANNEL"), RedisEnvelope.DEFAULT_CHANNEL),
            src.getBoolean(p + "AUTHENTICATION.ENABLED", false),
            src.getString(p + "AUTHENTICATION.USERNAME", ""),
            src.getString(p + "AUTHENTICATION.PASSWORD", "")
        );
    }

    private static String nonBlank(String raw, String fallback) {
        return raw == null || raw.trim().isEmpty() ? fallback : raw.trim();
    }
}
