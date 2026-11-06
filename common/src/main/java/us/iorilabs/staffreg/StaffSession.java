package us.iorilabs.staffreg;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import us.iorilabs.staffreg.StaffRegEnums.AuthMode;

@Getter
@Setter
@Accessors(fluent = true)
public final class StaffSession {

    private UUID sessionId;
    private UUID accountUuid;
    private String proxyId;
    private String serverId;
    private AuthMode authMode;
    private boolean authenticated;
    private boolean stepUpGranted;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant stepUpExpiresAt;
}
