package us.iorilabs.staffreg.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import us.iorilabs.staffreg.AuditEntry;
import us.iorilabs.staffreg.AuditLog;
import us.iorilabs.staffreg.storage.StaffRegSqlRunner;

public final class SqlAuditLog implements AuditLog {

    private final StaffRegSqlRunner sql;

    public SqlAuditLog(StaffRegSqlRunner sql) {
        this.sql = sql;
    }

    @Override
    public CompletableFuture<Void> append(AuditEntry entry) {
        if (entry == null) return CompletableFuture.completedFuture(null);
        return sql.run(connection -> {
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO ip_staff_audit (account_uuid, actor_uuid, action, detail, created_at) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS
                )
            ) {
                ps.setString(1, entry.targetUuid() == null ? null : entry.targetUuid().toString());
                ps.setString(2, entry.actorUuid() == null ? null : entry.actorUuid().toString());
                ps.setString(3, entry.action() == null ? "UNKNOWN" : entry.action());
                ps.setString(4, truncate(entry.summary(), 512));
                ps.setLong(5, entry.at() == null ? Instant.now().toEpochMilli() : entry.at().toEpochMilli());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<List<AuditEntry>> listForTarget(UUID targetUuid, int limit) {
        if (targetUuid == null) return CompletableFuture.completedFuture(List.of());
        int max = Math.max(1, Math.min(200, limit));
        return sql.call(connection -> {
            List<AuditEntry> out = new ArrayList<>();
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, account_uuid, actor_uuid, action, detail, created_at FROM ip_staff_audit " +
                        "WHERE account_uuid = ? ORDER BY id DESC LIMIT ?"
                )
            ) {
                ps.setString(1, targetUuid.toString());
                ps.setInt(2, max);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AuditEntry entry = new AuditEntry()
                            .action(rs.getString("action"))
                            .summary(rs.getString("detail"))
                            .at(Instant.ofEpochMilli(rs.getLong("created_at")));
                        String account = rs.getString("account_uuid");
                        if (account != null) entry.targetUuid(UUID.fromString(account));
                        String actor = rs.getString("actor_uuid");
                        if (actor != null) entry.actorUuid(UUID.fromString(actor));
                        out.add(entry);
                    }
                }
            }
            return out;
        });
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
