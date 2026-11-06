package us.iorilabs.proxy.blacklist;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.proxy.config.BlacklistConfig;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;

public final class BlacklistService {

    private final ProxyPlatform platform;
    private final RedisService redis;
    private final BlacklistSql sql;
    private BlacklistConfig config;
    private final LanguageManager language;
    private final ProxyLogger logger;
    private final String proxyId;
    private final Set<String> entries = ConcurrentHashMap.newKeySet();

    public BlacklistService(
        ProxyPlatform platform,
        RedisService redis,
        StorageService storage,
        BlacklistConfig config,
        LanguageManager language,
        ProxyLogger logger,
        String proxyId
    ) {
        this.platform = platform;
        this.redis = redis;
        this.sql = new BlacklistSql(storage, logger);
        this.config = config;
        this.language = language;
        this.logger = logger;
        this.proxyId = proxyId;
        load();
        redis.subscribe(ProxyEventType.BLACKLIST_UPDATE.id(), this::onRemote);
    }

    public void reloadConfig(BlacklistConfig config) {
        this.config = config;
    }

    public boolean enabled() {
        return true;
    }

    public boolean isBlacklisted(UUID uuid, String name) {
        return (
            entries.contains(uuid.toString().toLowerCase(Locale.ROOT)) ||
            entries.contains(name.toLowerCase(Locale.ROOT))
        );
    }

    public void add(String entry, String reason, boolean silent, String by) {
        String normalized = entry.toLowerCase(Locale.ROOT);
        entries.add(normalized);
        persist();
        publish();
        platform.asyncExecutor().execute(() -> sql.insert(normalized, reason, by));

        if (!silent) {
            platform.broadcast(
                language.raw("blacklist.added", Map.of("player", entry, "reason", reason == null ? "" : reason))
            );
        }

        logger.module("Blacklist", "added " + entry + " by " + by);
        String deny = denyMessage();
        platform.findPlayerByName(entry).ifPresent(player -> platform.kickPlayer(player.uuid(), deny));

        try {
            UUID uuid = UUID.fromString(entry);
            platform.kickPlayer(uuid, deny);
        } catch (IllegalArgumentException ignored) {}
    }

    public void remove(String entry, String reason, boolean silent, String by) {
        String normalized = entry.toLowerCase(Locale.ROOT);
        entries.remove(normalized);
        persist();
        publish();
        platform.asyncExecutor().execute(() -> sql.delete(normalized));

        if (!silent) {
            platform.broadcast(language.raw("blacklist.removed", Map.of("player", entry)));
        }

        logger.module("Blacklist", "removed " + entry + " by " + by);
    }

    public String denyMessage() {
        List<String> lines = language.rawList("blacklist.deny");
        if (!lines.isEmpty()) return String.join("\n", lines);

        return language.raw("blacklist.deny");
    }

    public String silentFlag() {
        return BlacklistConfig.SILENT_FLAG;
    }

    public Set<String> entries() {
        return Set.copyOf(entries);
    }

    private void load() {
        sql.loadAll().forEach(entry -> entries.add(entry.toLowerCase(Locale.ROOT)));
        if (sql.shared()) {
            persist();
            return;
        }

        redis.withJedis(jedis -> {
            Set<String> values = jedis.smembers(redis.keyPrefix() + ":blacklist");
            if (values != null) {
                entries.addAll(values);
            }

            return true;
        });
    }

    private void persist() {
        redis.withJedis(jedis -> {
            String key = redis.keyPrefix() + ":blacklist";
            jedis.del(key);

            if (!entries.isEmpty()) {
                jedis.sadd(key, entries.toArray(String[]::new));
            }

            return true;
        });
    }

    private void publish() {
        JsonObject data = new JsonObject();
        JsonArray array = new JsonArray();

        for (String entry : entries) {
            array.add(entry);
        }

        data.add("entries", array);
        redis.publish(new RedisEvent(ProxyEventType.BLACKLIST_UPDATE.id(), proxyId, data));
    }

    private void onRemote(RedisEvent event) {
        entries.clear();

        if (event.data().has("entries") && event.data().get("entries").isJsonArray()) {
            event
                .data()
                .getAsJsonArray("entries")
                .forEach(el -> entries.add(el.getAsString().toLowerCase(Locale.ROOT)));
        }
    }
}
