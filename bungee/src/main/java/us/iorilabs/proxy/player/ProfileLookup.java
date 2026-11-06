package us.iorilabs.proxy.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.logging.ProxyLogger;

@RequiredArgsConstructor
public final class ProfileLookup {

    private final StorageService storage;
    private final ProxyLogger logger;

    public Optional<Known> findByName(String name) {
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty() || name == null || name.isBlank() || name.length() > 16) return Optional.empty();

        try (
            Connection connection = sql.get().connection();
            PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, name FROM ip_players WHERE LOWER(name) = LOWER(?) ORDER BY last_join DESC LIMIT 1"
            )
        ) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Known(UUID.fromString(rs.getString(1)), rs.getString(2)));
            }
        } catch (Exception ex) {
            logger.warn("Profile lookup failed: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public int friendCount(UUID uuid) {
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty() || uuid == null) return -1;

        try (
            Connection connection = sql.get().connection();
            PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM ip_friends WHERE uuid_a = ? OR uuid_b = ?"
            )
        ) {
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception ex) {
            logger.warn("Profile friend count failed: " + ex.getMessage());
            return -1;
        }
    }

    private Optional<MySqlStorageService> sql() {
        if (storage == null || !storage.isConnected()) return Optional.empty();
        return storage.mysql();
    }

    public record Known(UUID uuid, String name) {}
}
