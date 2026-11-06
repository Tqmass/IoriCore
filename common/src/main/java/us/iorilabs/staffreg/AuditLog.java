package us.iorilabs.staffreg;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AuditLog {
    CompletableFuture<Void> append(AuditEntry entry);

    CompletableFuture<List<AuditEntry>> listForTarget(UUID targetUuid, int limit);
}
