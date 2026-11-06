package us.iorilabs.bukkit.modules.economy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import us.iorilabs.api.storage.SqlStorage;

public final class EconomySql {

    private final SqlStorage storage;
    private final Logger logger;
    private final boolean persist;

    public EconomySql(SqlStorage storage, Logger logger, boolean persist) {
        this.storage = storage;
        this.logger = logger;
        this.persist = persist;
    }

    public double load(UUID uuid, double fallback) {
        if (!persist || uuid == null || storage == null || !storage.isConnected()) return fallback;

        try (
            Connection connection = storage.connection();
            PreparedStatement ps = connection.prepareStatement("SELECT balance FROM ic_balances WHERE player_uuid = ?")
        ) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Economy SQL load failed · " + uuid, ex);
        }
        return fallback;
    }

    public void save(UUID uuid, double balance) {
        if (!persist || uuid == null || storage == null || !storage.isConnected()) return;

        try (
            Connection connection = storage.connection();
            PreparedStatement ps = connection.prepareStatement(
                "MERGE INTO ic_balances (player_uuid, balance) KEY (player_uuid) VALUES (?, ?)"
            )
        ) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, balance);
            ps.executeUpdate();
        } catch (Exception mergeEx) {
            try (
                Connection connection = storage.connection();
                PreparedStatement upsert = connection.prepareStatement(
                    "INSERT INTO ic_balances (player_uuid, balance) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE balance = VALUES(balance)"
                )
            ) {
                upsert.setString(1, uuid.toString());
                upsert.setDouble(2, balance);
                upsert.executeUpdate();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Economy SQL save failed · " + uuid, ex);
            }
        }
    }
}
