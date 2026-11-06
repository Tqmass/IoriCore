package us.iorilabs.proxy.blacklist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.StorageType;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.logging.ProxyLogger;

@RequiredArgsConstructor
public final class BlacklistSql {

    private final StorageService storage;
    private final ProxyLogger logger;

    public Set<String> loadAll() {
        Set<String> entries = new HashSet<>();
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty()) return entries;

        try (
            Connection connection = sql.get().connection();
            PreparedStatement ps = connection.prepareStatement("SELECT entry FROM ic_blacklist");
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) entries.add(rs.getString(1));
        } catch (Exception ex) {
            logger.warn("Blacklist load failed: " + ex.getMessage());
        }
        return entries;
    }

    public void insert(String entry, String reason, String actor) {
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty()) return;

        try (
            Connection connection = sql.get().connection();
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO ic_blacklist (entry, reason, actor, created_at) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE reason = VALUES(reason), actor = VALUES(actor)"
            )
        ) {
            ps.setString(1, entry);
            ps.setString(2, truncate(reason, 256));
            ps.setString(3, truncate(actor, 64));
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception ex) {
            logger.warn("Blacklist insert failed: " + ex.getMessage());
        }
    }

    public void delete(String entry) {
        Optional<MySqlStorageService> sql = sql();
        if (sql.isEmpty()) return;

        try (
            Connection connection = sql.get().connection();
            PreparedStatement ps = connection.prepareStatement("DELETE FROM ic_blacklist WHERE entry = ?")
        ) {
            ps.setString(1, entry);
            ps.executeUpdate();
        } catch (Exception ex) {
            logger.warn("Blacklist delete failed: " + ex.getMessage());
        }
    }

    public boolean shared() {
        return sql().isPresent() && storage.type() == StorageType.MYSQL;
    }

    private Optional<MySqlStorageService> sql() {
        if (storage == null || !storage.isConnected()) return Optional.empty();
        return storage.mysql();
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
