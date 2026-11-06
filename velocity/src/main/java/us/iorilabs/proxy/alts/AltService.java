package us.iorilabs.proxy.alts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.proxy.blacklist.BlacklistService;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.staff.StaffAlertService;

public final class AltService {

    public enum AltStatus {
        ONLINE,
        OFFLINE,
        BANNED,
    }

    public record AltAccount(UUID uuid, String name, AltStatus status) {}

    private final ProxyPlatform platform;
    private final StorageService storage;
    private final ProxyLogger logger;
    private final LanguageManager language;
    private final BlacklistService blacklist;
    private final StaffAlertService staffAlerts;
    private final Map<UUID, Set<String>> ipsByUuid = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> uuidsByIp = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public AltService(
        ProxyPlatform platform,
        StorageService storage,
        ProxyLogger logger,
        LanguageManager language,
        BlacklistService blacklist,
        StaffAlertService staffAlerts
    ) {
        this.platform = platform;
        this.storage = storage;
        this.logger = logger;
        this.language = language;
        this.blacklist = blacklist;
        this.staffAlerts = staffAlerts;
        loadFromDatabase();
    }

    public void onJoin(UUID uuid, String name, String ip) {
        if (uuid == null || name == null) return;

        names.put(uuid, name);
        if (ip == null || ip.isBlank() || ip.startsWith("127.") || "0.0.0.0".equals(ip)) return;

        String normalizedIp = ip.trim().toLowerCase(Locale.ROOT);
        ipsByUuid.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(normalizedIp);
        uuidsByIp.computeIfAbsent(normalizedIp, k -> ConcurrentHashMap.newKeySet()).add(uuid);
        persistIp(uuid, normalizedIp);
        checkEvasion(uuid, name);
    }

    public List<AltAccount> altsOf(UUID uuid) {
        Set<UUID> related = new LinkedHashSet<>();
        Set<String> ips = ipsByUuid.getOrDefault(uuid, Set.of());
        for (String ip : ips) {
            related.addAll(uuidsByIp.getOrDefault(ip, Set.of()));
        }

        related.remove(uuid);
        List<AltAccount> out = new ArrayList<>();
        for (UUID other : related) {
            out.add(new AltAccount(other, resolveName(other), statusOf(other)));
        }

        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    public AltStatus statusOf(UUID uuid) {
        String name = resolveName(uuid);
        if (blacklist != null && blacklist.enabled() && blacklist.isBlacklisted(uuid, name)) {
            return AltStatus.BANNED;
        }

        if (platform.findPlayerByUuid(uuid).isPresent()) return AltStatus.ONLINE;

        return AltStatus.OFFLINE;
    }

    public String statusLabel(AltStatus status) {
        return switch (status) {
            case ONLINE -> language.raw("alts.status_online");
            case OFFLINE -> language.raw("alts.status_offline");
            case BANNED -> language.raw("alts.status_banned");
        };
    }

    public void transferIps(UUID from, UUID to) {
        Set<String> ips = ipsByUuid.remove(from);
        if (ips == null || ips.isEmpty()) return;

        for (String ip : ips) {
            Set<UUID> set = uuidsByIp.get(ip);
            if (set != null) {
                set.remove(from);
                set.add(to);
            }

            ipsByUuid.computeIfAbsent(to, k -> ConcurrentHashMap.newKeySet()).add(ip);
            persistIp(to, ip);
            deleteIp(from, ip);
        }
    }

    private void checkEvasion(UUID uuid, String name) {
        if (staffAlerts == null || blacklist == null || !blacklist.enabled()) return;
        if (blacklist.isBlacklisted(uuid, name)) return;

        for (AltAccount alt : altsOf(uuid)) {
            if (alt.status() != AltStatus.BANNED) continue;

            staffAlerts.alert(
                "alerts.alt_evasion",
                Map.of("player", name, "alt", alt.name(), "status", statusLabel(alt.status()))
            );
            return;
        }
    }

    private String resolveName(UUID uuid) {
        String cached = names.get(uuid);
        if (cached != null) return cached;

        return platform.findPlayerByUuid(uuid).map(ProxyPlayer::name).orElse(uuid.toString().substring(0, 8));
    }

    private void persistIp(UUID uuid, String ip) {
        if (!storage.isConnected()) return;

        long now = System.currentTimeMillis();
        platform.asyncExecutor().execute(() ->
            storage.mysql().ifPresent(mysql -> {
                try {
                    mysql.call(connection -> {
                        try (
                            PreparedStatement ps = connection.prepareStatement(
                                """
                                INSERT INTO ip_account_ips (uuid, ip, last_seen) VALUES (?, ?, ?)
                                ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)
                                """
                            )
                        ) {
                            ps.setString(1, uuid.toString());
                            ps.setString(2, ip);
                            ps.setLong(3, now);
                            ps.executeUpdate();
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }

                        return null;
                    });
                } catch (Exception ex) {
                    logger.error("Alt IP persist failed", ex);
                }
            })
        );
    }

    private void deleteIp(UUID uuid, String ip) {
        if (!storage.isConnected()) return;

        platform.asyncExecutor().execute(() ->
            storage.mysql().ifPresent(mysql -> {
                try {
                    mysql.call(connection -> {
                        try (
                            PreparedStatement ps = connection.prepareStatement(
                                "DELETE FROM ip_account_ips WHERE uuid = ? AND ip = ?"
                            )
                        ) {
                            ps.setString(1, uuid.toString());
                            ps.setString(2, ip);
                            ps.executeUpdate();
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }

                        return null;
                    });
                } catch (Exception ex) {
                    logger.error("Alt IP delete failed", ex);
                }
            })
        );
    }

    private void loadFromDatabase() {
        if (!storage.isConnected()) return;

        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty()) return;

        try {
            mysql.get().call(connection -> {
                try (PreparedStatement ps = connection.prepareStatement("SELECT uuid, ip FROM ip_account_ips")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString(1));
                            String ip = rs.getString(2).toLowerCase(Locale.ROOT);
                            ipsByUuid.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(ip);
                            uuidsByIp.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }

                return null;
            });
        } catch (Exception ex) {
            logger.error("Alt IP load failed", ex);
        }
    }
}
