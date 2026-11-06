package us.iorilabs.proxy.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import lombok.Getter;
import lombok.experimental.Accessors;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import us.iorilabs.api.redis.RedisEnvelope;
import us.iorilabs.proxy.config.RedisConfig;
import us.iorilabs.proxy.logging.ProxyLogger;

@Accessors(fluent = true)
public final class JedisRedisService implements RedisService {

    @Getter
    private final String proxyId;

    private final RedisConfig config;
    private final ProxyLogger logger;
    private final Gson gson = new Gson();
    private final Map<String, Set<RedisEventListener>> listeners = new ConcurrentHashMap<>();
    private final Set<String> recentEventIds = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean subscriberStarted = new AtomicBoolean(false);
    private final ExecutorService subscriberExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "IoriProxy-Redis-Sub");
        thread.setDaemon(true);
        return thread;
    });

    private volatile JedisPool pool;
    private volatile JedisPubSub pubSub;
    private volatile boolean connected;

    public JedisRedisService(String proxyId, RedisConfig config, ProxyLogger logger) {
        this.proxyId = proxyId;
        this.config = config;
        this.logger = logger;
    }

    public synchronized void connect() {
        closePoolOnly();
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(1);
        poolConfig.setMaxWait(Duration.ofMillis(config.timeoutMs()));
        poolConfig.setTestOnBorrow(true);

        DefaultJedisClientConfig.Builder clientBuilder = DefaultJedisClientConfig.builder()
            .connectionTimeoutMillis(config.timeoutMs())
            .socketTimeoutMillis(config.timeoutMs())
            .database(config.database())
            .ssl(config.ssl());

        if (config.authEnabled()) {
            if (config.username() != null && !config.username().isBlank()) {
                clientBuilder.user(config.username());
            }

            if (config.password() != null && !config.password().isBlank()) {
                clientBuilder.password(config.password());
            }
        }

        JedisClientConfig clientConfig = clientBuilder.build();
        HostAndPort host = new HostAndPort(config.host(), config.port());
        pool = new JedisPool(poolConfig, host, clientConfig);

        try (Jedis jedis = pool.getResource()) {
            String pong = jedis.ping();
            if (!"PONG".equalsIgnoreCase(pong)) {
                throw new IllegalStateException("Unexpected Redis PING response: " + pong);
            }
        }

        connected = true;
        running.set(true);
        startSubscriber();
        logger.success(
            "Redis connected &f" +
                config.host() +
                ":" +
                config.port() +
                " &7db=" +
                config.database() +
                " ssl=" +
                config.ssl()
        );
    }

    private void startSubscriber() {
        if (!subscriberStarted.compareAndSet(false, true)) return;

        subscriberExecutor.execute(() -> {
            while (running.get()) {
                try {
                    if (pool == null) {
                        Thread.sleep(1000L);
                        continue;
                    }

                    pubSub = new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            handleMessage(message);
                        }
                    };

                    try (Jedis jedis = pool.getResource()) {
                        logger.module("Redis", "subscribed to " + config.channel());
                        jedis.subscribe(pubSub, config.channel());
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ex) {
                    connected = false;

                    if (running.get()) {
                        logger.warn("Redis subscription lost, retrying in 3s...");

                        try {
                            Thread.sleep(3000L);
                            reconnectQuiet();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        });
    }

    private void handleMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            RedisEvent event = RedisEvent.fromJson(json);
            if (proxyId.equals(event.originProxyId())) return;
            if (!recentEventIds.add(event.eventId())) return;

            trimRecentIds();
            Set<RedisEventListener> typed = listeners.get(event.type());
            if (typed != null) {
                for (RedisEventListener listener : typed) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception ex) {
                        logger.error("Redis listener error for type " + event.type(), ex);
                    }
                }
            }

            Set<RedisEventListener> all = listeners.get(RedisEnvelope.WILDCARD);
            if (all != null) {
                for (RedisEventListener listener : all) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception ex) {
                        logger.error("Redis wildcard listener error", ex);
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Ignored invalid Redis payload");
        }
    }

    private void trimRecentIds() {
        if (recentEventIds.size() > 2048) {
            recentEventIds.clear();
        }
    }

    @Override
    public boolean isConnected() {
        return connected && pool != null && !pool.isClosed();
    }

    @Override
    public String keyPrefix() {
        return config.keyPrefix();
    }

    @Override
    public String statusLine() {
        if (!isConnected()) return "disconnected (" + config.host() + ":" + config.port() + ")";

        return (
            "connected " +
            config.host() +
            ":" +
            config.port() +
            " db=" +
            config.database() +
            " channel=" +
            config.channel() +
            " ssl=" +
            config.ssl()
        );
    }

    @Override
    public void publish(RedisEvent event) {
        if (!isConnected()) {
            logger.warn("Cannot publish Redis event — not connected");
            return;
        }

        try (Jedis jedis = pool.getResource()) {
            jedis.publish(config.channel(), gson.toJson(event.toJson()));
        } catch (Exception ex) {
            connected = false;
            logger.error("Failed to publish Redis event " + event.type(), ex);
        }
    }

    @Override
    public void subscribe(String type, RedisEventListener listener) {
        listeners.computeIfAbsent(type, key -> new CopyOnWriteArraySet<>()).add(listener);
    }

    @Override
    public <T> Optional<T> withJedis(Function<Jedis, T> function) {
        if (!isConnected()) return Optional.empty();

        try (Jedis jedis = pool.getResource()) {
            return Optional.ofNullable(function.apply(jedis));
        } catch (Exception ex) {
            logger.error("Redis command failed", ex);
            connected = false;
            return Optional.empty();
        }
    }

    @Override
    public synchronized void reconnect() {
        logger.info("Reconnecting Redis...");
        reconnectQuiet();
    }

    private synchronized void reconnectQuiet() {
        try {
            if (pubSub != null && pubSub.isSubscribed()) {
                try {
                    pubSub.unsubscribe();
                } catch (Exception ignored) {}
            }

            connect();
        } catch (Exception ex) {
            connected = false;
            logger.error("Redis reconnect failed", ex);
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);
        connected = false;

        if (pubSub != null && pubSub.isSubscribed()) {
            try {
                pubSub.unsubscribe();
            } catch (Exception ignored) {}
        }

        closePoolOnly();
        subscriberExecutor.shutdownNow();

        try {
            subscriberExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.module("Redis", "closed");
    }

    private void closePoolOnly() {
        if (pool != null) {
            try {
                pool.close();
            } catch (Exception ignored) {}
            pool = null;
        }
    }
}
