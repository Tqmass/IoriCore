package us.iorilabs.proxy.redis;

import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.api.redis.RedisEnvelope;

@Accessors(fluent = true)
public final class RedisEvent {

    public static final int CURRENT_SCHEMA = RedisEnvelope.SCHEMA_VERSION;

    @Getter
    private final String type;

    @Getter
    private final int schemaVersion;

    @Getter
    private final String originProxyId;

    @Getter
    private final String eventId;

    @Getter
    private final long timestamp;

    @Getter
    private final JsonObject data;

    public RedisEvent(String type, String originProxyId, JsonObject data) {
        this(type, CURRENT_SCHEMA, originProxyId, UUID.randomUUID().toString(), System.currentTimeMillis(), data);
    }

    public RedisEvent(
        String type,
        int schemaVersion,
        String originProxyId,
        String eventId,
        long timestamp,
        JsonObject data
    ) {
        this.type = Objects.requireNonNull(type, "type");
        this.schemaVersion = schemaVersion;
        this.originProxyId = Objects.requireNonNull(originProxyId, "originProxyId");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.timestamp = timestamp;
        this.data = data == null ? new JsonObject() : data;
    }

    public static RedisEvent of(ProxyEventType type, String originProxyId, JsonObject data) {
        return new RedisEvent(type.id(), originProxyId, data);
    }

    public static RedisEvent clientProtocol(String originProxyId, UUID uuid, int protocol, String server) {
        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.UUID, uuid.toString());
        data.addProperty(NetworkEventKeys.PROTOCOL, protocol);
        data.addProperty(NetworkEventKeys.SERVER, server == null ? "" : server);
        return of(ProxyEventType.CLIENT_PROTOCOL, originProxyId, data);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(RedisEnvelope.TYPE, type);
        json.addProperty(RedisEnvelope.SCHEMA, schemaVersion);
        json.addProperty(RedisEnvelope.ORIGIN, originProxyId);
        json.addProperty(RedisEnvelope.EVENT_ID, eventId);
        json.addProperty(RedisEnvelope.TIMESTAMP, timestamp);
        json.add(RedisEnvelope.DATA, data);
        return json;
    }

    public static RedisEvent fromJson(JsonObject json) {
        if (
            json == null ||
            !json.has(RedisEnvelope.TYPE) ||
            !json.has(RedisEnvelope.ORIGIN) ||
            !json.has(RedisEnvelope.EVENT_ID)
        ) {
            throw new IllegalArgumentException("Invalid Redis event envelope");
        }

        JsonObject data =
            json.has(RedisEnvelope.DATA) && json.get(RedisEnvelope.DATA).isJsonObject()
                ? json.getAsJsonObject(RedisEnvelope.DATA)
                : new JsonObject();
        return new RedisEvent(
            json.get(RedisEnvelope.TYPE).getAsString(),
            json.has(RedisEnvelope.SCHEMA) ? json.get(RedisEnvelope.SCHEMA).getAsInt() : CURRENT_SCHEMA,
            json.get(RedisEnvelope.ORIGIN).getAsString(),
            json.get(RedisEnvelope.EVENT_ID).getAsString(),
            json.has(RedisEnvelope.TIMESTAMP) ? json.get(RedisEnvelope.TIMESTAMP).getAsLong() : 0L,
            data
        );
    }
}
