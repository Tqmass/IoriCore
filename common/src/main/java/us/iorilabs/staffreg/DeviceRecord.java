package us.iorilabs.staffreg;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class DeviceRecord {

    private UUID id;
    private UUID accountUuid;
    private String ipHmac;
    private String country;
    private String region;
    private String city;
    private boolean vpnOrProxy;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
}
