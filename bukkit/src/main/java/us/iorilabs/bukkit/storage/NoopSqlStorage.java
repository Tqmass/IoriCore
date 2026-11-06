package us.iorilabs.bukkit.storage;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import us.iorilabs.api.storage.SqlStorage;
import us.iorilabs.api.storage.StorageBackend;

public final class NoopSqlStorage implements SqlStorage {

    @Override
    public StorageBackend backend() {
        return StorageBackend.NONE;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public DataSource dataSource() {
        return null;
    }

    @Override
    public Connection connection() throws SQLException {
        throw new SQLException("SQL storage disabled (DATABASE: NONE)");
    }

    @Override
    public void close() {}
}
