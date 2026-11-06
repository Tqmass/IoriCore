package us.iorilabs.proxy.punishment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;

@Getter
@Accessors(fluent = true)
public final class BanCheckService {

    private final BanCheckCache cache;
    private final StorageService storage;
    private final LanguageManager language;
    private final ProxyLogger logger;

    public BanCheckService(StorageService storage, LanguageManager language, ProxyLogger logger, long cacheTtlSeconds) {
        this.cache = new BanCheckCache(cacheTtlSeconds);
        this.storage = storage;
        this.language = language;
        this.logger = logger;
    }

    public Optional<String> denyJoinMessage(UUID playerId) {
        if (playerId == null) return Optional.empty();

        if (cache.knownNotBanned(playerId) || !storageReady()) return Optional.empty();

        try {
            Optional<ActiveBan> ban = loadActiveBan(playerId);
            if (ban.isEmpty()) {
                cache.markNotBanned(playerId);
                return Optional.empty();
            }

            return Optional.of(formatDeny(ban.get().reason(), ban.get().expiresLabel()));
        } catch (Exception ex) {
            logger.warn("Ban check failed (fail-open) for " + playerId + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean storageReady() {
        return storage != null && storage.isConnected() && storage.mysql().isPresent();
    }

    public void invalidate(UUID playerId) {
        cache.invalidate(playerId);
    }

    public void cacheTtl(long seconds) {
        cache.ttl(seconds);
    }

    public void shutdown() {
        cache.clear();
    }

    private String formatDeny(String reason, String expires) {
        Map<String, String> placeholders = Map.of(
            "reason",
            reason == null || reason.isBlank() ? language.raw("punishments.ban.no_reason") : reason,
            "expires",
            expires == null || expires.isBlank() ? language.raw("punishments.ban.permanent") : expires
        );
        List<String> lines = language.rawList("punishments.ban.deny", placeholders);
        if (lines != null && !lines.isEmpty()) return String.join("\n", lines);
        return language.raw("punishments.ban.deny", placeholders);
    }

    private Optional<ActiveBan> loadActiveBan(UUID playerId) {
        if (playerId == null || storage == null || !storage.isConnected()) return Optional.empty();

        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty()) return Optional.empty();

        try {
            return mysql.get().call(connection -> {
                try (
                    PreparedStatement ps = connection.prepareStatement(
                        "SELECT reason, expires_at FROM ic_punishments " +
                            "WHERE target_uuid = ? AND status = ? AND type IN (?, ?) " +
                            "ORDER BY created_at DESC LIMIT 1"
                    )
                ) {
                    ps.setString(1, playerId.toString());
                    ps.setString(2, "ACTIVE");
                    ps.setString(3, "BAN");
                    ps.setString(4, "TEMPBAN");
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return Optional.empty();

                        String reason = rs.getString("reason");
                        long expiresAt = rs.getLong("expires_at");
                        if (rs.wasNull()) expiresAt = 0L;
                        if (expiresAt > 0L && expiresAt <= System.currentTimeMillis()) return Optional.empty();

                        return Optional.of(new ActiveBan(reason, expiresLabel(expiresAt)));
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String expiresLabel(long expiresAt) {
        if (expiresAt <= 0L) return "";
        long remaining = Math.max(0L, expiresAt - System.currentTimeMillis());
        long seconds = remaining / 1000L;
        if (seconds < 60L) return seconds + "s";
        long minutes = seconds / 60L;
        if (minutes < 60L) return minutes + "m";
        long hours = minutes / 60L;
        if (hours < 48L) return hours + "h";
        return hours / 24L + "d";
    }

    private record ActiveBan(String reason, String expiresLabel) {}
}
