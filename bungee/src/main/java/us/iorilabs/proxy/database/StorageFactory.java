package us.iorilabs.proxy.database;

import java.nio.file.Path;
import us.iorilabs.proxy.config.DatabaseConfig;
import us.iorilabs.proxy.database.mongo.MongoStorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.logging.ProxyLogger;

public final class StorageFactory {

    private StorageFactory() {}

    public static StorageService create(DatabaseConfig database, ProxyLogger logger, Path dataDirectory) {
        if (!StorageType.known(database.type())) {
            logger.warn("Unknown storage type '" + database.type() + "' in database.yml, using NONE");
        }

        StorageType type = StorageType.from(database.type());
        if ("h2".equalsIgnoreCase(database.type()) || "file".equalsIgnoreCase(database.type())) {
            logger.warn("DATABASE=H2 removed · using NONE. Set MYSQL to share ic_punishments with Core.");
        }

        try {
            return switch (type) {
                case H2, NONE -> {
                    logger.info("Persistent SQL disabled (DATABASE: NONE) · Core enforces bans via data/");
                    yield new NoopStorageService();
                }
                case MYSQL -> {
                    MySqlStorageService mysql = MySqlStorageService.mysql(database, logger);
                    mysql.connect();
                    yield mysql;
                }
                case MONGODB -> {
                    MongoStorageService mongo = MongoStorageService.create(database, logger);
                    mongo.connect();
                    logger.warn("Mongo primary · SQL features need MYSQL; bans enforced on Core if NONE");
                    yield mongo;
                }
            };
        } catch (Exception ex) {
            logger.error("Failed to connect storage (" + type + "), falling back to NONE", ex);
            return new NoopStorageService();
        }
    }
}
