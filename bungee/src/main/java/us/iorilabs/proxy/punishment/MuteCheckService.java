package us.iorilabs.proxy.punishment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.logging.ProxyLogger;

public final class MuteCheckService {

    private final StorageService storage;
    private final ProxyLogger logger;
    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();
    private volatile long ttlMillis;

    public MuteCheckService(StorageService storage, ProxyLogger logger, long ttlSeconds) {
        this.storage = storage;
        this.logger = logger;
        cacheTtl(ttlSeconds);
    }

    public void cacheTtl(long seconds) {
        this.ttlMillis = Math.max(0L, seconds) * 1000L;
        if (ttlMillis == 0L) cache.clear();
    }

    public boolean isMuted(UUID playerId) {
        if (playerId == null) return false;

        long now = System.currentTimeMillis();
        Entry cached = cache.get(playerId);
        if (cached != null && cached.validUntil > now) {
            return cached.mutedUntil != 0L && (cached.mutedUntil < 0L || cached.mutedUntil > now);
        }

        long mutedUntil = loadMutedUntil(playerId);
        if (ttlMillis > 0L) cache.put(playerId, new Entry(mutedUntil, now + ttlMillis));
        return mutedUntil != 0L && (mutedUntil < 0L || mutedUntil > now);
    }

    public void invalidate(UUID playerId) {
        if (playerId != null) cache.remove(playerId);
    }

    public void shutdown() {
        cache.clear();
    }

    // ~ 0 = sin mute · -1 = permanente · >0 = expira en ese epoch ms. Fail-open si SQL cae.
    private long loadMutedUntil(UUID playerId) {
        if (storage == null || !storage.isConnected()) return 0L;

        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty()) return 0L;

        try (
            Connection connection = mysql.get().connection();
            PreparedStatement ps = connection.prepareStatement(
                "SELECT expires_at FROM ic_punishments WHERE target_uuid = ? AND status = ? AND type IN (?, ?) " +
                    "ORDER BY created_at DESC"
            )
        ) {
            ps.setString(1, playerId.toString());
            ps.setString(2, "ACTIVE");
            ps.setString(3, "MUTE");
            ps.setString(4, "TEMPMUTE");
            long now = System.currentTimeMillis();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long expiresAt = rs.getLong(1);
                    if (rs.wasNull() || expiresAt <= 0L) return -1L;
                    if (expiresAt > now) return expiresAt;
                }
            }
        } catch (Exception ex) {
            logger.warn("Mute check failed (fail-open): " + ex.getMessage());
        }
        return 0L;
    }

    private record Entry(long mutedUntil, long validUntil) {}
}
