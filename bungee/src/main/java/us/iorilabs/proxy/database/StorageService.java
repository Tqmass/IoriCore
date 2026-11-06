package us.iorilabs.proxy.database;

import java.util.Optional;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;

public interface StorageService extends AutoCloseable {
    StorageType type();

    boolean isConnected();

    String statusLine();

    default Optional<MySqlStorageService> mysql() {
        return Optional.empty();
    }

    @Override
    void close();
}
