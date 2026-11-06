package us.iorilabs.proxy.database;

public final class NoopStorageService implements StorageService {

    @Override
    public StorageType type() {
        return StorageType.NONE;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public String statusLine() {
        return "disabled";
    }

    @Override
    public void close() {}
}
