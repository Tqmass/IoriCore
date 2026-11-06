package us.iorilabs.proxy.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

public enum SqlDialect {
    MYSQL,
    H2;

    public static SqlDialect detect(Connection connection) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName();
        if (product != null && product.toLowerCase(Locale.ROOT).contains("h2")) return H2;
        return MYSQL;
    }
}
