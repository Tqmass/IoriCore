package us.iorilabs.staffreg.impl;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import us.iorilabs.staffreg.crypto.OtcGenerator;
import us.iorilabs.staffreg.storage.StaffRegRedisPort;

public final class RedisOtcStore {

    private final StaffRegRedisPort redis;
    private final String keyPrefix;

    public RedisOtcStore(StaffRegRedisPort redis, String keyPrefix) {
        this.redis = redis;
        this.keyPrefix = keyPrefix == null ? "staffreg:otc:" : keyPrefix;
    }

    public CompletableFuture<String> issue(UUID accountUuid, int ttlSeconds) {
        String code = OtcGenerator.generate(12);
        String hash = OtcGenerator.hash(code);
        return redis.setex(keyPrefix + hash, Math.max(60, ttlSeconds), accountUuid.toString()).thenApply(v -> code);
    }

    public CompletableFuture<Optional<UUID>> consume(String code) {
        if (code == null || code.isBlank()) return CompletableFuture.completedFuture(Optional.empty());
        String hash = OtcGenerator.hash(code);
        return redis.getdel(keyPrefix + hash).thenApply(opt -> {
            if (opt.isEmpty()) return Optional.empty();
            try {
                return Optional.of(UUID.fromString(opt.get()));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        });
    }
}
