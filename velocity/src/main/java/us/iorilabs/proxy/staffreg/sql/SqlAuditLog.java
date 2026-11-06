package us.iorilabs.proxy.staffreg.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.staffreg.AuditEntry;
import us.iorilabs.staffreg.AuditLog;

public final class SqlAuditLog implements AuditLog {

    private final StorageService storage;
    private final ProxyLogger logger;

    public SqlAuditLog(StorageService storage, ProxyLogger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Void> append(AuditEntry entry) {
        return CompletableFuture.runAsync(() -> {
            if (entry == null || entry.action() == null) return;
            Optional<MySqlStorageService> sql = sql();
            if (sql.isEmpty()) return;

            UUID id = entry.id() == null ? UUID.randomUUID() : entry.id();
            long at = entry.at() == null ? System.currentTimeMillis() : entry.at().toEpochMilli();

            try (
                Connection connection = sql.get().connection();
                PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO ip_staff_audit (account_uuid, actor_uuid, action, detail, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """
                )
            ) {
                ps.setString(1, entry.targetUuid() == null ? null : entry.targetUuid().toString());
                ps.setString(2, entry.actorUuid() == null ? null : entry.actorUuid().toString());
                ps.setString(3, entry.action());
                ps.setString(4, entry.summary() == null ? "" : entry.summary());
                ps.setLong(5, at);
                ps.executeUpdate();
            } catch (Exception ex) {
                logger.warn("StaffReg audit append failed: " + ex.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<AuditEntry>> listForTarget(UUID targetUuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<AuditEntry> out = new ArrayList<>();
            if (targetUuid == null) return out;
            Optional<MySqlStorageService> sql = sql();
            if (sql.isEmpty()) return out;

            int max = Math.max(1, Math.min(limit, 100));
            try (
                Connection connection = sql.get().connection();
                PreparedStatement ps = connection.prepareStatement(
                    """
                    SELECT action, actor_uuid, account_uuid, detail, created_at
                    FROM ip_staff_audit WHERE account_uuid = ? ORDER BY created_at DESC LIMIT ?
                    """
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
                        String actor = rs.getString("actor_uuid");
                        if (actor != null && !actor.isBlank()) entry.actorUuid(UUID.fromString(actor));
                        entry.targetUuid(targetUuid);
                        out.add(entry);
                    }
                }
            } catch (Exception ex) {
                logger.warn("StaffReg audit list failed: " + ex.getMessage());
            }
            return out;
        });
    }

    private Optional<MySqlStorageService> sql() {
        if (storage == null || !storage.isConnected()) return Optional.empty();
        return storage.mysql();
    }
}
