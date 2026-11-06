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
import us.iorilabs.staffreg.StaffAccount;
import us.iorilabs.staffreg.StaffAccountRepo;
import us.iorilabs.staffreg.StaffRegEnums.AccountStatus;
import us.iorilabs.staffreg.StaffRegEnums.AuthMode;
import us.iorilabs.staffreg.StaffRegEnums.LinkStatus;
import us.iorilabs.staffreg.TrustSnapshot;
import us.iorilabs.staffreg.storage.StaffRegSqlRunner;

public final class SqlStaffAccountRepo implements StaffAccountRepo {

    private final StaffRegSqlRunner sql;

    public SqlStaffAccountRepo(StaffRegSqlRunner sql) {
        this.sql = sql;
    }

    @Override
    public CompletableFuture<Optional<StaffAccount>> findByUuid(UUID uuid) {
        if (uuid == null) return CompletableFuture.completedFuture(Optional.empty());
        return sql.call(connection -> load(connection, "uuid = ?", uuid.toString()));
    }

    @Override
    public CompletableFuture<Optional<StaffAccount>> findByUsername(String username) {
        if (username == null || username.isBlank()) return CompletableFuture.completedFuture(Optional.empty());
        return sql.call(connection -> load(connection, "username = ?", username.toLowerCase(Locale.ROOT)));
    }

    @Override
    public CompletableFuture<Optional<StaffAccount>> findByDiscordId(String discordId) {
        if (discordId == null || discordId.isBlank()) return CompletableFuture.completedFuture(Optional.empty());
        return sql.call(connection -> load(connection, "discord_id = ?", discordId));
    }

    @Override
    public CompletableFuture<Void> save(StaffAccount account) {
        if (account == null || account.uuid() == null) return CompletableFuture.completedFuture(null);
        return sql.run(connection -> upsert(connection, account));
    }

