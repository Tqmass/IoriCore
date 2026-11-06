package us.iorilabs.proxy.discord.enums;

import java.util.Locale;

public enum DiscordChannelKey {
    STAFF_CHAT("staff-chat"),
    PUNISHMENTS("punishments"),
    WELCOME("welcome"),
    GOODBYE("goodbye"),
    BOOST("boost"),
    STORE("store"),
    STATUS("status"),
    STAFF_AUTH("staff-auth");

    private final String id;

    DiscordChannelKey(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static DiscordChannelKey fromId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (DiscordChannelKey key : values()) {
            if (key.id.equals(normalized)) return key;
        }
        return null;
    }
}
