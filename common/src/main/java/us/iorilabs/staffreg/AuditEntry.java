package us.iorilabs.staffreg;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class AuditEntry {

    private UUID id;
    private String action;
    private UUID actorUuid;
    private UUID targetUuid;
    private String summary;
    private Instant at;
}
