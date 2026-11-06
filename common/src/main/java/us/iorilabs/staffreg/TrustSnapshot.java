package us.iorilabs.staffreg;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class TrustSnapshot {

    private String ipHmac;
    private String country;
    private String region;
    private String city;
    private boolean vpnOrProxy;
    private Instant establishedAt;
    private Instant expiresAt;
}
