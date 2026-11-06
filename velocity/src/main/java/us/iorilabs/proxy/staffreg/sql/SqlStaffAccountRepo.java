package us.iorilabs.proxy.staffreg.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.staffreg.StaffAccount;
import us.iorilabs.staffreg.StaffAccountRepo;
import us.iorilabs.staffreg.StaffRegEnums.AccountStatus;
import us.iorilabs.staffreg.StaffRegEnums.AuthMode;
import us.iorilabs.staffreg.StaffRegEnums.LinkStatus;
import us.iorilabs.staffreg.TrustSnapshot;

public final class SqlStaffAccountRepo implements StaffAccountRepo {

    private final StorageService storage;
    private final ProxyLogger logger;

    public SqlStaffAccountRepo(StorageService storage, ProxyLogger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public boolean available() {
        return sql().isPresent();
    }

    @Override
    public CompletableFuture<Optional<StaffAccount>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (uuid == null) return Optional.empty();
            Optional<MySqlStorageService> sql = sql();
            if (sql.isEmpty()) return Optional.empty();
            try (
                Connection connection = sql.get().connection();
                PreparedStatement ps = connection.prepareStatement(
                    """
                    SELECT uuid, username, password_hash, must_change_password, discord_id, link_status,
                           account_status, preferred_auth, created_at, updated_at, created_by,
                           trust_ip_hmac, trust_country, trust_region, trust_city, trust_at
                    FROM ip_staff_accounts WHERE uuid = ?
                    """
                )
            ) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    return Optional.of(mapRow(rs));
                }
            } catch (Exception ex) {
                logger.warn("StaffReg account load failed: " + ex.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<StaffAccount>> findByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            if (username == null || username.isBlank()) return Optional.empty();
            Optional<MySqlStorageService> sql = sql();
            if (sql.isEmpty()) return Optional.empty();
            try (
                Connection connection = sql.get().connection();
                PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid FROM ip_staff_accounts WHERE LOWER(username) = LOWER(?) LIMIT 1"
                )
            ) {
                ps.setString(1, username.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    return findByUuid(UUID.fromString(rs.getString(1))).join();
                }
            } catch (Exception ex) {
                logger.warn("StaffReg account lookup failed: " + ex.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<StaffAccount>> findByDiscordId(String discordId) {
        return CompletableFuture.supplyAsync(() -> {
            if (discordId == null || discordId.isBlank()) return Optional.empty();
            Optional<MySqlStorageService> sql = sql();
            if (sql.isEmpty()) return Optional.empty();
            try (
                Connection connection = sql.get().connection();
                PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid FROM ip_staff_accounts WHERE discord_id = ? LIMIT 1"
                )
            ) {
                ps.setString(1, discordId.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    return findByUuid(UUID.fromString(rs.getString(1))).join();
                }
            } catch (Exception ex) {
                logger.warn("StaffReg discord lookup failed: " + ex.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Void> save(StaffAccount account) {
        return CompletableFuture.runAsync(() -> {
            if (account == null || account.uuid() == null) return;
            Optional<MySqlStorageService> sql = sql();
            if (sql.isEmpty()) return;

            TrustSnapshot trust = account.trust();
            long now = System.currentTimeMillis();
            long created = account.createdAt() == null ? now : account.createdAt().toEpochMilli();
            long updated = account.updatedAt() == null ? now : account.updatedAt().toEpochMilli();
            Long trustAt = trust == null || trust.establishedAt() == null ? null : trust.establishedAt().toEpochMilli();

            try (
                Connection connection = sql.get().connection();
                PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO ip_staff_accounts (
                      uuid, username, password_hash, must_change_password, discord_id, link_status,
                      account_status, preferred_auth, created_at, updated_at, created_by,
                      trust_ip_hmac, trust_country, trust_region, trust_city, trust_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      username = VALUES(username),
                      password_hash = VALUES(password_hash),
                      must_change_password = VALUES(must_change_password),
                      discord_id = VALUES(discord_id),
                      link_status = VALUES(link_status),
                      account_status = VALUES(account_status),
                      preferred_auth = VALUES(preferred_auth),
                      updated_at = VALUES(updated_at),
                      created_by = VALUES(created_by),
                      trust_ip_hmac = VALUES(trust_ip_hmac),
                      trust_country = VALUES(trust_country),
                      trust_region = VALUES(trust_region),
                      trust_city = VALUES(trust_city),
                      trust_at = VALUES(trust_at)
                    """
                )
            ) {
                ps.setString(1, account.uuid().toString());
                ps.setString(2, account.username());
                ps.setString(3, account.passwordHash());
                ps.setBoolean(4, account.mustChangePassword());
                ps.setString(5, account.discordId());
                ps.setString(6, account.linkStatus().name());
                ps.setString(7, account.accountStatus().name());
                ps.setString(8, account.preferredAuthMode().name());
                ps.setLong(9, created);
                ps.setLong(10, updated);
                ps.setString(11, account.createdBy() == null ? null : account.createdBy().toString());
                ps.setString(12, trust == null ? null : trust.ipHmac());
                ps.setString(13, trust == null ? null : trust.country());
                ps.setString(14, trust == null ? null : trust.region());
                ps.setString(15, trust == null ? null : trust.city());
                if (trustAt == null) ps.setObject(16, null);
                else ps.setLong(16, trustAt);
                ps.executeUpdate();
            } catch (Exception ex) {
                logger.warn("StaffReg account save failed: " + ex.getMessage());
            }
        });
    }

    private StaffAccount mapRow(ResultSet rs) throws SQLException {
        StaffAccount account = new StaffAccount()
            .uuid(UUID.fromString(rs.getString("uuid")))
            .username(rs.getString("username"))
            .passwordHash(rs.getString("password_hash"))
            .mustChangePassword(rs.getBoolean("must_change_password"))
            .discordId(rs.getString("discord_id"))
            .linkStatus(parseEnum(rs.getString("link_status"), LinkStatus.class, LinkStatus.UNLINKED))
            .accountStatus(parseEnum(rs.getString("account_status"), AccountStatus.class, AccountStatus.ACTIVE))
            .preferredAuthMode(parseEnum(rs.getString("preferred_auth"), AuthMode.class, AuthMode.DISCORD))
            .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")))
            .updatedAt(Instant.ofEpochMilli(rs.getLong("updated_at")));

        String createdBy = rs.getString("created_by");
        if (createdBy != null && !createdBy.isBlank()) {
            account.createdBy(UUID.fromString(createdBy));
        }

        String ipHmac = rs.getString("trust_ip_hmac");
        if (ipHmac != null && !ipHmac.isBlank()) {
            TrustSnapshot trust = new TrustSnapshot()
                .ipHmac(ipHmac)
                .country(rs.getString("trust_country"))
                .region(rs.getString("trust_region"))
                .city(rs.getString("trust_city"));
            long trustAt = rs.getLong("trust_at");
            if (!rs.wasNull()) trust.establishedAt(Instant.ofEpochMilli(trustAt));
            account.trust(trust);
        }
        return account;
    }

    private static <E extends Enum<E>> E parseEnum(String raw, Class<E> type, E fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private Optional<MySqlStorageService> sql() {
        if (storage == null || !storage.isConnected()) return Optional.empty();
        return storage.mysql();
    }
}
