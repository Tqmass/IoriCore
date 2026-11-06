package us.iorilabs.api.storage;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public interface SqlStorage extends AutoCloseable {
    StorageBackend backend();

    boolean isConnected();

    DataSource dataSource();

    Connection connection() throws SQLException;

    @Override
    void close();
}
