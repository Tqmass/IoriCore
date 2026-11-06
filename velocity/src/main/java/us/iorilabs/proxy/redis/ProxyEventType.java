package us.iorilabs.proxy.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import us.iorilabs.api.redis.NetworkEventType;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ProxyEventType {
    PLAYER_JOIN(NetworkEventType.PLAYER_JOIN),
    PLAYER_QUIT(NetworkEventType.PLAYER_QUIT),
    PLAYER_SWITCH(NetworkEventType.PLAYER_SWITCH),
    REDIS_TEST(NetworkEventType.REDIS_TEST),
    MAINTENANCE_UPDATE(NetworkEventType.MAINTENANCE_UPDATE),
    MAINTENANCE_WHITELIST(NetworkEventType.MAINTENANCE_WHITELIST),
    BLACKLIST_UPDATE(NetworkEventType.BLACKLIST_UPDATE),
    STAFFCHAT_MESSAGE(NetworkEventType.STAFFCHAT_MESSAGE),
    ADMINCHAT_MESSAGE(NetworkEventType.ADMINCHAT_MESSAGE),
    STREAM_BROADCAST(NetworkEventType.STREAM_BROADCAST),
    ANNOUNCEMENT_RELOAD(NetworkEventType.ANNOUNCEMENT_RELOAD),
    REPORT_CREATED(NetworkEventType.REPORT_CREATED),
    REPORT_RESOLVED(NetworkEventType.REPORT_RESOLVED),
    HELPOP_CREATED(NetworkEventType.HELPOP_CREATED),
    HELPOP_REPLY(NetworkEventType.HELPOP_REPLY),
    DONATION_BROADCAST(NetworkEventType.DONATION_BROADCAST),
    GGWAVE_START(NetworkEventType.GGWAVE_START),
    GGWAVE_CHAT(NetworkEventType.GGWAVE_CHAT),
    FRIEND_REQUEST(NetworkEventType.FRIEND_REQUEST),
    FRIEND_ACCEPT(NetworkEventType.FRIEND_ACCEPT),
    FRIEND_REMOVE(NetworkEventType.FRIEND_REMOVE),
    FRIEND_NOTIFY(NetworkEventType.FRIEND_NOTIFY),
    PUNISHMENT_APPLIED(NetworkEventType.PUNISHMENT_APPLIED),
    PUNISHMENT_INVALIDATE(NetworkEventType.PUNISHMENT_INVALIDATE),
    RANK_UPDATE(NetworkEventType.RANK_UPDATE),
    GRANT_UPDATE(NetworkEventType.GRANT_UPDATE),
    STAFFMODE_UPDATE(NetworkEventType.STAFFMODE_UPDATE),
    VANISH_UPDATE(NetworkEventType.VANISH_UPDATE),
    FREEZE_UPDATE(NetworkEventType.FREEZE_UPDATE),
    CLIENT_PROTOCOL(NetworkEventType.CLIENT_PROTOCOL),
    NETWORK_BROADCAST(NetworkEventType.NETWORK_BROADCAST),
    NETWORK_MESSAGE(NetworkEventType.NETWORK_MESSAGE),
    DISCORD_LINK_UPDATE(NetworkEventType.DISCORD_LINK_UPDATE),
    STAFF_AUTH_REQUEST(NetworkEventType.STAFF_AUTH_REQUEST),
    STAFF_AUTH_RESPONSE(NetworkEventType.STAFF_AUTH_RESPONSE),
    STAFF_AUTH_SESSION(NetworkEventType.STAFF_AUTH_SESSION);

    private final NetworkEventType network;

    public String id() {
        return network.id();
    }

    public static ProxyEventType fromId(String raw) {
        NetworkEventType type = NetworkEventType.fromId(raw);
        if (type == null) throw new IllegalArgumentException("Unknown Redis event type: " + raw);
        return valueOf(type.name());
    }
}
