package us.iorilabs.bukkit.modules.core.tpa;

import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class TpaRequest {

    private final UUID requester;
    private final UUID target;
    private final boolean here;
    private final long expiresAt;

    public TpaRequest(UUID requester, UUID target, boolean here, long expiresAt) {
        this.requester = requester;
        this.target = target;
        this.here = here;
        this.expiresAt = expiresAt;
    }

    public boolean expired(long now) {
        return now > expiresAt;
    }
}
