package us.iorilabs.bukkit.storage;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
@Accessors(fluent = true)
public final class SoftMongoStorage implements AutoCloseable {

    private final boolean available;
    private Object client;

    private SoftMongoStorage(boolean available, Object client) {
        this.available = available;
        this.client = client;
    }

    public static SoftMongoStorage tryOpen(FileConfiguration settings, Logger logger) {
        return tryOpen(settings, null, logger);
    }

    public static SoftMongoStorage tryOpen(FileConfiguration settings, FileConfiguration database, Logger logger) {
        FileConfiguration src = database != null ? database : settings;
        if (src == null) return new SoftMongoStorage(false, null);

        boolean enabled = src.getBoolean("MONGODB.ENABLED", false) || src.getBoolean("STORAGE.MONGO.ENABLED", false);
        if (!enabled) return new SoftMongoStorage(false, null);

        try {
            Class.forName("com.mongodb.client.MongoClients");
        } catch (ClassNotFoundException ex) {
            logger.warning("Mongo soft · driver jar not present (compileOnly / external) — noop");
            return new SoftMongoStorage(false, null);
        }

        String uri = src.getString("MONGODB.URI", src.getString("STORAGE.MONGO.URI", "mongodb://127.0.0.1:27017"));
        String databaseName = src.getString("MONGODB.DATABASE", src.getString("STORAGE.MONGO.DATABASE", "ioricore"));
        try {
            Class<?> clients = Class.forName("com.mongodb.client.MongoClients");
            Method create = clients.getMethod("create", String.class);
            Object mongoClient = create.invoke(null, uri);
            Method getDatabase = mongoClient.getClass().getMethod("getDatabase", String.class);
            Object db = getDatabase.invoke(mongoClient, databaseName);

            Class<?> documentType = Class.forName("org.bson.Document");
            Constructor<?> docCtor = documentType.getConstructor(String.class, Object.class);
            Object ping = docCtor.newInstance("ping", Integer.valueOf(1));

            Method runCommand = findRunCommand(db.getClass(), documentType);
            if (runCommand == null) {
                throw new IllegalStateException("MongoDatabase#runCommand not found");
            }
            runCommand.invoke(db, ping);
            logger.info("Mongo soft · connected uri=" + maskUri(uri) + " db=" + databaseName);
            return new SoftMongoStorage(true, mongoClient);
        } catch (Throwable ex) {
            logger.log(Level.WARNING, "Mongo soft · unavailable — YAML/MySQL continues", ex);
            return new SoftMongoStorage(false, null);
        }
    }

    static String maskUri(String uri) {
        if (uri == null || uri.isBlank()) return "?";
        return uri.replaceAll("://([^/@]+):([^/@]+)@", "://***:***@");
    }

    private static Method findRunCommand(Class<?> databaseClass, Class<?> documentType) {
        try {
            return databaseClass.getMethod("runCommand", documentType);
        } catch (NoSuchMethodException ignored) {}

        for (Method method : databaseClass.getMethods()) {
            if (!"runCommand".equals(method.getName()) || method.getParameterCount() != 1) continue;
            return method;
        }
        return null;
    }

    @Override
    public void close() {
        if (client == null) return;
        try {
            client.getClass().getMethod("close").invoke(client);
        } catch (ReflectiveOperationException ignored) {}
        client = null;
    }
}
