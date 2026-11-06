package us.iorilabs.bukkit.ranks.enums;

import java.util.Locale;

public enum LuckPermsMode {
    OFF,
    READ,
    SYNC;

    public static LuckPermsMode from(String raw) {
        if (raw == null || raw.trim().isEmpty()) return OFF;
        try {
            return LuckPermsMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return OFF;
        }
    }

    public boolean appliesAttachments() {
        return this == OFF;
    }

    public boolean ownDisplay() {
        return this != READ;
    }
}
