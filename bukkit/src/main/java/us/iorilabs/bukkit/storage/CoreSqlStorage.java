package us.iorilabs.bukkit.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.bukkit.configuration.file.FileConfiguration;
import us.iorilabs.api.storage.SqlStorage;
import us.iorilabs.api.storage.StorageBackend;
import us.iorilabs.proxy.storage.MigrationRunner;
import us.iorilabs.proxy.storage.SchemaCatalog;

public final class CoreSqlStorage implements SqlStorage {

    private static final int MAX_POOL = 32;

    private final Logger logger;
    private final StorageBackend backend;
    private HikariDataSource dataSource;

    private CoreSqlStorage(Logger logger, StorageBackend backend, HikariDataSource dataSource) {
        this.logger = logger;
        this.backend = backend;
        this.dataSource = dataSource;
    }

    public static SqlStorage open(File dataFolder, DatabaseConfig database, Logger logger) {
        purgeLegacyH2(dataFolder, logger);

        if (database == null || !database.sqlEnabled()) {
            logger.fine("Storage · NONE (YAML data/ · no SQL)");
            return new NoopSqlStorage();
        }

        try {
            HikariDataSource ds = openMysql(database.yaml());
            migrate(ds, logger);
            logger.info("Storage connected · backend=MYSQL");
            return new CoreSqlStorage(logger, StorageBackend.MYSQL, ds);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "MySQL failed · falling back to NONE (YAML data/)", ex);
            return new NoopSqlStorage();
        }
    }

    private static void purgeLegacyH2(File dataFolder, Logger logger) {
        if (dataFolder == null) return;
        File storageDir = new File(dataFolder, "storage");
        if (!storageDir.isDirectory()) return;

        File[] children = storageDir.listFiles(
            (dir, name) ->
                name != null && (name.endsWith(".mv.db") || name.endsWith(".trace.db") || name.endsWith(".lock.db"))
        );
        if (children == null) return;

        for (File child : children) {
            if (child.delete()) logger.info("Removed legacy H2 file · " + child.getName());
        }
    }

    private static void migrate(HikariDataSource ds, Logger logger) throws SQLException {
        try {
            int applied = MigrationRunner.run(
                ds,
                line -> logger.info("Schema · " + line),
                SchemaCatalog.shared(),
                SchemaCatalog.core()
            );
            if (applied == 0) logger.info("Schema · up to date");
        } catch (SQLException ex) {
            ds.close();
            throw ex;
        }
    }

    private static HikariDataSource openMysql(FileConfiguration yaml) {
        String host = yaml.getString("MYSQL.HOST", "127.0.0.1");
        int port = yaml.getInt("MYSQL.PORT", 3306);
        String database = yaml.getString("MYSQL.DATABASE", "ioricore");
        String user = yaml.getString("MYSQL.USERNAME", yaml.getString("MYSQL.USER", "root"));
        String password = yaml.getString("MYSQL.PASSWORD", "");
        boolean ssl = yaml.getBoolean("MYSQL.SSL", false);
        int poolSize = yaml.getInt("MYSQL.POOL-SIZE", yaml.getInt("MYSQL.MAX-POOL-SIZE", 5));

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("IoriCore-MySQL");
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari.setJdbcUrl(
            "jdbc:mysql://" +
                host +
                ":" +
                port +
                "/" +
                database +
                "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true" +
                (ssl ? "&useSSL=true&verifyServerCertificate=false" : "&useSSL=false")
        );
        hikari.setUsername(user);
        hikari.setPassword(password);
        hikari.setMaximumPoolSize(Math.max(1, Math.min(MAX_POOL, poolSize)));
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(15_000L);
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        return new HikariDataSource(hikari);
    }

    @Override
    public StorageBackend backend() {
        return backend;
    }

    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    @Override
    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public Connection connection() throws SQLException {
        if (!isConnected()) throw new SQLException("Storage not connected");
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (dataSource == null || dataSource.isClosed()) return;
        dataSource.close();
        dataSource = null;
        logger.info("Storage closed · backend=" + backend.name());
    }
}
