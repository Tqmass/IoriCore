package us.iorilabs.proxy.redis;

import java.util.Optional;
import java.util.function.Function;

public interface RedisService extends AutoCloseable {
    boolean isConnected();

    String proxyId();

    String keyPrefix();

    String statusLine();

    void publish(RedisEvent event);

    void subscribe(String type, RedisEventListener listener);

    <T> Optional<T> withJedis(Function<redis.clients.jedis.Jedis, T> function);

    void reconnect();

    @Override
    void close();
}
