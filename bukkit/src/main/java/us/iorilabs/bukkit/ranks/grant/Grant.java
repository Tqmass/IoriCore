package us.iorilabs.bukkit.ranks.grant;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import us.iorilabs.bukkit.ranks.enums.RankScope;

@Getter
@Setter
@Accessors(fluent = true)
public final class Grant {

    private long id;
    private UUID playerId;
    private UUID rankId;
    private RankScope scope = RankScope.GLOBAL;
    private String serverKey = "";
    private String addedBy;
    private String reason;
    private long createdAt;
    private long expiresAt;
    private long removedAt;
    private String removedBy;
    private String removedReason;

    public boolean permanent() {
        return expiresAt <= 0L;
    }

    public boolean removed() {
        return removedAt > 0L;
    }

    public boolean expired(long now) {
        return !permanent() && expiresAt <= now;
    }

    public boolean active(long now, String server) {
        if (removed() || expired(now)) return false;
        return scope == RankScope.GLOBAL || serverKey.equalsIgnoreCase(server);
    }
}
