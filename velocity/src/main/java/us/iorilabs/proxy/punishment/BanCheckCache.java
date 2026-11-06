package us.iorilabs.proxy.punishment;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BanCheckCache {

    private final Map<UUID, Long> notBannedUntil = new ConcurrentHashMap<>();
    private volatile long ttlMillis;

    public BanCheckCache(long ttlSeconds) {
        ttl(ttlSeconds);
    }

    public void ttl(long ttlSeconds) {
        this.ttlMillis = Math.max(0L, ttlSeconds) * 1000L;
        if (ttlMillis == 0L) notBannedUntil.clear();
    }

    public boolean knownNotBanned(UUID playerId) {
        if (playerId == null) return false;

        Long until = notBannedUntil.get(playerId);
        if (until == null) return false;
        if (until > System.currentTimeMillis()) return true;

        notBannedUntil.remove(playerId, until);
        return false;
    }

    public void markNotBanned(UUID playerId) {
        if (playerId == null || ttlMillis == 0L) return;
        notBannedUntil.put(playerId, System.currentTimeMillis() + ttlMillis);
    }

    public void invalidate(UUID playerId) {
        if (playerId == null) return;
        notBannedUntil.remove(playerId);
    }

    public void clear() {
        notBannedUntil.clear();
    }
}
