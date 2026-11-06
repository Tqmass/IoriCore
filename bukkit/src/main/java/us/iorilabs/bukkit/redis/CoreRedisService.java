package us.iorilabs.bukkit.redis;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.api.redis.RedisEnvelope;

@Accessors(fluent = true)
public final class CoreRedisService implements AutoCloseable {

    private static final int DEDUPE_SIZE = 2048;
    private static final long RETRY_MIN_MS = 3_000L;
    private static final long RETRY_MAX_MS = 30_000L;

    @Getter
    private final String origin;

    @Getter
    private final RedisSettings settings;

    private final Logger logger;
    private final Map<String, Set<CoreRedisListener>> listeners = new ConcurrentHashMap<>();
    private final Map<String, Boolean> recentIds = Collections.synchronizedMap(
        new LinkedHashMap<String, Boolean>(256, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > DEDUPE_SIZE;
            }
        }
    );
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService publisher = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "IoriCore-Redis-Pub");
        thread.setDaemon(true);
        return thread;
    });

    private volatile JedisPool pool;
    private volatile JedisPubSub pubSub;
    private volatile Thread subscriber;

    @Getter
    private volatile boolean connected;

    public CoreRedisService(String origin, RedisSettings settings, Logger logger) {
        this.origin = origin;
        this.settings = settings;
        this.logger = logger;
    }

    public boolean enabled() {
        return settings.enabled();
    }

    public void start() {
        if (!settings.enabled() || !running.compareAndSet(false, true)) return;

        Thread thread = new Thread(this::subscribeLoop, "IoriCore-Redis-Sub");
        thread.setDaemon(true);
        subscriber = thread;
        thread.start();
    }

    public void subscribe(NetworkEventType type, CoreRedisListener listener) {
        String key = type == null ? RedisEnvelope.WILDCARD : type.id();
        listeners.computeIfAbsent(key, ignored -> new CopyOnWriteArraySet<>()).add(listener);
    }

    public void publish(NetworkEventType type, JsonObject data) {
        if (!running.get() || type == null) return;

        CoreRedisEvent event = CoreRedisEvent.create(type, origin, data);
        recentIds.put(event.eventId(), Boolean.TRUE);
        publisher.execute(() -> send(event));
    }

    public void publishRaw(String channel, String payload) {
        if (!running.get() || channel == null || channel.isBlank() || payload == null) return;
        JedisPool current = pool;
        if (current == null || current.isClosed()) return;

        publisher.execute(() -> {
            try (Jedis jedis = current.getResource()) {
                jedis.publish(channel, payload);
            } catch (Exception ex) {
                logger.warning("Redis · bridge publish failed: " + ex.getClass().getSimpleName());
            }
        });
    }

    public String statusLine() {
        if (!settings.enabled()) return "disabled";
        return (connected ? "connected " : "disconnected ") + settings.host() + ":" + settings.port();
    }

    @Override
    public void close() {
        if (!running.getAndSet(false)) return;

        connected = false;
        unsubscribeQuietly();
        Thread thread = subscriber;
        if (thread != null) thread.interrupt();
        publisher.shutdown();
        try {
            publisher.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        closePool();
    }

    private void subscribeLoop() {
        long backoff = RETRY_MIN_MS;
        while (running.get()) {
            try {
                JedisPool current = openPool();
                pubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        handle(message);
                    }
                };
                try (Jedis jedis = current.getResource()) {
                    connected = true;
                    backoff = RETRY_MIN_MS;
                    logger.info(
                        "Redis · subscribed " + settings.host() + ":" + settings.port() + " #" + settings.channel()
                    );
                    jedis.subscribe(pubSub, settings.channel());
                }
            } catch (Exception ex) {
                if (!running.get()) return;
                logger.warning(
                    "Redis · connection lost (" + ex.getClass().getSimpleName() + "), retry in " + backoff + "ms"
                );
            }

            connected = false;
            closePool();
            if (!running.get()) return;
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            backoff = Math.min(RETRY_MAX_MS, backoff * 2);
        }
    }

    private synchronized JedisPool openPool() {
        if (pool != null && !pool.isClosed()) return pool;

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(4);
        poolConfig.setMinIdle(1);
        poolConfig.setMaxWait(Duration.ofMillis(settings.timeoutMs()));
        poolConfig.setTestOnBorrow(true);

        DefaultJedisClientConfig.Builder client = DefaultJedisClientConfig.builder()
            .connectionTimeoutMillis(settings.timeoutMs())
            .socketTimeoutMillis(settings.timeoutMs())
            .database(settings.database())
            .ssl(settings.ssl());
        if (settings.authEnabled()) {
            if (settings.username() != null && !settings.username().isEmpty()) client.user(settings.username());
            if (settings.password() != null && !settings.password().isEmpty()) client.password(settings.password());
        }

        JedisPool created = new JedisPool(
            poolConfig,
            new HostAndPort(settings.host(), settings.port()),
            client.build()
        );
        try (Jedis jedis = created.getResource()) {
            jedis.ping();
        } catch (RuntimeException ex) {
            created.close();
            throw ex;
        }
        pool = created;
        return created;
    }

    private void send(CoreRedisEvent event) {
        JedisPool current = pool;
        if (current == null || current.isClosed()) return;

        try (Jedis jedis = current.getResource()) {
            jedis.publish(settings.channel(), event.toJson());
        } catch (Exception ex) {
            logger.warning("Redis · publish failed " + event.type() + ": " + ex.getClass().getSimpleName());
        }
    }

    private void handle(String message) {
        CoreRedisEvent event;
        try {
            event = CoreRedisEvent.parse(message);
        } catch (RuntimeException ex) {
            return;
        }
        if (event == null) return;
        if (origin.equals(event.origin())) return;
        if (recentIds.put(event.eventId(), Boolean.TRUE) != null) return;

        dispatch(listeners.get(event.type()), event);
        dispatch(listeners.get(RedisEnvelope.WILDCARD), event);
    }

    private void dispatch(Set<CoreRedisListener> targets, CoreRedisEvent event) {
        if (targets == null) return;
        for (CoreRedisListener listener : targets) {
            try {
                listener.onEvent(event);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Redis · listener error " + event.type(), ex);
            }
        }
    }

    private void unsubscribeQuietly() {
        JedisPubSub current = pubSub;
        if (current == null || !current.isSubscribed()) return;
        try {
            current.unsubscribe();
        } catch (Exception ignored) {}
    }

    private synchronized void closePool() {
        if (pool == null) return;
        try {
            pool.close();
        } catch (Exception ignored) {}
        pool = null;
    }
}
