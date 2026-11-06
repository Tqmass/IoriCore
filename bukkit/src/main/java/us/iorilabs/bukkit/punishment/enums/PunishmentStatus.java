package us.iorilabs.bukkit.punishment.enums;

public enum PunishmentStatus {
    ACTIVE,
    EXPIRED,
    REVOKED;

    public static PunishmentStatus from(String raw) {
        if (raw == null || raw.trim().isEmpty()) return ACTIVE;
        try {
            return PunishmentStatus.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return ACTIVE;
        }
    }
}
