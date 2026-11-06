package us.iorilabs.staffreg.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import us.iorilabs.staffreg.Challenge;
import us.iorilabs.staffreg.ChallengeStore;
import us.iorilabs.staffreg.StaffRegEnums.ChallengeKind;
import us.iorilabs.staffreg.StaffRegEnums.ChallengeStatus;
import us.iorilabs.staffreg.storage.StaffRegSqlRunner;

public final class SqlChallengeStore implements ChallengeStore {

    private final StaffRegSqlRunner sql;

    public SqlChallengeStore(StaffRegSqlRunner sql) {
        this.sql = sql;
    }

    @Override
    public CompletableFuture<Optional<Challenge>> findById(UUID id) {
        if (id == null) return CompletableFuture.completedFuture(Optional.empty());
        return sql.call(connection -> {
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM ip_staff_challenges WHERE id = ? LIMIT 1"
                )
            ) {
                ps.setString(1, id.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    return Optional.of(map(rs));
                }
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Challenge>> findPendingForAccount(UUID accountUuid) {
        if (accountUuid == null) return CompletableFuture.completedFuture(Optional.empty());
        return sql.call(connection -> {
            long now = Instant.now().toEpochMilli();
            try (
                PreparedStatement expire = connection.prepareStatement(
                    "UPDATE ip_staff_challenges SET status = ? WHERE account_uuid = ? AND status = ? AND expires_at < ?"
                )
            ) {
                expire.setString(1, ChallengeStatus.EXPIRED.name());
                expire.setString(2, accountUuid.toString());
                expire.setString(3, ChallengeStatus.PENDING.name());
                expire.setLong(4, now);
                expire.executeUpdate();
            }
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM ip_staff_challenges WHERE account_uuid = ? AND status = ? ORDER BY created_at DESC LIMIT 1"
                )
            ) {
                ps.setString(1, accountUuid.toString());
                ps.setString(2, ChallengeStatus.PENDING.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    return Optional.of(map(rs));
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> save(Challenge challenge) {
        if (challenge == null || challenge.id() == null) return CompletableFuture.completedFuture(null);
        return sql.run(connection -> {
            String upsert =
                "INSERT INTO ip_staff_challenges (id, account_uuid, kind, status, token_hash, discord_message_id, " +
                "channel_id, resolved_by, created_at, expires_at, resolved_at) VALUES (?,?,?,?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE kind=VALUES(kind), status=VALUES(status), token_hash=VALUES(token_hash), " +
                "discord_message_id=VALUES(discord_message_id), channel_id=VALUES(channel_id), " +
                "resolved_by=VALUES(resolved_by), expires_at=VALUES(expires_at), resolved_at=VALUES(resolved_at)";
            try (PreparedStatement ps = connection.prepareStatement(upsert)) {
                ps.setString(1, challenge.id().toString());
                ps.setString(2, challenge.accountUuid().toString());
                ps.setString(3, challenge.kind().name());
                ps.setString(4, challenge.status().name());
                ps.setString(5, challenge.tokenHash());
                ps.setString(6, challenge.discordMessageId());
                ps.setString(7, challenge.channelId());
                ps.setString(8, challenge.resolvedByUuid() == null ? null : challenge.resolvedByUuid().toString());
                ps.setLong(9, epoch(challenge.createdAt()));
                ps.setLong(10, epoch(challenge.expiresAt()));
                if (challenge.resolvedAt() == null) ps.setObject(11, null);
                else ps.setLong(11, challenge.resolvedAt().toEpochMilli());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> consumeToken(UUID challengeId, String tokenHash) {
        if (challengeId == null || tokenHash == null || tokenHash.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }
        return sql.call(connection -> {
            long now = Instant.now().toEpochMilli();
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "UPDATE ip_staff_challenges SET status = ?, resolved_at = ? " +
                        "WHERE id = ? AND status = ? AND token_hash = ? AND expires_at >= ?"
                )
            ) {
                ps.setString(1, ChallengeStatus.APPROVED.name());
                ps.setLong(2, now);
                ps.setString(3, challengeId.toString());
                ps.setString(4, ChallengeStatus.PENDING.name());
                ps.setString(5, tokenHash);
                ps.setLong(6, now);
                return ps.executeUpdate() == 1;
            }
        });
    }

    private static Challenge map(ResultSet rs) throws SQLException {
        Challenge challenge = new Challenge()
            .id(UUID.fromString(rs.getString("id")))
            .accountUuid(UUID.fromString(rs.getString("account_uuid")))
            .kind(parse(ChallengeKind.class, rs.getString("kind"), ChallengeKind.LOCATION_CHANGE))
            .status(parse(ChallengeStatus.class, rs.getString("status"), ChallengeStatus.PENDING))
            .tokenHash(rs.getString("token_hash"))
            .discordMessageId(rs.getString("discord_message_id"))
            .channelId(rs.getString("channel_id"))
            .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")))
            .expiresAt(Instant.ofEpochMilli(rs.getLong("expires_at")));
        String resolvedBy = rs.getString("resolved_by");
        if (resolvedBy != null && !resolvedBy.isBlank()) challenge.resolvedByUuid(UUID.fromString(resolvedBy));
        long resolvedAt = rs.getLong("resolved_at");
        if (!rs.wasNull()) challenge.resolvedAt(Instant.ofEpochMilli(resolvedAt));
        return challenge;
    }

    private static long epoch(Instant instant) {
        return instant == null ? Instant.now().toEpochMilli() : instant.toEpochMilli();
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String raw, E fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
