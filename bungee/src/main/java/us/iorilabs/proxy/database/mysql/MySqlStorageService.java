package us.iorilabs.proxy.database.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;
import us.iorilabs.proxy.config.DatabaseConfig;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.StorageType;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.storage.MigrationRunner;
import us.iorilabs.proxy.storage.SchemaCatalog;

public final class MySqlStorageService implements StorageService {

    private static final int H2_POOL_SIZE = 4;

    private final StorageType storageType;
    private final DatabaseConfig config;
    private final Path h2File;
    private final ProxyLogger logger;
    private HikariDataSource dataSource;
    private boolean connected;

    private MySqlStorageService(StorageType storageType, DatabaseConfig config, Path h2File, ProxyLogger logger) {
        this.storageType = storageType;
        this.config = config;
        this.h2File = h2File;
        this.logger = logger;
    }

    public static MySqlStorageService mysql(DatabaseConfig config, ProxyLogger logger) {
        return new MySqlStorageService(StorageType.MYSQL, config, null, logger);
    }

    public static MySqlStorageService h2(DatabaseConfig config, Path dataDirectory, ProxyLogger logger) {
        Path file = dataDirectory.resolve("storage").resolve(config.h2File());
        return new MySqlStorageService(StorageType.H2, config, file, logger);
    }

    public void connect() throws SQLException {
        if (storageType == StorageType.H2) {
            connectH2();
        } else {
            connectMysql();
        }
    }

    private void connectH2() throws SQLException {
        try {
            Files.createDirectories(h2File.getParent());
        } catch (Exception ex) {
            throw new SQLException("Cannot create H2 directory: " + h2File.getParent(), ex);
        }

        String jdbc =
            "jdbc:h2:file:" +
            h2File.toAbsolutePath() +
            ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE";

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("IoriCore-Proxy-H2");
        hikari.setDriverClassName("org.h2.Driver");
        hikari.setJdbcUrl(jdbc);
        hikari.setUsername("sa");
        hikari.setPassword("");
        hikari.setMaximumPoolSize(H2_POOL_SIZE);
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(config.connectionTimeoutMillis());
        hikari.setMaxLifetime(config.maxLifetimeMillis());

        open(hikari);
        logger.success("H2 connected &7(file storage)");
        logger.module("SQL", "file &f" + h2File.getFileName() + ".mv.db");
    }

    private void connectMysql() throws SQLException {
        StringBuilder jdbc = new StringBuilder("jdbc:mysql://")
            .append(config.host())
            .append(':')
            .append(config.port())
            .append('/')
            .append(config.database())
            .append("?useUnicode=true&characterEncoding=UTF-8")
            .append("&serverTimezone=UTC")
            .append("&allowPublicKeyRetrieval=true");
        if (config.ssl()) {
            jdbc.append("&useSSL=true&verifyServerCertificate=false");
        } else {
            jdbc.append("&useSSL=false");
        }

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("IoriCore-Proxy-MySQL");
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari.setJdbcUrl(jdbc.toString());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(config.poolSize());
        hikari.setMinimumIdle(Math.min(config.minIdle(), config.poolSize()));
        hikari.setConnectionTimeout(config.connectionTimeoutMillis());
        hikari.setMaxLifetime(config.maxLifetimeMillis());
        hikari.setLeakDetectionThreshold(60_000L);
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        hikari.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikari.addDataSourceProperty("maintainTimeStats", "false");

        open(hikari);
        logger.success("MySQL connected &f" + config.host() + ":" + config.port() + "/" + config.database());
    }

    private void open(HikariConfig hikari) throws SQLException {
        dataSource = new HikariDataSource(hikari);
        try {
            migrate();
        } catch (SQLException ex) {
            dataSource.close();
            dataSource = null;
            throw ex;
        }

        connected = true;
    }

    private void migrate() throws SQLException {
        int legacy = legacySchemaVersion();
        if (legacy > 0) {
            logger.module("SQL", "legacy ip_schema_version v" + legacy + " · adopting into ic_schema_version");
        }

        int applied = MigrationRunner.run(
            dataSource,
            line -> logger.module("SQL", line),
            SchemaCatalog.shared(),
            SchemaCatalog.proxy()
        );
        if (applied == 0) logger.module("SQL", "schema up to date");
    }

    private int legacySchemaVersion() {
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT MAX(version) FROM ip_schema_version");
            ResultSet rs = ps.executeQuery()
        ) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            return 0;
        }
    }

    public HikariDataSource dataSource() {
        return dataSource;
    }

    public Connection connection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("SQL pool is not ready");
        }

        return dataSource.getConnection();
    }

    public <T> T call(Function<Connection, T> function) throws SQLException {
        try (Connection connection = connection()) {
            return function.apply(connection);
        }
    }

    @Override
    public StorageType type() {
        return storageType;
    }

    @Override
    public Optional<MySqlStorageService> mysql() {
        return Optional.of(this);
    }

    @Override
    public boolean isConnected() {
        return connected && dataSource != null && !dataSource.isClosed();
    }

    @Override
    public String statusLine() {
        if (!isConnected()) return "disconnected";

        if (storageType == StorageType.H2) {
            return "h2";
        }

        return "mysql " + config.host() + ":" + config.port() + "/" + config.database();
    }

    @Override
    public void close() {
        connected = false;

        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }

        logger.module("SQL", "closed");
    }
}
