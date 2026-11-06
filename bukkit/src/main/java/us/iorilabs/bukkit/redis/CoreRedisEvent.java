package us.iorilabs.bukkit.redis;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.api.redis.RedisEnvelope;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class CoreRedisEvent {

    private final String type;
    private final int schemaVersion;
    private final String origin;
    private final String eventId;
    private final long timestamp;
    private final JsonObject data;

    public static CoreRedisEvent create(NetworkEventType type, String origin, JsonObject data) {
        return new CoreRedisEvent(
            type.id(),
            RedisEnvelope.SCHEMA_VERSION,
            origin,
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            data == null ? new JsonObject() : data
        );
    }

    @SuppressWarnings("deprecation")
    public static CoreRedisEvent parse(String raw) {
        JsonElement element = new JsonParser().parse(raw);
        if (element == null || !element.isJsonObject()) return null;

        JsonObject json = element.getAsJsonObject();
        if (!json.has(RedisEnvelope.TYPE) || !json.has(RedisEnvelope.ORIGIN) || !json.has(RedisEnvelope.EVENT_ID)) {
            return null;
        }

        JsonObject data =
            json.has(RedisEnvelope.DATA) && json.get(RedisEnvelope.DATA).isJsonObject()
                ? json.getAsJsonObject(RedisEnvelope.DATA)
                : new JsonObject();
        return new CoreRedisEvent(
            json.get(RedisEnvelope.TYPE).getAsString(),
            json.has(RedisEnvelope.SCHEMA) ? json.get(RedisEnvelope.SCHEMA).getAsInt() : RedisEnvelope.SCHEMA_VERSION,
            json.get(RedisEnvelope.ORIGIN).getAsString(),
            json.get(RedisEnvelope.EVENT_ID).getAsString(),
            json.has(RedisEnvelope.TIMESTAMP) ? json.get(RedisEnvelope.TIMESTAMP).getAsLong() : 0L,
            data
        );
    }

    public NetworkEventType networkType() {
        return NetworkEventType.fromId(type);
    }

    public String string(String key) {
        if (!data.has(key) || data.get(key).isJsonNull()) return null;
        return data.get(key).getAsString();
    }

    public UUID uuid(String key) {
        String raw = string(key);
        if (raw == null || raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public int integer(String key, int fallback) {
        if (!data.has(key) || data.get(key).isJsonNull()) return fallback;
        try {
            return data.get(key).getAsInt();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    public String toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(RedisEnvelope.TYPE, type);
        json.addProperty(RedisEnvelope.SCHEMA, schemaVersion);
        json.addProperty(RedisEnvelope.ORIGIN, origin);
        json.addProperty(RedisEnvelope.EVENT_ID, eventId);
        json.addProperty(RedisEnvelope.TIMESTAMP, timestamp);
        json.add(RedisEnvelope.DATA, data);
        return json.toString();
    }
}
