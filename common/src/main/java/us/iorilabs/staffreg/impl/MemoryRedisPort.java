package us.iorilabs.staffreg.impl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.staffreg.storage.StaffRegRedisPort;

public final class MemoryRedisPort implements StaffRegRedisPort {

    private final Map<String, Entry> values = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> setex(String key, int ttlSeconds, String value) {
        if (key == null) return CompletableFuture.completedFuture(null);
        long expires = System.currentTimeMillis() + Math.max(1, ttlSeconds) * 1000L;
        values.put(key, new Entry(value, expires));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Optional<String>> get(String key) {
        return CompletableFuture.completedFuture(Optional.ofNullable(read(key)));
    }

    @Override
    public CompletableFuture<Optional<String>> getdel(String key) {
        Entry entry = values.remove(key);
        if (entry == null || entry.expiresAt < System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(entry.value));
    }

    @Override
    public CompletableFuture<Void> del(String key) {
        if (key != null) values.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    private String read(String key) {
        if (key == null) return null;
        Entry entry = values.get(key);
        if (entry == null) return null;
        if (entry.expiresAt < System.currentTimeMillis()) {
            values.remove(key);
            return null;
        }
        return entry.value;
    }

    private record Entry(String value, long expiresAt) {}
}
