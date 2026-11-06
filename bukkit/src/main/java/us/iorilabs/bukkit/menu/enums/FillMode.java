package us.iorilabs.bukkit.menu.enums;

import java.util.Locale;

public enum FillMode {
    NONE,
    FILL,
    BORDER;

    public static FillMode parse(String raw, FillMode fallback) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
