package us.iorilabs.api.redis;

import java.util.Locale;

public enum NetworkEventType {
    PLAYER_JOIN,
    PLAYER_QUIT,
    PLAYER_SWITCH,
    REDIS_TEST,
    MAINTENANCE_UPDATE,
    MAINTENANCE_WHITELIST,
    BLACKLIST_UPDATE,
    STAFFCHAT_MESSAGE,
    ADMINCHAT_MESSAGE,
    STREAM_BROADCAST,
    ANNOUNCEMENT_RELOAD,
    REPORT_CREATED,
    REPORT_RESOLVED,
    HELPOP_CREATED,
    HELPOP_REPLY,
    DONATION_BROADCAST,
    GGWAVE_START,
    GGWAVE_CHAT,
    FRIEND_REQUEST,
    FRIEND_ACCEPT,
    FRIEND_REMOVE,
    FRIEND_NOTIFY,
    PUNISHMENT_APPLIED,
    PUNISHMENT_INVALIDATE,
    RANK_UPDATE,
    GRANT_UPDATE,
    STAFFMODE_UPDATE,
    VANISH_UPDATE,
    FREEZE_UPDATE,
    CLIENT_PROTOCOL,
    NETWORK_BROADCAST,
    NETWORK_MESSAGE,
    DISCORD_LINK_UPDATE,
    STAFF_AUTH_REQUEST,
    STAFF_AUTH_RESPONSE,
    STAFF_AUTH_SESSION;

    public String id() {
        return name();
    }

    public static NetworkEventType fromId(String raw) {
        if (raw == null) return null;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
