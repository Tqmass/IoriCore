package us.iorilabs.proxy.database.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import us.iorilabs.proxy.config.DatabaseConfig;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.StorageType;
import us.iorilabs.proxy.logging.ProxyLogger;

public final class MongoStorageService implements StorageService {

    private final DatabaseConfig config;
    private final ProxyLogger logger;
    private MongoClient client;
    private MongoDatabase database;
    private boolean connected;

    private MongoStorageService(DatabaseConfig config, ProxyLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    public static MongoStorageService create(DatabaseConfig config, ProxyLogger logger) {
        return new MongoStorageService(config, logger);
    }

    public void connect() {
        String uri = config.mongoUri();
        client = MongoClients.create(uri);
        database = client.getDatabase(config.mongoDatabase());
        database.runCommand(new Document("ping", 1));
        connected = true;
        logger.info("Mongo · connected db=" + config.mongoDatabase());
    }

    public MongoDatabase database() {
        return database;
    }

    @Override
    public StorageType type() {
        return StorageType.MONGODB;
    }

    @Override
    public boolean isConnected() {
        return connected && client != null && database != null;
    }

    @Override
    public String statusLine() {
        if (!isConnected()) return "mongodb · disconnected";
        return "mongodb · " + config.mongoDatabase();
    }

    @Override
    public void close() {
        connected = false;
        if (client == null) return;
        try {
            client.close();
        } catch (Exception ignored) {}
        client = null;
        database = null;
    }
}
