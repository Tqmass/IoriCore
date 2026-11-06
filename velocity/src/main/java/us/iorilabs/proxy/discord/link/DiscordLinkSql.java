package us.iorilabs.proxy.discord.link;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.logging.ProxyLogger;

@RequiredArgsConstructor
public final class DiscordLinkSql {

    private final StorageService storage;
    private final ProxyLogger logger;

    public boolean available() {
        return sql().isPresent();
    }

    public Optional<String> discordId(UUID uuid) {
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty() || uuid == null) return Optional.empty();

        try (
            Connection connection = sql.get().connection();
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
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty() || discordId == null || discordId.isBlank()) return Optional.empty();

        try (
            Connection connection = sql.get().connection();
            PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid FROM ic_discord_links WHERE discord_id = ? LIMIT 1"
            )
        ) {
            ps.setString(1, discordId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(UUID.fromString(rs.getString(1))) : Optional.empty();
            }
        } catch (SQLException | IllegalArgumentException ex) {
            logger.warn("Discord link lookup failed: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean link(UUID uuid, String discordId, long now) {
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty() || uuid == null || discordId == null) return false;

        try (Connection connection = sql.get().connection()) {
            boolean auto = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (
                    PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM ic_discord_links WHERE uuid = ? OR discord_id = ?"
                    )
                ) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, discordId);
                    ps.executeUpdate();
                }
                try (
                    PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO ic_discord_links (uuid, discord_id, linked_at) VALUES (?, ?, ?)"
                    )
                ) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, discordId);
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
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty() || uuid == null) return false;

        try (
            Connection connection = sql.get().connection();
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
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty()) return false;

        try (Connection connection = sql.get().connection()) {
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
                ps.setString(1, code);
                ps.setString(2, uuid.toString());
                ps.setString(3, name.length() > 16 ? name.substring(0, 16) : name);
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
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty() || code == null) return Optional.empty();

        try (Connection connection = sql.get().connection()) {
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

    private Optional<MySqlStorageService> sql() {
        if (storage == null || !storage.isConnected()) return Optional.empty();
        return storage.mysql();
    }

    public record PendingCode(UUID uuid, String name, long expiresAt) {}
}
