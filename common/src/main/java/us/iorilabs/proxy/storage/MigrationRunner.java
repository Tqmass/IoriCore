package us.iorilabs.proxy.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.sql.DataSource;

public final class MigrationRunner {

    private static final String LOCK_NAME = "ioricore_schema";
    private static final int LOCK_TIMEOUT_SECONDS = 30;
    private static final Object LOCAL_LOCK = new Object();

    private MigrationRunner() {}

    @SafeVarargs
    public static int run(DataSource dataSource, Consumer<String> log, List<Migration>... catalogs)
        throws SQLException {
        List<Migration> migrations = new ArrayList<>();
        for (List<Migration> catalog : catalogs) migrations.addAll(catalog);

        synchronized (LOCAL_LOCK) {
            try (Connection connection = dataSource.getConnection()) {
                SqlDialect dialect = SqlDialect.detect(connection);
                boolean locked = acquire(connection, dialect);
                try {
                    return apply(connection, dialect, migrations, log);
                } finally {
                    if (locked) release(connection);
                }
            }
        }
    }

    private static int apply(
        Connection connection,
        SqlDialect dialect,
        List<Migration> migrations,
        Consumer<String> log
    ) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS ic_schema_version (
                  component VARCHAR(32) NOT NULL,
                  version INT NOT NULL,
                  description VARCHAR(128),
                  applied_at BIGINT NOT NULL,
                  PRIMARY KEY (component, version)
                )
                """
            );
        }

        Set<String> applied = loadApplied(connection);
        int count = 0;

        for (Migration migration : migrations) {
            if (applied.contains(key(migration.component(), migration.version()))) continue;

            for (MigrationStep step : migration.steps()) {
                step.apply(connection, dialect);
            }

            record(connection, migration);
            log.accept(
                "applied " + migration.component() + " v" + migration.version() + " (" + migration.description() + ")"
            );
            count++;
        }

        return count;
    }

    private static Set<String> loadApplied(Connection connection) throws SQLException {
        Set<String> applied = new HashSet<>();
        try (
            PreparedStatement ps = connection.prepareStatement("SELECT component, version FROM ic_schema_version");
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) applied.add(key(rs.getString(1), rs.getInt(2)));
        }
        return applied;
    }

    private static void record(Connection connection, Migration migration) throws SQLException {
        try (
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO ic_schema_version (component, version, description, applied_at) VALUES (?, ?, ?, ?)"
            )
        ) {
            ps.setString(1, migration.component());
            ps.setInt(2, migration.version());
            ps.setString(3, migration.description());
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException ignored) {
            // ~ otro nodo la registró en paralelo; los pasos son idempotentes
        } catch (SQLException ex) {
            if (ex.getSQLState() == null || !ex.getSQLState().startsWith("23")) throw ex;
        }
    }

    private static boolean acquire(Connection connection, SqlDialect dialect) throws SQLException {
        if (dialect != SqlDialect.MYSQL) return false;

        try (PreparedStatement ps = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            ps.setString(1, LOCK_NAME);
            ps.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 1) return true;
            }
        }
        throw new SQLException("Schema lock timeout (" + LOCK_NAME + ")");
    }

    private static void release(Connection connection) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            ps.setString(1, LOCK_NAME);
            ps.executeQuery().close();
        } catch (SQLException ignored) {}
    }

    private static String key(String component, int version) {
        return component + "#" + version;
    }
}
