package us.iorilabs.api.redis;

public final class RedisEnvelope {

    public static final int SCHEMA_VERSION = 1;
    public static final String DEFAULT_CHANNEL = "ioriproxy";
    public static final String DEFAULT_KEY_PREFIX = "ioriproxy";
    public static final String WILDCARD = "*";

    public static final String TYPE = "type";
    public static final String SCHEMA = "schema_version";
    public static final String ORIGIN = "origin_proxy_id";
    public static final String EVENT_ID = "event_id";
    public static final String TIMESTAMP = "timestamp";
    public static final String DATA = "data";

    private RedisEnvelope() {}
}
