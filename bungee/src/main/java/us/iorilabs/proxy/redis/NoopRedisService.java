package us.iorilabs.proxy.redis;

import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class NoopRedisService implements RedisService {

    private final String proxyId;
    private final String keyPrefix;

    public NoopRedisService(String proxyId) {
        this(proxyId, "ioriproxy");
    }

    public NoopRedisService(String proxyId, String keyPrefix) {
        this.proxyId = proxyId;
        this.keyPrefix = keyPrefix == null ? "ioriproxy" : keyPrefix;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public String statusLine() {
        return "disabled";
    }

    @Override
    public void publish(RedisEvent event) {}

    @Override
    public void subscribe(String type, RedisEventListener listener) {}

    @Override
    public <T> Optional<T> withJedis(Function<redis.clients.jedis.Jedis, T> function) {
        return Optional.empty();
    }

    @Override
    public void reconnect() {}

    @Override
    public void close() {}
}
