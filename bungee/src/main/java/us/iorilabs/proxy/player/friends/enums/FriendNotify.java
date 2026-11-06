package us.iorilabs.proxy.player.friends.enums;

public enum FriendNotify {
    JOIN,
    QUIT,
    SWITCH;

    public static FriendNotify fromString(String raw) {
        if (raw == null || raw.isBlank()) return JOIN;

        return FriendNotify.valueOf(raw.trim().toUpperCase());
    }
}
