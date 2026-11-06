package us.iorilabs.proxy.report;

import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class Report {

    private final String id;
    private final UUID reporterUuid;
    private final String reporterName;
    private final String targetName;
    private final String reason;
    private final String detail;
    private final String server;
    private final long createdAt;
    private ReportStatus status;
    private String resolverName;

    public Report(
        String id,
        UUID reporterUuid,
        String reporterName,
        String targetName,
        String reason,
        String detail,
        String server,
        long createdAt,
        ReportStatus status,
        String resolverName
    ) {
        this.id = id;
        this.reporterUuid = reporterUuid;
        this.reporterName = reporterName;
        this.targetName = targetName;
        this.reason = reason;
        this.detail = detail;
        this.server = server;
        this.createdAt = createdAt;
        this.status = status;
        this.resolverName = resolverName;
    }

    public void resolve(String resolver) {
        this.status = ReportStatus.RESOLVED;
        this.resolverName = resolver;
    }
}
