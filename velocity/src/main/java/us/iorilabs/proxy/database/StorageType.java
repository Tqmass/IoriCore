package us.iorilabs.proxy.database;

import java.util.Locale;

public enum StorageType {
    NONE,

    /** @deprecated Use NONE (YAML on Core) or MYSQL. */
    @Deprecated
    H2,

    MYSQL,
    MONGODB;

    public static StorageType from(String raw) {
        if (raw == null || raw.isBlank()) return NONE;

        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> MYSQL;
            case "mongo", "mongodb" -> MONGODB;
            case "h2", "file", "local" -> NONE;
            case "none", "off", "disabled", "yaml" -> NONE;
            default -> NONE;
        };
    }

    public static boolean known(String raw) {
        if (raw == null || raw.isBlank()) return true;

        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case
                "h2",
                "file",
                "local",
                "mysql",
                "mariadb",
                "mongo",
                "mongodb",
                "none",
                "off",
                "disabled",
                "yaml" -> true;
            default -> false;
        };
    }
}
