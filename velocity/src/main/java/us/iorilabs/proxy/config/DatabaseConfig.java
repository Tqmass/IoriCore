package us.iorilabs.proxy.config;

import java.util.Locale;

public final class DatabaseConfig {

    private static final int MAX_POOL = 32;

    private final YamlConfig yaml;

    public DatabaseConfig(YamlConfig yaml) {
        this.yaml = yaml;
    }

    public String type() {
        return yaml.getString("database", "h2").toLowerCase(Locale.ROOT);
    }

    public String host() {
        return yaml.getString("mysql.host", "127.0.0.1");
    }

    public int port() {
        return yaml.getInt("mysql.port", 3306);
    }

    public String database() {
        return yaml.getString("mysql.database", "ioricore");
    }

    public String username() {
        return yaml.getString("mysql.username", yaml.getString("mysql.user", "root"));
    }

    public String password() {
        return yaml.getString("mysql.password", "");
    }

    public boolean ssl() {
        return yaml.getBoolean("mysql.ssl", false);
    }

    public int poolSize() {
        int size = yaml.getInt("mysql.pool-size", yaml.getInt("mysql.max-pool-size", 5));
        return Math.max(1, Math.min(MAX_POOL, size));
    }

    public int minIdle() {
        return Math.max(0, yaml.getInt("mysql.min-idle", 1));
    }

    public long connectionTimeoutMillis() {
        return Math.max(1_000L, yaml.getInt("mysql.connection-timeout-ms", 15_000));
    }

    public long maxLifetimeMillis() {
        return Math.max(30_000L, yaml.getInt("mysql.max-lifetime-ms", 1_800_000));
    }

    public String h2File() {
        String file = yaml.getString("h2.file", "ioricore");
        return file == null || file.isBlank() ? "ioricore" : file.trim();
    }

    public String mongoUri() {
        if (yaml.getBoolean("mongodb.uri-enabled", true)) {
            String uri = yaml.getString("mongodb.uri", "");
            if (uri != null && !uri.isBlank()) return uri.trim();
        }
        String host = yaml.getString("mongodb.host", "127.0.0.1");
        int port = yaml.getInt("mongodb.port", 27017);
        String db = mongoDatabase();
        return "mongodb://" + host + ":" + port + "/" + db;
    }

    public String mongoDatabase() {
        String db = yaml.getString("mongodb.database", "ioricore");
        return db == null || db.isBlank() ? "ioricore" : db.trim();
    }
}
