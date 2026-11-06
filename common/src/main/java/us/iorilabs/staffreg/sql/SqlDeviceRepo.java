package us.iorilabs.staffreg.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import us.iorilabs.staffreg.DeviceRecord;
import us.iorilabs.staffreg.DeviceRepo;
import us.iorilabs.staffreg.storage.StaffRegSqlRunner;

public final class SqlDeviceRepo implements DeviceRepo {

    private final StaffRegSqlRunner sql;

    public SqlDeviceRepo(StaffRegSqlRunner sql) {
        this.sql = sql;
    }

    @Override
    public CompletableFuture<List<DeviceRecord>> listForAccount(UUID accountUuid) {
        if (accountUuid == null) return CompletableFuture.completedFuture(List.of());
        return sql.call(connection -> {
            List<DeviceRecord> out = new ArrayList<>();
            try (
                PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM ip_staff_devices WHERE account_uuid = ? ORDER BY last_seen DESC"
                )
            ) {
                ps.setString(1, accountUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(map(rs));
                }
            }
            return out;
        });
    }

    @Override
    public CompletableFuture<Void> upsert(DeviceRecord record) {
        if (record == null || record.accountUuid() == null) return CompletableFuture.completedFuture(null);
        return sql.run(connection -> {
            if (record.id() == null) record.id(UUID.randomUUID());
            Instant now = Instant.now();
            if (record.firstSeenAt() == null) record.firstSeenAt(now);
            if (record.lastSeenAt() == null) record.lastSeenAt(now);
            String upsert =
                "INSERT INTO ip_staff_devices (id, account_uuid, ip_hmac, country, region, city, first_seen, " +
                "last_seen, vpn_or_proxy) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                "country=VALUES(country), region=VALUES(region), city=VALUES(city), last_seen=VALUES(last_seen), " +
                "vpn_or_proxy=VALUES(vpn_or_proxy)";
            try (PreparedStatement ps = connection.prepareStatement(upsert)) {
                bind(ps, record);
                ps.executeUpdate();
            } catch (SQLException ex) {
                if (!missingColumn(ex)) throw ex;
                String legacy =
                    "INSERT INTO ip_staff_devices (id, account_uuid, ip_hmac, country, region, city, first_seen, " +
                    "last_seen) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE country=VALUES(country), " +
                    "region=VALUES(region), city=VALUES(city), last_seen=VALUES(last_seen)";
                try (PreparedStatement ps = connection.prepareStatement(legacy)) {
                    ps.setString(1, record.id().toString());
                    ps.setString(2, record.accountUuid().toString());
                    ps.setString(3, record.ipHmac());
                    ps.setString(4, record.country());
                    ps.setString(5, record.region());
                    ps.setString(6, record.city());
                    ps.setLong(7, record.firstSeenAt().toEpochMilli());
                    ps.setLong(8, record.lastSeenAt().toEpochMilli());
                    ps.executeUpdate();
                }
            }
        });
    }

    private static void bind(PreparedStatement ps, DeviceRecord record) throws SQLException {
        ps.setString(1, record.id().toString());
        ps.setString(2, record.accountUuid().toString());
        ps.setString(3, record.ipHmac());
        ps.setString(4, record.country());
        ps.setString(5, record.region());
        ps.setString(6, record.city());
        ps.setLong(7, record.firstSeenAt().toEpochMilli());
        ps.setLong(8, record.lastSeenAt().toEpochMilli());
        ps.setBoolean(9, record.vpnOrProxy());
    }

    private static DeviceRecord map(ResultSet rs) throws SQLException {
        DeviceRecord record = new DeviceRecord()
            .id(UUID.fromString(rs.getString("id")))
            .accountUuid(UUID.fromString(rs.getString("account_uuid")))
            .ipHmac(rs.getString("ip_hmac"))
            .country(rs.getString("country"))
            .region(rs.getString("region"))
            .city(rs.getString("city"))
            .firstSeenAt(Instant.ofEpochMilli(rs.getLong("first_seen")))
            .lastSeenAt(Instant.ofEpochMilli(rs.getLong("last_seen")));
        try {
            record.vpnOrProxy(rs.getBoolean("vpn_or_proxy"));
        } catch (SQLException ignored) {
            record.vpnOrProxy(false);
        }
        return record;
    }

    private static boolean missingColumn(SQLException ex) {
        String message = ex.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("unknown column");
    }
}
