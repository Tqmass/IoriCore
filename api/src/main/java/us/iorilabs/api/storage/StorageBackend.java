package us.iorilabs.api.storage;

public enum StorageBackend {
    NONE,
    MYSQL,

    /** @deprecated Removed — use NONE (YAML data/) or MYSQL. */
    @Deprecated
    H2,

    MONGODB,
}
