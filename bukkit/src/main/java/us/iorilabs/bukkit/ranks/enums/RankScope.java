package us.iorilabs.bukkit.ranks.enums;

public enum RankScope {
    GLOBAL,
    SERVER;

    public static RankScope from(String raw) {
        if (raw == null || raw.trim().isEmpty()) return GLOBAL;
        try {
            return RankScope.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return GLOBAL;
        }
    }
}
