package us.iorilabs.proxy.discord.enums;

import java.util.Locale;

public enum DiscordPunishmentType {
    BAN(false, true),
    TEMPBAN(true, true),
    MUTE(false, false),
    TEMPMUTE(true, false),
    WARN(false, false),
    KICK(false, false);

    private final boolean requiresDuration;
    private final boolean disconnect;

    DiscordPunishmentType(boolean requiresDuration, boolean disconnect) {
        this.requiresDuration = requiresDuration;
        this.disconnect = disconnect;
    }

    public boolean requiresDuration() {
        return requiresDuration;
    }

    public boolean disconnect() {
        return disconnect;
    }

    public String sqlType() {
        return name();
    }

    public static DiscordPunishmentType fromName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return DiscordPunishmentType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
