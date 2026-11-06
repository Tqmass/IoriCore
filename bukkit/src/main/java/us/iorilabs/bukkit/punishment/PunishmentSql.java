package us.iorilabs.bukkit.punishment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import us.iorilabs.api.punishment.PunishmentType;
import us.iorilabs.api.storage.SqlStorage;
import us.iorilabs.bukkit.punishment.enums.PunishmentStatus;

public final class PunishmentSql {

    private final SqlStorage storage;
    private final Logger logger;

    public PunishmentSql(SqlStorage storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public Optional<Punishment> load(long id) {
        if (storage == null || !storage.isConnected() || id <= 0L) return Optional.empty();

        try (
            Connection connection = storage.connection();
            PreparedStatement ps = connection.prepareStatement(
                "SELECT id, type, status, target_uuid, target_name, actor_uuid, actor_name, reason, created_at, expires_at, silent " +
                    "FROM ic_punishments WHERE id = ? LIMIT 1"
            )
        ) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Punishment load failed id=" + id, ex);
            return Optional.empty();
        }
    }

    public Optional<Punishment> loadActiveBan(UUID targetId) {
        return loadActive(targetId, PunishmentType.BAN, PunishmentType.TEMPBAN);
    }

    public Optional<Punishment> loadActiveMute(UUID targetId) {
        return loadActive(targetId, PunishmentType.MUTE, PunishmentType.TEMPMUTE);
    }

    public Optional<Punishment> loadActiveWarn(UUID targetId) {
        return loadActive(targetId, PunishmentType.WARN, PunishmentType.WARN);
    }

    private Optional<Punishment> loadActive(UUID targetId, PunishmentType primary, PunishmentType secondary) {
        if (storage == null || !storage.isConnected() || targetId == null) return Optional.empty();

        try (
            Connection connection = storage.connection();
            PreparedStatement ps = connection.prepareStatement(
                "SELECT id, type, status, target_uuid, target_name, actor_uuid, actor_name, reason, created_at, expires_at, silent " +
                    "FROM ic_punishments WHERE target_uuid = ? AND status = ? AND type IN (?, ?) " +
                    "ORDER BY created_at DESC LIMIT 1"
            )
        ) {
            ps.setString(1, targetId.toString());
            ps.setString(2, PunishmentStatus.ACTIVE.name());
            ps.setString(3, primary.name());
            ps.setString(4, secondary.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Punishment active load failed for " + targetId, ex);
            return Optional.empty();
        }
    }

    public void upsert(Punishment punishment) {
        if (storage == null || !storage.isConnected() || punishment == null) return;

        if (punishment.id() > 0L) {
            update(punishment);
            return;
        }
        insert(punishment);
    }

    private void insert(Punishment punishment) {
        try (
            Connection connection = storage.connection();
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO ic_punishments (type, status, target_uuid, target_name, actor_uuid, actor_name, reason, created_at, expires_at, silent) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
        ) {
            bind(ps, punishment);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) punishment.id(keys.getLong(1));
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Punishment insert failed", ex);
        }
    }

    private void update(Punishment punishment) {
        try (
            Connection connection = storage.connection();
            PreparedStatement ps = connection.prepareStatement(
                "UPDATE ic_punishments SET type = ?, status = ?, target_uuid = ?, target_name = ?, actor_uuid = ?, " +
                    "actor_name = ?, reason = ?, created_at = ?, expires_at = ?, silent = ? WHERE id = ?"
            )
        ) {
            bind(ps, punishment);
            ps.setLong(11, punishment.id());
            ps.executeUpdate();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Punishment update failed id=" + punishment.id(), ex);
        }
    }

    private static void bind(PreparedStatement ps, Punishment punishment) throws Exception {
        ps.setString(1, punishment.type() == null ? PunishmentType.WARN.name() : punishment.type().name());
        ps.setString(2, punishment.status() == null ? PunishmentStatus.ACTIVE.name() : punishment.status().name());
        ps.setString(3, punishment.targetId() == null ? "" : punishment.targetId().toString());
        ps.setString(4, punishment.targetName());
        if (punishment.actorId() == null) {
            ps.setNull(5, Types.VARCHAR);
        } else {
            ps.setString(5, punishment.actorId().toString());
        }
        ps.setString(6, punishment.actorName());
        ps.setString(7, punishment.reason());
        ps.setLong(8, punishment.createdAt());
        if (punishment.expiresAt() <= 0L) {
            ps.setNull(9, Types.BIGINT);
        } else {
            ps.setLong(9, punishment.expiresAt());
        }
        ps.setInt(10, punishment.silent() ? 1 : 0);
    }

    private static Punishment map(ResultSet rs) throws Exception {
        Punishment punishment = new Punishment();
        punishment
            .id(rs.getLong("id"))
            .type(parseType(rs.getString("type")))
            .status(PunishmentStatus.from(rs.getString("status")))
            .targetId(parseUuid(rs.getString("target_uuid")))
            .targetName(rs.getString("target_name"))
            .actorId(parseUuid(rs.getString("actor_uuid")))
            .actorName(rs.getString("actor_name"))
            .reason(rs.getString("reason"))
            .createdAt(rs.getLong("created_at"))
            .expiresAt(rs.getLong("expires_at"))
            .silent(rs.getInt("silent") == 1);
        return punishment;
    }

    private static PunishmentType parseType(String raw) {
        if (raw == null || raw.trim().isEmpty()) return PunishmentType.WARN;
        try {
            return PunishmentType.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return PunishmentType.WARN;
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
