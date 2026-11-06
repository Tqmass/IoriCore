package us.iorilabs.proxy.storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

@FunctionalInterface
public interface MigrationStep {
    void apply(Connection connection, SqlDialect dialect) throws SQLException;

    static MigrationStep sql(String statement) {
        return (connection, dialect) -> execute(connection, statement);
    }

    static MigrationStep index(String table, String name, String columns) {
        return (connection, dialect) -> {
            if (indexExists(connection, table, name)) return;
            executeTolerant(connection, "CREATE INDEX " + name + " ON " + table + " (" + columns + ")");
        };
    }

    static MigrationStep column(String table, String column, String definition) {
        return (connection, dialect) -> {
            if (columnExists(connection, table, column)) return;
            executeTolerant(connection, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        };
    }

    static MigrationStep widenVarchar(String table, String column, int length) {
        return (connection, dialect) -> {
            String statement =
                dialect == SqlDialect.H2
                    ? "ALTER TABLE " + table + " ALTER COLUMN " + column + " VARCHAR(" + length + ") NOT NULL"
                    : "ALTER TABLE " + table + " MODIFY COLUMN " + column + " VARCHAR(" + length + ") NOT NULL";
            executeTolerant(connection, statement);
        };
    }

    private static void execute(Connection connection, String statement) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(statement);
        }
    }

    private static void executeTolerant(Connection connection, String statement) throws SQLException {
        try {
            execute(connection, statement);
        } catch (SQLException ex) {
            if (!alreadyExists(ex)) throw ex;
        }
    }

    private static boolean alreadyExists(SQLException ex) {
        int code = ex.getErrorCode();
        if (code == 1060 || code == 1061 || code == 42111 || code == 42121) return true;
        String message = ex.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("already exists");
    }

    private static boolean indexExists(Connection connection, String table, String index) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        for (String name : candidates(table)) {
            try (ResultSet rs = meta.getIndexInfo(connection.getCatalog(), null, name, false, false)) {
                while (rs.next()) {
                    String found = rs.getString("INDEX_NAME");
                    if (found != null && found.equalsIgnoreCase(index)) return true;
                }
            }
        }
        return false;
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        for (String name : candidates(table)) {
            try (ResultSet rs = meta.getColumns(connection.getCatalog(), null, name, null)) {
                while (rs.next()) {
                    String found = rs.getString("COLUMN_NAME");
                    if (found != null && found.equalsIgnoreCase(column)) return true;
                }
            }
        }
        return false;
    }

    private static String[] candidates(String table) {
        return new String[] { table.toLowerCase(Locale.ROOT), table.toUpperCase(Locale.ROOT) };
    }
}
