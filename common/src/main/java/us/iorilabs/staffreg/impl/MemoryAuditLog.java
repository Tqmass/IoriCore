package us.iorilabs.staffreg.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import us.iorilabs.staffreg.AuditEntry;
import us.iorilabs.staffreg.AuditLog;

public final class MemoryAuditLog implements AuditLog {

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public CompletableFuture<Void> append(AuditEntry entry) {
        if (entry != null) entries.add(entry);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<AuditEntry>> listForTarget(UUID targetUuid, int limit) {
        List<AuditEntry> out = new ArrayList<>();
        if (targetUuid == null) return CompletableFuture.completedFuture(out);
        int max = Math.max(1, limit);
        for (int i = entries.size() - 1; i >= 0 && out.size() < max; i--) {
            AuditEntry entry = entries.get(i);
            if (targetUuid.equals(entry.targetUuid())) out.add(entry);
        }
        return CompletableFuture.completedFuture(out);
    }
}
