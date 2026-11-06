package us.iorilabs.proxy.player.playtime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.proxy.config.MaintenanceConfig;
import us.iorilabs.proxy.config.PlaytimeConfig;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.server.ServerModes;

public final class PlaytimeService {

    public static final String SCOPE_TOTAL = "total";

    private final ProxyPlatform platform;
    private final StorageService storage;
    private final MaintenanceConfig maintenanceConfig;
    private final PlaytimeConfig playtimeConfig;
    private final ProxyLogger logger;
    private final Map<UUID, PlaytimeSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> memoryMillis = new ConcurrentHashMap<>();

    public PlaytimeService(
        ProxyPlatform platform,
        StorageService storage,
        MaintenanceConfig maintenanceConfig,
        PlaytimeConfig playtimeConfig,
        ProxyLogger logger
    ) {
        this.platform = platform;
        this.storage = storage;
        this.maintenanceConfig = maintenanceConfig;
        this.playtimeConfig = playtimeConfig;
        this.logger = logger;
        platform.runTimer(
            this::flushActiveSessions,
            playtimeConfig.flushIntervalSeconds() * 1000L,
            playtimeConfig.flushIntervalSeconds() * 1000L,
            "ioriproxy-playtime-flush"
        );
    }

    public void onJoin(UUID uuid, String server) {
        sessions.put(uuid, new PlaytimeSession(server, System.currentTimeMillis()));
    }

    public void onSwitch(UUID uuid, String server) {
        PlaytimeSession session = sessions.get(uuid);
        if (session == null) {
            sessions.put(uuid, new PlaytimeSession(server, System.currentTimeMillis()));
            return;
        }

        closeSlice(uuid, session);
        sessions.put(uuid, new PlaytimeSession(server, System.currentTimeMillis()));
    }

    public void onQuit(UUID uuid) {
        PlaytimeSession session = sessions.remove(uuid);
        if (session != null) {
            closeSlice(uuid, session);
        }
    }

    public long millis(UUID uuid, String scope) {
        long total = loadPersisted(uuid, scope) + memoryMillis.getOrDefault(memoryKey(uuid, scope), 0L);
        PlaytimeSession session = sessions.get(uuid);
        if (session != null && scopeApplies(session.server(), scope)) {
            total += Math.max(0L, System.currentTimeMillis() - session.startedAtMillis());
        }

        return total;
    }

    public long totalMillis(UUID uuid) {
        return millis(uuid, SCOPE_TOTAL);
    }

    public void transferAccount(UUID from, UUID to) {
        PlaytimeSession session = sessions.remove(from);
        if (session != null) {
            closeSlice(from, session);
            sessions.put(to, new PlaytimeSession(session.server(), System.currentTimeMillis()));
        }

        String prefix = from + "|";
        for (Map.Entry<String, Long> entry : List.copyOf(memoryMillis.entrySet())) {
            if (!entry.getKey().startsWith(prefix)) continue;

            String scope = entry.getKey().substring(prefix.length());
            memoryMillis.merge(memoryKey(to, scope), entry.getValue(), Long::sum);
            memoryMillis.remove(entry.getKey());
        }
    }

    public Map<String, Long> serverMillis(UUID uuid) {
        Map<String, Long> raw = new LinkedHashMap<>();

        for (Map.Entry<String, Long> entry : memoryMillis.entrySet()) {
            String key = entry.getKey();
            String prefix = uuid + "|server:";
            if (!key.startsWith(prefix)) continue;

            String server = key.substring(prefix.length());
            raw.merge(server, entry.getValue(), Long::sum);
        }

        loadPersistedServers(uuid, raw);

        PlaytimeSession session = sessions.get(uuid);
        if (session != null && session.server() != null && !session.server().isBlank()) {
            long live = Math.max(0L, System.currentTimeMillis() - session.startedAtMillis());
            raw.merge(session.server().toLowerCase(Locale.ROOT), live, Long::sum);
        }

        return raw
            .entrySet()
            .stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .collect(
                LinkedHashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                LinkedHashMap::putAll
            );
    }

    private void loadPersistedServers(UUID uuid, Map<String, Long> into) {
        if (!storage.isConnected()) return;

        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty()) return;