    private static Optional<StaffAccount> load(Connection connection, String where, String value) throws SQLException {
        String query =
            "SELECT uuid, username, password_hash, must_change_password, discord_id, link_status, account_status, " +
            "preferred_auth, created_at, updated_at, created_by, trust_ip_hmac, trust_country, trust_region, " +
            "trust_city, trust_at, trust_vpn, trust_expires_at FROM ip_staff_accounts WHERE " +
            where +
            " LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            if (missingColumn(ex)) return loadLegacy(connection, where, value);
            throw ex;
        }
    }

    private static Optional<StaffAccount> loadLegacy(Connection connection, String where, String value)
        throws SQLException {
        String query =
            "SELECT uuid, username, password_hash, must_change_password, discord_id, link_status, account_status, " +
            "preferred_auth, created_at, updated_at, created_by, trust_ip_hmac, trust_country, trust_region, " +
            "trust_city, trust_at FROM ip_staff_accounts WHERE " +
            where +
            " LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapLegacy(rs));
            }
        }
    }

    private static void upsert(Connection connection, StaffAccount account) throws SQLException {
        String sql =
            "INSERT INTO ip_staff_accounts (uuid, username, password_hash, must_change_password, discord_id, " +
            "link_status, account_status, preferred_auth, created_at, updated_at, created_by, trust_ip_hmac, " +
            "trust_country, trust_region, trust_city, trust_at, trust_vpn, trust_expires_at) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
            "username=VALUES(username), password_hash=VALUES(password_hash), " +
            "must_change_password=VALUES(must_change_password), discord_id=VALUES(discord_id), " +
            "link_status=VALUES(link_status), account_status=VALUES(account_status), " +
            "preferred_auth=VALUES(preferred_auth), updated_at=VALUES(updated_at), " +
            "trust_ip_hmac=VALUES(trust_ip_hmac), trust_country=VALUES(trust_country), " +
            "trust_region=VALUES(trust_region), trust_city=VALUES(trust_city), trust_at=VALUES(trust_at), " +
            "trust_vpn=VALUES(trust_vpn), trust_expires_at=VALUES(trust_expires_at)";
        TrustSnapshot trust = account.trust();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, account.uuid().toString());
            ps.setString(2, account.username() == null ? "" : account.username().toLowerCase(Locale.ROOT));
            ps.setString(3, account.passwordHash());
            ps.setBoolean(4, account.mustChangePassword());
            ps.setString(5, blankToNull(account.discordId()));
            ps.setString(6, enumName(account.linkStatus(), LinkStatus.UNLINKED));
            ps.setString(7, enumName(account.accountStatus(), AccountStatus.ACTIVE));
            ps.setString(8, enumName(account.preferredAuthMode(), AuthMode.DISCORD));
            ps.setLong(9, epoch(account.createdAt()));
            ps.setLong(10, epoch(account.updatedAt() == null ? Instant.now() : account.updatedAt()));
            ps.setString(11, account.createdBy() == null ? null : account.createdBy().toString());
            ps.setString(12, trust == null ? null : trust.ipHmac());
            ps.setString(13, trust == null ? null : trust.country());
            ps.setString(14, trust == null ? null : trust.region());
            ps.setString(15, trust == null ? null : trust.city());
            ps.setObject(16, trust == null || trust.establishedAt() == null ? null : epoch(trust.establishedAt()));
            ps.setBoolean(17, trust != null && trust.vpnOrProxy());
            ps.setObject(18, trust == null || trust.expiresAt() == null ? null : epoch(trust.expiresAt()));
            ps.executeUpdate();
        }
    }

    private static StaffAccount map(ResultSet rs) throws SQLException {
        StaffAccount account = mapLegacy(rs);
        TrustSnapshot trust = account.trust();
        if (trust != null) {
            trust.vpnOrProxy(rs.getBoolean("trust_vpn"));
            long exp = rs.getLong("trust_expires_at");
            if (!rs.wasNull()) trust.expiresAt(Instant.ofEpochMilli(exp));
        }
        return account;
    }

    private static StaffAccount mapLegacy(ResultSet rs) throws SQLException {
        StaffAccount account = new StaffAccount()
            .uuid(UUID.fromString(rs.getString("uuid")))
            .username(rs.getString("username"))
            .passwordHash(rs.getString("password_hash"))
            .mustChangePassword(rs.getBoolean("must_change_password"))
            .discordId(rs.getString("discord_id"))
            .linkStatus(parse(LinkStatus.class, rs.getString("link_status"), LinkStatus.UNLINKED))
            .accountStatus(parse(AccountStatus.class, rs.getString("account_status"), AccountStatus.ACTIVE))
            .preferredAuthMode(parse(AuthMode.class, rs.getString("preferred_auth"), AuthMode.DISCORD))
            .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")))
            .updatedAt(Instant.ofEpochMilli(rs.getLong("updated_at")));
        String createdBy = rs.getString("created_by");
        if (createdBy != null && !createdBy.isBlank()) account.createdBy(UUID.fromString(createdBy));
        String ip = rs.getString("trust_ip_hmac");
        if (ip != null || rs.getString("trust_country") != null) {
            TrustSnapshot trust = new TrustSnapshot()
                .ipHmac(ip)
                .country(rs.getString("trust_country"))
                .region(rs.getString("trust_region"))
                .city(rs.getString("trust_city"));
            long at = rs.getLong("trust_at");
            if (!rs.wasNull()) trust.establishedAt(Instant.ofEpochMilli(at));
            account.trust(trust);
        }
        return account;
    }

    private static boolean missingColumn(SQLException ex) {
        String message = ex.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("unknown column");
    }

    private static long epoch(Instant instant) {
        return instant == null ? Instant.now().toEpochMilli() : instant.toEpochMilli();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String enumName(Enum<?> value, Enum<?> fallback) {
        return (value == null ? fallback : value).name();
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
