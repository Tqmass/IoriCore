package us.iorilabs.bukkit.ranks.enums;

public enum RankType {
    NORMAL,
    STAFF,
    DONOR,
    MEDIA;

    public static RankType from(String raw) {
        if (raw == null || raw.trim().isEmpty()) return NORMAL;
        try {
            return RankType.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return NORMAL;
        }
    }

    public String display() {
        String name = name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
