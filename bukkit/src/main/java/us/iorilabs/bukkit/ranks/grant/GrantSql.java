package us.iorilabs.bukkit.ranks.grant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import us.iorilabs.api.storage.SqlStorage;
import us.iorilabs.bukkit.ranks.enums.RankScope;

@RequiredArgsConstructor
public final class GrantSql {

    private static final String COLUMNS =
        "id, player_uuid, rank_id, scope, server_key, added_by, reason, created_at, expires_at, " +
        "removed_at, removed_by, removed_reason";

    private final SqlStorage storage;
    private final Logger logger;

    public boolean available() {
        return storage != null && storage.isConnected();
    }

    public List<Grant> active(UUID playerId, long now) {
        if (!available() || playerId == null) return Collections.emptyList();

        String query =
            "SELECT " +
            COLUMNS +
            " FROM ic_grants WHERE player_uuid = ? AND removed_at IS NULL AND (expires_at IS NULL OR expires_at > ?)";
        try (Connection connection = storage.connection(); PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerId.toString());
            ps.setLong(2, now);
            return read(ps);
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Grant load failed uuid=" + playerId, ex);
            return Collections.emptyList();
        }
    }

    public List<Grant> history(UUID playerId, int limit) {
        if (!available() || playerId == null) return Collections.emptyList();

        String query = "SELECT " + COLUMNS + " FROM ic_grants WHERE player_uuid = ? ORDER BY created_at DESC";
        try (Connection connection = storage.connection(); PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerId.toString());
            ps.setMaxRows(Math.max(1, limit));
            return read(ps);
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Grant history failed uuid=" + playerId, ex);
            return Collections.emptyList();
        }
    }

    public boolean insert(Grant grant) {
        if (!available() || grant == null) return false;

        String query =
            "INSERT INTO ic_grants (player_uuid, rank_id, scope, server_key, added_by, reason, created_at, expires_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (
            Connection connection = storage.connection();
            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setString(1, grant.playerId().toString());
            ps.setString(2, grant.rankId().toString());
            ps.setString(3, grant.scope().name());
            ps.setString(4, grant.serverKey());
            ps.setString(5, grant.addedBy());
            ps.setString(6, grant.reason());
            ps.setLong(7, grant.createdAt());
            if (grant.permanent()) ps.setNull(8, Types.BIGINT);
            else ps.setLong(8, grant.expiresAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) grant.id(keys.getLong(1));
            }
            return true;
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Grant insert failed uuid=" + grant.playerId(), ex);
            return false;
        }
    }

    public int remove(UUID playerId, UUID rankId, String removedBy, String reason, long now) {
        if (!available() || playerId == null || rankId == null) return 0;

        String query =
            "UPDATE ic_grants SET removed_at = ?, removed_by = ?, removed_reason = ? " +
            "WHERE player_uuid = ? AND rank_id = ? AND removed_at IS NULL AND (expires_at IS NULL OR expires_at > ?)";
        try (Connection connection = storage.connection(); PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, now);
            ps.setString(2, removedBy);
            ps.setString(3, reason);
            ps.setString(4, playerId.toString());
            ps.setString(5, rankId.toString());
            ps.setLong(6, now);
            return ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Grant remove failed uuid=" + playerId, ex);
            return 0;
        }
    }

    private static List<Grant> read(PreparedStatement ps) throws SQLException {
        List<Grant> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Grant grant = map(rs);
                if (grant != null) out.add(grant);
            }
        }
        return out;
    }

    private static Grant map(ResultSet rs) throws SQLException {
        UUID player = parseUuid(rs.getString("player_uuid"));
        UUID rank = parseUuid(rs.getString("rank_id"));
        if (player == null || rank == null) return null;

        Grant grant = new Grant();
        grant
            .id(rs.getLong("id"))
            .playerId(player)
            .rankId(rank)
            .scope(RankScope.from(rs.getString("scope")))
            .serverKey(nullToEmpty(rs.getString("server_key")))
            .addedBy(rs.getString("added_by"))
            .reason(rs.getString("reason"))
            .createdAt(rs.getLong("created_at"))
            .expiresAt(rs.getLong("expires_at"))
            .removedAt(rs.getLong("removed_at"))
            .removedBy(rs.getString("removed_by"))
            .removedReason(rs.getString("removed_reason"));
        return grant;
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
