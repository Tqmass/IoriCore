package us.iorilabs.bukkit.ranks.enums;

import java.util.Locale;

public enum RankFlag {
    STAFF,
    ADMIN,
    MEDIA,
    DONOR,
    HIDDEN;

    public static RankFlag from(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return RankFlag.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
