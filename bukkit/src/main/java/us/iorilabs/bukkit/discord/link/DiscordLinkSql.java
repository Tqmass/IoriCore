package us.iorilabs.bukkit.discord.link;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.api.storage.SqlStorage;
import us.iorilabs.bukkit.logging.BukkitLogger;

public final class DiscordLinkSql {

    private final SqlStorage storage;
    private final BukkitLogger logger;

    public DiscordLinkSql(SqlStorage storage, BukkitLogger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public boolean available() {
        return storage != null && storage.isConnected();
    }

    public Optional<String> discordId(UUID uuid) {
        if (!available() || uuid == null) return Optional.empty();

        try (
            Connection connection = storage.connection();
            PreparedStatement ps = connection.prepareStatement("SELECT discord_id FROM ic_discord_links WHERE uuid = ?")
        ) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException ex) {
            logger.warn("Discord link load failed: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<UUID> uuidByDiscord(String discordId) {
        if (!available() || discordId == null || discordId.isBlank()) return Optional.empty();

        try (
            Connection connection = storage.connection();
            PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid FROM ic_discord_links WHERE discord_id = ? LIMIT 1"
            )
        ) {
            ps.setString(1, discordId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(UUID.fromString(rs.getString(1))) : Optional.empty();
            }
        } catch (SQLException | IllegalArgumentException ex) {
            logger.warn("Discord link lookup failed: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean link(UUID uuid, String discordId, long now) {
        if (!available() || uuid == null || discordId == null || discordId.isBlank()) return false;

        try (Connection connection = storage.connection()) {
            boolean auto = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (
                    PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM ic_discord_links WHERE uuid = ? OR discord_id = ?"
                    )
                ) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, discordId.trim());
                    ps.executeUpdate();
                }
                try (
                    PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO ic_discord_links (uuid, discord_id, linked_at) VALUES (?, ?, ?)"
                    )
                ) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, discordId.trim());
                    ps.setLong(3, now);
                    ps.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(auto);
            }
        } catch (SQLException ex) {
            logger.warn("Discord link save failed: " + ex.getMessage());
            return false;
        }
    }

    public boolean unlink(UUID uuid) {
        if (!available() || uuid == null) return false;

        try (
            Connection connection = storage.connection();
            PreparedStatement ps = connection.prepareStatement("DELETE FROM ic_discord_links WHERE uuid = ?")
        ) {
            ps.setString(1, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            logger.warn("Discord unlink failed: " + ex.getMessage());
            return false;
        }
    }

    public boolean saveCode(String code, UUID uuid, String name, long expiresAt) {
        if (!available() || code == null || uuid == null) return false;

        try (Connection connection = storage.connection()) {
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM ic_discord_sync_codes WHERE uuid = ? OR expires_at < ?"
                )
            ) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, System.currentTimeMillis());
                ps.executeUpdate();
            }
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO ic_discord_sync_codes (code, uuid, name, expires_at) VALUES (?, ?, ?, ?)"
                )
            ) {
                String safeName = name == null ? "?" : name;
                ps.setString(1, code);
                ps.setString(2, uuid.toString());
                ps.setString(3, safeName.length() > 16 ? safeName.substring(0, 16) : safeName);
                ps.setLong(4, expiresAt);
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException ex) {
            logger.warn("Discord sync code save failed: " + ex.getMessage());
            return false;
        }
    }

    public Optional<PendingCode> consumeCode(String code) {
        if (!available() || code == null) return Optional.empty();

        try (Connection connection = storage.connection()) {
            PendingCode found;
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid, name, expires_at FROM ic_discord_sync_codes WHERE code = ?"
                )
            ) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    found = new PendingCode(UUID.fromString(rs.getString(1)), rs.getString(2), rs.getLong(3));
                }
            }
            try (
                PreparedStatement ps = connection.prepareStatement("DELETE FROM ic_discord_sync_codes WHERE code = ?")
            ) {
                ps.setString(1, code);
                if (ps.executeUpdate() == 0) return Optional.empty();
            }
            return found.expiresAt() < System.currentTimeMillis() ? Optional.empty() : Optional.of(found);
        } catch (SQLException | IllegalArgumentException ex) {
            logger.warn("Discord sync code lookup failed: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public record PendingCode(UUID uuid, String name, long expiresAt) {}
}
