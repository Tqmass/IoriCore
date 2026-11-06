package us.iorilabs.proxy.staffauth;

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
public final class StaffSessionSql {

    private final StorageService storage;
    private final ProxyLogger logger;

    public boolean available() {
        return sql().isPresent();
    }

    public Optional<Stored> load(UUID uuid) throws SQLException {
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty() || uuid == null) return Optional.empty();

        try (
            Connection connection = sql.get().connection();
            PreparedStatement ps = connection.prepareStatement(
                "SELECT last_ip, last_country FROM ic_staff_sessions WHERE uuid = ?"
            )
        ) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Stored(rs.getString(1), rs.getString(2)));
            }
        }
    }

    public void save(UUID uuid, String ip, String country) {
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty() || uuid == null || ip == null || ip.isBlank()) return;

        try (
            Connection connection = sql.get().connection();
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO ic_staff_sessions (uuid, last_ip, last_country, updated_at) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_ip = VALUES(last_ip), last_country = VALUES(last_country), " +
                    "updated_at = VALUES(updated_at)"
            )
        ) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ip.length() > 45 ? ip.substring(0, 45) : ip);
            ps.setString(3, country == null || country.length() > 8 ? "?" : country);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception ex) {
            logger.warn("StaffAuth session save failed: " + ex.getMessage());
        }
    }

    private Optional<MySqlStorageService> sql() {
        if (storage == null || !storage.isConnected()) return Optional.empty();
        return storage.mysql();
    }

    public record Stored(String ip, String country) {}
}
