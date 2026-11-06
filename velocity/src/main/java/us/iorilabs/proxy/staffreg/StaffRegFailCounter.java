package us.iorilabs.proxy.staffreg;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.proxy.config.StaffRegConfig;
import us.iorilabs.proxy.redis.RedisService;

public final class StaffRegFailCounter {

    private final RedisService redis;
    private final String keyPrefix;
    private final StaffRegConfig config;
    private final Map<UUID, Window> local = new ConcurrentHashMap<>();

    public StaffRegFailCounter(RedisService redis, String keyPrefix, StaffRegConfig config) {
        this.redis = redis;
        this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
        this.config = config;
    }

    public int recordFail(UUID accountUuid) {
        if (accountUuid == null) return 0;
        if (redis != null && redis.isConnected()) {
            return redis
                .withJedis(jedis -> {
                    String key = keyPrefix + "staffreg:fails:" + accountUuid;
                    long count = jedis.incr(key);
                    if (count == 1L) jedis.expire(key, config.failWindowSeconds());
                    return (int) count;
                })
                .orElse(recordLocal(accountUuid));
        }
        return recordLocal(accountUuid);
    }

    public void clear(UUID accountUuid) {
        if (accountUuid == null) return;
        local.remove(accountUuid);
        if (redis != null && redis.isConnected()) {
            redis.withJedis(jedis -> {
                jedis.del(keyPrefix + "staffreg:fails:" + accountUuid);
                return null;
            });
        }
    }

    private int recordLocal(UUID accountUuid) {
        long now = System.currentTimeMillis();
        Window window = local.compute(accountUuid, (id, current) -> {
            if (current == null || now - current.startMs > config.failWindowSeconds() * 1000L) {
                return new Window(now, 1);
            }
            current.count++;
            return current;
        });
        return window == null ? 0 : window.count;
    }

    private static final class Window {

        private final long startMs;
        private int count;

        private Window(long startMs, int count) {
            this.startMs = startMs;
            this.count = count;
        }
    }
}
