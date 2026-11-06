package us.iorilabs.proxy.maintenance;

import java.util.Locale;

public enum MaintenanceType {
    GLOBAL,
    MODE,
    SERVER;

    public static MaintenanceType from(String raw) {
        if (raw == null || raw.isBlank()) return GLOBAL;

        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "global", "*" -> GLOBAL;
            case "mode" -> MODE;
            default -> SERVER;
        };
    }
}
