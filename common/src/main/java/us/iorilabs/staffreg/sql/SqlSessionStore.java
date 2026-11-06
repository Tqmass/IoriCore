package us.iorilabs.staffreg.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import us.iorilabs.staffreg.SessionStore;
import us.iorilabs.staffreg.StaffRegEnums.AuthMode;
import us.iorilabs.staffreg.StaffSession;
import us.iorilabs.staffreg.storage.StaffRegSqlRunner;

public final class SqlSessionStore implements SessionStore {

    private final StaffRegSqlRunner sql;

    public SqlSessionStore(StaffRegSqlRunner sql) {
        this.sql = sql;
    }

    @Override
    public CompletableFuture<Optional<StaffSession>> find(UUID sessionId) {
        if (sessionId == null) return CompletableFuture.completedFuture(Optional.empty());
        return sql.call(connection -> {
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM ip_staff_sessions WHERE id = ? LIMIT 1"
                )
            ) {
                ps.setString(1, sessionId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    return Optional.of(map(rs));
                }
            }
        });
    }

    @Override
    public CompletableFuture<List<StaffSession>> listForAccount(UUID accountUuid) {
        if (accountUuid == null) return CompletableFuture.completedFuture(List.of());
        return sql.call(connection -> {
            List<StaffSession> out = new ArrayList<>();
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM ip_staff_sessions WHERE account_uuid = ? AND (revoked_at IS NULL) " +
                        "AND (authenticated = 1 OR authenticated IS NULL)"
                )
            ) {
                ps.setString(1, accountUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(map(rs));
                }
            } catch (SQLException ex) {
                if (!missingColumn(ex)) throw ex;
                try (
                    PreparedStatement ps = connection.prepareStatement(
                        "SELECT * FROM ip_staff_sessions WHERE account_uuid = ? AND revoked_at IS NULL"
                    )
                ) {
                    ps.setString(1, accountUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) out.add(mapLegacy(rs));
                    }
                }
            }
            return out;
        });
    }

    @Override
    public CompletableFuture<Void> save(StaffSession session) {
        if (session == null || session.sessionId() == null) return CompletableFuture.completedFuture(null);
        return sql.run(connection -> upsert(connection, session));
    }

    @Override
    public CompletableFuture<Void> revokeAll(UUID accountUuid) {
        if (accountUuid == null) return CompletableFuture.completedFuture(null);
        return sql.run(connection -> {
            long now = Instant.now().toEpochMilli();
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "UPDATE ip_staff_sessions SET revoked_at = ?, authenticated = 0, step_up_granted = 0, " +
                        "step_up_expires_at = NULL WHERE account_uuid = ? AND revoked_at IS NULL"
                )
            ) {
                ps.setLong(1, now);
                ps.setString(2, accountUuid.toString());
                ps.executeUpdate();
            } catch (SQLException ex) {
                if (!missingColumn(ex)) throw ex;
                try (
                    PreparedStatement ps = connection.prepareStatement(
                        "UPDATE ip_staff_sessions SET revoked_at = ? WHERE account_uuid = ? AND revoked_at IS NULL"
                    )
                ) {
                    ps.setLong(1, now);
                    ps.setString(2, accountUuid.toString());
                    ps.executeUpdate();
                }
            }
        });
    }

    private static void upsert(Connection connection, StaffSession session) throws SQLException {
        String upsert =
            "INSERT INTO ip_staff_sessions (id, account_uuid, created_at, expires_at, revoked_at, ip_hmac, " +
            "server_name, proxy_id, auth_mode, authenticated, step_up_granted, step_up_expires_at) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE expires_at=VALUES(expires_at), " +
            "revoked_at=VALUES(revoked_at), server_name=VALUES(server_name), proxy_id=VALUES(proxy_id), " +
            "auth_mode=VALUES(auth_mode), authenticated=VALUES(authenticated), " +
            "step_up_granted=VALUES(step_up_granted), step_up_expires_at=VALUES(step_up_expires_at)";
        try (PreparedStatement ps = connection.prepareStatement(upsert)) {
            bindFull(ps, session);
            ps.executeUpdate();
        } catch (SQLException ex) {
            if (!missingColumn(ex)) throw ex;
            String legacy =
                "INSERT INTO ip_staff_sessions (id, account_uuid, created_at, expires_at, revoked_at, ip_hmac, " +
                "server_name) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE expires_at=VALUES(expires_at), " +
                "revoked_at=VALUES(revoked_at), server_name=VALUES(server_name)";
            try (PreparedStatement ps = connection.prepareStatement(legacy)) {
                ps.setString(1, session.sessionId().toString());
                ps.setString(2, session.accountUuid().toString());
                ps.setLong(3, epoch(session.createdAt()));
                ps.setObject(4, session.expiresAt() == null ? null : session.expiresAt().toEpochMilli());
                ps.setObject(5, session.authenticated() ? null : Instant.now().toEpochMilli());
                ps.setString(6, null);
                ps.setString(7, session.serverId());
                ps.executeUpdate();
            }
        }
    }

    private static void bindFull(PreparedStatement ps, StaffSession session) throws SQLException {
        ps.setString(1, session.sessionId().toString());
        ps.setString(2, session.accountUuid().toString());
        ps.setLong(3, epoch(session.createdAt()));
        ps.setObject(4, session.expiresAt() == null ? null : session.expiresAt().toEpochMilli());
        ps.setObject(5, session.authenticated() ? null : Instant.now().toEpochMilli());
        ps.setString(6, null);
        ps.setString(7, session.serverId());
        ps.setString(8, session.proxyId());
        ps.setString(9, session.authMode() == null ? null : session.authMode().name());
        ps.setBoolean(10, session.authenticated());
        ps.setBoolean(11, session.stepUpGranted());
        ps.setObject(12, session.stepUpExpiresAt() == null ? null : session.stepUpExpiresAt().toEpochMilli());
    }

    private static StaffSession map(ResultSet rs) throws SQLException {
        try {
            StaffSession session = mapLegacy(rs);
            session.proxyId(rs.getString("proxy_id"));
            String mode = rs.getString("auth_mode");
            if (mode != null && !mode.isBlank()) {
                session.authMode(AuthMode.valueOf(mode.trim().toUpperCase(Locale.ROOT)));
            }
            session.authenticated(rs.getBoolean("authenticated"));
            session.stepUpGranted(rs.getBoolean("step_up_granted"));
            long step = rs.getLong("step_up_expires_at");
            if (!rs.wasNull()) session.stepUpExpiresAt(Instant.ofEpochMilli(step));
            long revoked = rs.getLong("revoked_at");
            if (!rs.wasNull()) session.authenticated(false);
            return session;
        } catch (SQLException ex) {
            if (missingColumn(ex)) return mapLegacy(rs);
            throw ex;
        }
    }

    private static StaffSession mapLegacy(ResultSet rs) throws SQLException {
        StaffSession session = new StaffSession()
            .sessionId(UUID.fromString(rs.getString("id")))
            .accountUuid(UUID.fromString(rs.getString("account_uuid")))
            .serverId(rs.getString("server_name"))
            .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")));
        long expires = rs.getLong("expires_at");
        if (!rs.wasNull()) session.expiresAt(Instant.ofEpochMilli(expires));
        long revoked = rs.getLong("revoked_at");
        session.authenticated(rs.wasNull());
        return session;
    }

    private static boolean missingColumn(SQLException ex) {
        String message = ex.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("unknown column");
    }

    private static long epoch(Instant instant) {
        return instant == null ? Instant.now().toEpochMilli() : instant.toEpochMilli();
    }
}
