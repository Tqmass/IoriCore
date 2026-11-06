package us.iorilabs.bukkit.modules.gender;

import java.util.Locale;

public enum GenderKind {
    MALE,
    FEMALE,
    OTHER,
    UNKNOWN;

    public static GenderKind parse(String raw) {
        if (raw == null || raw.isBlank()) return UNKNOWN;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "m", "male", "h", "hombre" -> MALE;
            case "f", "female", "mujer" -> FEMALE;
            case "o", "other", "n", "nb", "nonbinary", "otro" -> OTHER;
            default -> UNKNOWN;
        };
    }
}