        try {
            mysql.get().call(connection -> {
                try (
                    PreparedStatement ps = connection.prepareStatement(
                        "SELECT scope, millis FROM ip_playtime WHERE uuid = ? AND scope LIKE 'server:%'"
                    )
                ) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String scope = rs.getString(1);
                            if (scope == null || !scope.startsWith("server:")) continue;

                            into.merge(scope.substring("server:".length()), rs.getLong(2), Long::sum);
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }

                return null;
            });
        } catch (Exception ex) {
            logger.error("Playtime server breakdown failed for " + uuid, ex);
        }
    }

    private void closeSlice(UUID uuid, PlaytimeSession session) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - session.startedAtMillis());
        if (elapsed == 0L) return;

        addScope(uuid, SCOPE_TOTAL, elapsed);
        addScope(uuid, serverScope(session.server()), elapsed);
        Optional<String> mode = modeForServer(session.server());
        mode.ifPresent(value -> addScope(uuid, modeScope(value), elapsed));
    }

    private void addScope(UUID uuid, String scope, long elapsed) {
        if (storage.isConnected()) {
            persistAsync(uuid, scope, elapsed);
            return;
        }

        if (playtimeConfig.memoryFallback()) {
            memoryMillis.merge(memoryKey(uuid, scope), elapsed, Long::sum);
        }
    }

    private void flushActiveSessions() {
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, PlaytimeSession> entry : sessions.entrySet()) {
            PlaytimeSession session = entry.getValue();
            long elapsed = now - session.startedAtMillis();
            if (elapsed < 1000L) continue;

            closeSlice(entry.getKey(), session);
            sessions.put(entry.getKey(), new PlaytimeSession(session.server(), now));
        }
    }

    private void persistAsync(UUID uuid, String scope, long deltaMillis) {
        if (!storage.isConnected()) return;

        platform.asyncExecutor().execute(() -> {
            storage.mysql().ifPresent(mysql -> {
                try {
                    mysql.call(connection -> {
                        try {
                            upsert(connection, uuid, scope, deltaMillis);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }

                        return null;
                    });
                } catch (Exception ex) {
                    logger.error("Playtime SQL persist failed for " + uuid, ex);
                }
            });
        });
    }

    private void upsert(Connection connection, UUID uuid, String scope, long deltaMillis) throws Exception {
        try (
            PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO ip_playtime (uuid, scope, millis) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE millis = millis + VALUES(millis)
                """
            )
        ) {
            ps.setString(1, uuid.toString());
            ps.setString(2, scope);
            ps.setLong(3, deltaMillis);
            ps.executeUpdate();
        }
    }

    private long loadPersisted(UUID uuid, String scope) {
        if (!storage.isConnected()) return 0L;

        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty()) return 0L;

        try {
            return mysql.get().call(connection -> {
                try {
                    try (
                        PreparedStatement ps = connection.prepareStatement(
                            "SELECT millis FROM ip_playtime WHERE uuid = ? AND scope = ?"
                        )
                    ) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, scope);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) return rs.getLong(1);
                        }
                    }

                    return 0L;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception ex) {
            logger.error("Playtime SQL read failed for " + uuid, ex);
            return 0L;
        }
    }

    private boolean scopeApplies(String server, String scope) {
        if (SCOPE_TOTAL.equals(scope)) return true;
        if (scope.startsWith("server:")) return scope.substring("server:".length()).equalsIgnoreCase(server);

        if (scope.startsWith("mode:")) {
            String mode = scope.substring("mode:".length());
            return modeForServer(server)
                .map(value -> value.equalsIgnoreCase(mode))
                .orElse(false);
        }

        return false;
    }

    private Optional<String> modeForServer(String server) {
        return ServerModes.modeForServer(ServerModes.resolve(platform, maintenanceConfig.modes()), server);
    }

    public static String serverScope(String server) {
        return "server:" + server.toLowerCase(Locale.ROOT);
    }

    public static String modeScope(String mode) {
        return "mode:" + mode.toLowerCase(Locale.ROOT);
    }

    private static String memoryKey(UUID uuid, String scope) {
        return uuid + "|" + scope.toLowerCase(Locale.ROOT);
    }

    private record PlaytimeSession(String server, long startedAtMillis) {}
}
