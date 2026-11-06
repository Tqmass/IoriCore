package us.iorilabs.proxy.maintenance;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class MaintenanceState {

    private final MaintenanceType scopeType;
    private final String scopeId;
    private final boolean active;
    private final String reason;
    private final long startAt;
    private final long endAt;
    private final String enabledBy;

    public MaintenanceState(
        MaintenanceType scopeType,
        String scopeId,
        boolean active,
        String reason,
        long startAt,
        long endAt,
        String enabledBy
    ) {
        this.scopeType = scopeType;
        this.scopeId = scopeId == null ? "global" : scopeId;
        this.active = active;
        this.reason = reason == null ? "" : reason;
        this.startAt = startAt;
        this.endAt = endAt;
        this.enabledBy = enabledBy == null ? "SYSTEM" : enabledBy;
    }

    public boolean isCurrentlyActive(long now) {
        if (!active) return false;
        if (startAt > 0 && now < startAt) return false;
        if (endAt > 0 && now > endAt) return false;

        return true;
    }

    public String key() {
        return scopeType.name().toLowerCase() + ":" + scopeId.toLowerCase();
    }
}
