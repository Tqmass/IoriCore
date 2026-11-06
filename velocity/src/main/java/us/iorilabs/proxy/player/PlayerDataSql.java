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
public final class PlayerDataSql {

    private final StorageService storage;
    private final ProxyLogger logger;

    public Optional<PlayerData> load(UUID uuid) {
        if (!storage.isConnected()) return Optional.empty();

        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty()) return Optional.empty();

        try {
            return mysql.get().call(connection -> {
                try {
                    return load(connection, uuid);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception ex) {
            logger.error("PlayerData load failed for " + uuid, ex);
            return Optional.empty();
        }
    }

    public void upsert(PlayerData data) {
        if (!storage.isConnected() || data == null) return;

        storage.mysql().ifPresent(mysql -> {
            try {
                mysql.call(connection -> {
                    try {
                        upsert(connection, data);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }

                    return null;
                });
            } catch (Exception ex) {
                logger.error("PlayerData upsert failed for " + data.uuid(), ex);
            }
        });
    }

    public void transfer(UUID from, UUID to, String toName) {
        if (!storage.isConnected()) return;

        storage.mysql().ifPresent(mysql -> {
            try {
                mysql.call(connection -> {
                    try {
                        transferRows(connection, from, to, toName);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }

                    return null;
                });
            } catch (Exception ex) {
                logger.error("PlayerData transfer SQL failed", ex);
            }
        });
    }

    private static Optional<PlayerData> load(Connection connection, UUID uuid) throws Exception {
        try (
            PreparedStatement ps = connection.prepareStatement(
                "SELECT name, first_join, last_join FROM ip_players WHERE uuid = ? LIMIT 1"
            )
        ) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                PlayerData data = new PlayerData(uuid, rs.getString(1));
                data.firstJoin(rs.getLong(2));
                data.lastJoin(rs.getLong(3));
                return Optional.of(data);
            }
        }
    }

    private static void upsert(Connection connection, PlayerData data) throws Exception {
        try (
            PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO ip_players (uuid, name, first_join, last_join) VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), last_join = VALUES(last_join)
                """
            )
        ) {
            ps.setString(1, data.uuid().toString());
            ps.setString(2, data.name());
            ps.setLong(3, data.firstJoin() > 0 ? data.firstJoin() : System.currentTimeMillis());
            ps.setLong(4, data.lastJoin() > 0 ? data.lastJoin() : System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    private static void transferRows(Connection connection, UUID from, UUID to, String toName) throws Exception {
        long fromFirst = 0L;
        long toFirst = 0L;
        try (PreparedStatement ps = connection.prepareStatement("SELECT first_join FROM ip_players WHERE uuid = ?")) {
            ps.setString(1, from.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) fromFirst = rs.getLong(1);
            }

            ps.setString(1, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) toFirst = rs.getLong(1);
            }
        }

        long first = fromFirst > 0 && (toFirst <= 0 || fromFirst < toFirst) ? fromFirst : toFirst;
        long now = System.currentTimeMillis();
        try (
            PreparedStatement upsert = connection.prepareStatement(
                """
                INSERT INTO ip_players (uuid, name, first_join, last_join) VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), last_join = VALUES(last_join)
                """
            )
        ) {
            upsert.setString(1, to.toString());
            upsert.setString(2, toName);
            upsert.setLong(3, first > 0 ? first : now);
            upsert.setLong(4, now);
            upsert.executeUpdate();
        }

        if (first > 0) {
            try (
                PreparedStatement fix = connection.prepareStatement(
                    "UPDATE ip_players SET first_join = ? WHERE uuid = ?"
                )
            ) {
                fix.setLong(1, first);
                fix.setString(2, to.toString());
                fix.executeUpdate();
            }
        }

        try (PreparedStatement del = connection.prepareStatement("DELETE FROM ip_players WHERE uuid = ?")) {
            del.setString(1, from.toString());
            del.executeUpdate();
        }
    }
}
