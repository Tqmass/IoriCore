package us.iorilabs.proxy.server;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.player.PlayerPresence;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;

public final class PresenceService {

    private final RedisService redis;
    private final ProxyLogger logger;
    private final String proxyId;
    private final String keyPrefix;
    private final Map<UUID, PlayerPresence> byUuid = new ConcurrentHashMap<>();
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();

    public PresenceService(RedisService redis, ProxyLogger logger, String proxyId, String keyPrefix) {
        this.redis = redis;
        this.logger = logger;
        this.proxyId = proxyId;
        this.keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "ioriproxy" : keyPrefix;
        subscribe();
    }

    private void subscribe() {
        redis.subscribe(ProxyEventType.PLAYER_JOIN.id(), this::onRemote);
        redis.subscribe(ProxyEventType.PLAYER_QUIT.id(), this::onRemote);
        redis.subscribe(ProxyEventType.PLAYER_SWITCH.id(), this::onRemote);
    }

    public void localJoin(UUID uuid, String name, String server) {
        PlayerPresence presence = new PlayerPresence(uuid, name, server, proxyId, System.currentTimeMillis());
        putLocal(presence);
        persist(presence);
        publish(ProxyEventType.PLAYER_JOIN, presence);
    }

    public void localSwitch(UUID uuid, String name, String server) {
        PlayerPresence presence = new PlayerPresence(uuid, name, server, proxyId, System.currentTimeMillis());
        putLocal(presence);
        persist(presence);
        publish(ProxyEventType.PLAYER_SWITCH, presence);
    }

    public void localQuit(UUID uuid) {
        PlayerPresence removed = byUuid.remove(uuid);
        if (removed != null) {
            byName.remove(removed.normalizedName());
            deletePersist(uuid, removed.normalizedName());
            publish(ProxyEventType.PLAYER_QUIT, removed);
        }
    }

    public Optional<PlayerPresence> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        UUID uuid = byName.get(name.toLowerCase(Locale.ROOT));
        if (uuid != null) return Optional.ofNullable(byUuid.get(uuid));

        return loadFromRedisByName(name);
    }

    public Optional<PlayerPresence> findByUuid(UUID uuid) {
        PlayerPresence local = byUuid.get(uuid);
        if (local != null) return Optional.of(local);

        return loadFromRedis(uuid);
    }

    public Collection<PlayerPresence> all() {
        return byUuid.values();
    }

    private void onRemote(RedisEvent event) {
        JsonObject data = event.data();
        if (!data.has("uuid") || !data.has("name")) return;

        UUID uuid = UUID.fromString(data.get("uuid").getAsString());
        String name = data.get("name").getAsString();
        String server = data.has("server") ? data.get("server").getAsString() : "";
        String origin = event.originProxyId();
        ProxyEventType type = ProxyEventType.fromId(event.type());
        switch (type) {
            case PLAYER_QUIT -> {
                PlayerPresence existing = byUuid.get(uuid);
                if (existing != null && existing.proxyId().equals(origin)) {
                    byUuid.remove(uuid);
                    byName.remove(existing.normalizedName());
                }
            }
            case PLAYER_JOIN, PLAYER_SWITCH -> putLocal(
                new PlayerPresence(uuid, name, server, origin, event.timestamp())
            );
            default -> {
            }
        }
    }

    private void putLocal(PlayerPresence presence) {
        PlayerPresence previous = byUuid.put(presence.uuid(), presence);
        if (previous != null) {
            byName.remove(previous.normalizedName());
        }

        byName.put(presence.normalizedName(), presence.uuid());
    }

    private void publish(ProxyEventType type, PlayerPresence presence) {
        JsonObject data = new JsonObject();
        data.addProperty("uuid", presence.uuid().toString());
        data.addProperty("name", presence.name());
        data.addProperty("server", presence.server());
        redis.publish(new RedisEvent(type.id(), proxyId, data));
    }

    private void persist(PlayerPresence presence) {
        redis.withJedis(jedis -> {
            String key = presenceKey(presence.uuid());
            jedis.hset(
                key,
                Map.of(
                    "uuid",
                    presence.uuid().toString(),
                    "name",
                    presence.name(),
                    "server",
                    presence.server(),
                    "proxy_id",
                    presence.proxyId(),
                    "updated_at",
                    Long.toString(presence.updatedAt())
                )
            );
            jedis.expire(key, 86_400);
            jedis.setex(nameKey(presence.normalizedName()), 86_400, presence.uuid().toString());
            return true;
        });
    }

    private void deletePersist(UUID uuid, String normalizedName) {
        redis.withJedis(jedis -> {
            jedis.del(presenceKey(uuid));
            jedis.del(nameKey(normalizedName));
            return true;
        });
    }

    private Optional<PlayerPresence> loadFromRedis(UUID uuid) {
        return redis.withJedis(jedis -> {
            Map<String, String> map = jedis.hgetAll(presenceKey(uuid));
            if (map == null || map.isEmpty()) return null;

            PlayerPresence presence = fromMap(map);
            putLocal(presence);
            return presence;
        });
    }

    private Optional<PlayerPresence> loadFromRedisByName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return redis.withJedis(jedis -> {
            String uuidRaw = jedis.get(nameKey(normalized));
            if (uuidRaw == null || uuidRaw.isBlank()) return null;

            UUID uuid = UUID.fromString(uuidRaw);
            Map<String, String> map = jedis.hgetAll(presenceKey(uuid));
            if (map == null || map.isEmpty()) return null;

            PlayerPresence presence = fromMap(map);
            putLocal(presence);
            return presence;
        });
    }

    private PlayerPresence fromMap(Map<String, String> map) {
        return new PlayerPresence(
            UUID.fromString(map.get("uuid")),
            map.getOrDefault("name", "unknown"),
            map.getOrDefault("server", ""),
            map.getOrDefault("proxy_id", "unknown"),
            Long.parseLong(map.getOrDefault("updated_at", "0"))
        );
    }

    private String presenceKey(UUID uuid) {
        return keyPrefix + ":presence:" + uuid;
    }

    private String nameKey(String normalizedName) {
        return keyPrefix + ":presence:name:" + normalizedName;
    }

    public void clearLocalOwnedByThisProxy() {
        for (PlayerPresence presence : byUuid.values().toArray(PlayerPresence[]::new)) {
            if (proxyId.equals(presence.proxyId())) {
                localQuit(presence.uuid());
            }
        }
    }
}
