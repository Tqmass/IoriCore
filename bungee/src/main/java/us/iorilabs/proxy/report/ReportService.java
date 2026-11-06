package us.iorilabs.proxy.report;

import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import us.iorilabs.proxy.config.CommandsConfig;
import us.iorilabs.proxy.config.ReportConfig;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.webhooks.WebhookRegistry;

public final class ReportService {

    private final ProxyPlatform platform;
    private final RedisService redis;
    private final LanguageManager language;
    private final WebhookRegistry webhooks;
    private final StorageService storage;
    private final ProxyLogger logger;
    private final CommandsConfig commands;
    private final String proxyId;
    private final Map<String, Report> byId = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(1);
    private final java.util.Set<UUID> titleMuted = ConcurrentHashMap.newKeySet();
    private ReportConfig reportConfig;

    public ReportService(
        ProxyPlatform platform,
        RedisService redis,
        LanguageManager language,
        WebhookRegistry webhooks,
        StorageService storage,
        ProxyLogger logger,
        CommandsConfig commands,
        String proxyId,
        ReportConfig reportConfig
    ) {
        this.platform = platform;
        this.redis = redis;
        this.language = language;
        this.webhooks = webhooks;
        this.storage = storage;
        this.logger = logger;
        this.commands = commands;
        this.proxyId = proxyId;
        this.reportConfig = reportConfig;
        redis.subscribe(ProxyEventType.REPORT_CREATED.id(), this::onRemoteCreated);
        redis.subscribe(ProxyEventType.REPORT_RESOLVED.id(), this::onRemoteResolved);
        loadFromDatabase();
    }

    public void reloadConfig(ReportConfig reportConfig) {
        this.reportConfig = reportConfig;
    }

    public Optional<Long> cooldownRemainingSeconds(UUID uuid) {
        Long last = cooldownMs.get(uuid);
        if (last == null) return Optional.empty();

        long cooldownMs = reportConfig.cooldownSeconds() * 1000L;
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed >= cooldownMs) return Optional.empty();

        return Optional.of((cooldownMs - elapsed + 999) / 1000);
    }

    private final Map<UUID, Long> cooldownMs = new ConcurrentHashMap<>();

    public Optional<String> create(ProxyPlayer reporter, String targetName, String reason, String detail) {
        if (reporter.name().equalsIgnoreCase(targetName)) return Optional.of("self");
        if (cooldownRemainingSeconds(reporter.uuid()).isPresent()) return Optional.of("cooldown");

        String id = nextId();
        String server = reporter.currentServer() == null ? "unknown" : reporter.currentServer();
        Report report = new Report(
            id,
            reporter.uuid(),
            reporter.name(),
            targetName,
            reason.toLowerCase(Locale.ROOT),
            detail == null ? "" : detail,
            server,
            System.currentTimeMillis(),
            ReportStatus.PENDING,
            null
        );
        byId.put(id, report);
        cooldownMs.put(reporter.uuid(), System.currentTimeMillis());
        persistAsync(report);
        notifyStaffLocal(report, false);
        publishCreated(report);
        fireWebhook(report);
        return Optional.empty();
    }

    public List<Report> pendingReports() {
        List<Report> pending = new ArrayList<>();

        for (Report report : byId.values()) {
            if (report.status() == ReportStatus.PENDING) {
                pending.add(report);
            }
        }

        pending.sort((a, b) -> Long.compare(a.createdAt(), b.createdAt()));
        return pending;
    }

    public Optional<Report> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public boolean toggleTitleMute(UUID uuid) {
        if (titleMuted.contains(uuid)) {
            titleMuted.remove(uuid);
            return false;
        }

        titleMuted.add(uuid);
        return true;
    }

    public boolean isTitleMuted(UUID uuid) {
        return titleMuted.contains(uuid);
    }

    public boolean resolve(String id, String resolverName) {
        Report report = byId.get(id);
        if (report == null || report.status() != ReportStatus.PENDING) return false;

        report.resolve(resolverName);
        persistAsync(report);
        publishResolved(report);
        notifyStaffResolved(report);
        return true;
    }

    private void notifyStaffLocal(Report report, boolean remoteOnly) {
        String receivePerm = commands.permission("report_receive");
        Map<String, String> ph = Map.of(
            "id",
            report.id(),
            "reporter",
            report.reporterName(),
            "target",
            report.targetName(),
            "reason",
            report.reason(),
            "detail",
            report.detail().isBlank() ? "-" : report.detail(),
            "server",
            report.server()
        );
        String titleToggleBypass = commands.permission("report_title_bypass");
        for (ProxyPlayer online : platform.onlinePlayers()) {
            if (!online.hasPermission(receivePerm)) continue;

            for (String line : language.rawList("report.notify", ph)) {
                online.sendMessage(line);
            }

            // Title overlay (can disable with report.title.bypass / toggle cmd).
            if (!online.hasPermission(titleToggleBypass) && !titleMuted.contains(online.uuid())) {
                String title = language.raw("report.title_main", ph, online.protocolVersion());
                String sub = language.raw("report.title_sub", ph, online.protocolVersion());
                platform.sendTitle(online.uuid(), title, sub);
            }
        }

        if (!remoteOnly) return;
    }

    private void notifyStaffResolved(Report report) {
        String receivePerm = commands.permission("report_receive");
        String message = language.raw(
            "report.resolved",
            Map.of(
                "id",
                report.id(),
                "target",
                report.targetName(),
                "staff",
                report.resolverName() == null ? "?" : report.resolverName()
            )
        );

        for (ProxyPlayer online : platform.onlinePlayers()) {
            if (online.hasPermission(receivePerm)) {
                online.sendMessage(message);
            }
        }
    }

    private void publishCreated(Report report) {
        JsonObject data = toJson(report);
        redis.publish(new RedisEvent(ProxyEventType.REPORT_CREATED.id(), proxyId, data));
    }

    private void publishResolved(Report report) {
        JsonObject data = new JsonObject();
        data.addProperty("id", report.id());
        data.addProperty("resolver", report.resolverName());
        redis.publish(new RedisEvent(ProxyEventType.REPORT_RESOLVED.id(), proxyId, data));
    }

    private void onRemoteCreated(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        Report report = fromJson(event.data());
        byId.put(report.id(), report);
        notifyStaffLocal(report, true);
    }

    private void onRemoteResolved(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        JsonObject data = event.data();
        if (!data.has("id")) return;

        Report report = byId.get(data.get("id").getAsString());
        if (report != null) {
            report.resolve(data.has("resolver") ? data.get("resolver").getAsString() : "?");
        }
    }

    private void fireWebhook(Report report) {
        webhooks.send(
            "reports",
            Map.of(
                "id",
                report.id(),
                "reporter",
                report.reporterName(),
                "target",
                report.targetName(),
                "player",
                report.targetName(),
                "reason",
                report.reason(),
                "detail",
                report.detail().isBlank() ? "-" : report.detail(),
                "server",
                report.server()
            )
        );
    }

    private String nextId() {
        return String.format("%06d", idSequence.getAndIncrement() % 1_000_000);
    }

    private JsonObject toJson(Report report) {
        JsonObject data = new JsonObject();
        data.addProperty("id", report.id());
        data.addProperty("reporter_uuid", report.reporterUuid().toString());
        data.addProperty("reporter", report.reporterName());
        data.addProperty("target", report.targetName());
        data.addProperty("reason", report.reason());
        data.addProperty("detail", report.detail());
        data.addProperty("server", report.server());
        data.addProperty("created_at", report.createdAt());
        data.addProperty("status", report.status().name());
        return data;
    }

    private Report fromJson(JsonObject data) {
        UUID reporterUuid = data.has("reporter_uuid")
            ? UUID.fromString(data.get("reporter_uuid").getAsString())
            : UUID.randomUUID();
        return new Report(
            data.get("id").getAsString(),
            reporterUuid,
            data.get("reporter").getAsString(),
            data.get("target").getAsString(),
            data.get("reason").getAsString(),
            data.has("detail") ? data.get("detail").getAsString() : "",
            data.has("server") ? data.get("server").getAsString() : "?",
            data.has("created_at") ? data.get("created_at").getAsLong() : System.currentTimeMillis(),
            ReportStatus.PENDING,
            null
        );
    }

    private void loadFromDatabase() {
        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty() || !mysql.get().isConnected()) return;

        platform.asyncExecutor().execute(() -> {
            try {
                mysql.get().call(connection -> {
                    try {
                        loadPendingFromConnection(connection);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }

                    return null;
                });
            } catch (SQLException ex) {
                logger.error("Failed loading reports from MySQL", ex);
            }
        });
    }

    private void persistAsync(Report report) {
        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty() || !mysql.get().isConnected()) return;

        platform.asyncExecutor().execute(() -> {
            try {
                mysql.get().call(connection -> {
                    try {
                        upsert(connection, report);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }

                    return null;
                });
            } catch (SQLException ex) {
                logger.error("Failed persisting report " + report.id(), ex);
            }
        });
    }

    private void loadPendingFromConnection(Connection connection) throws SQLException {
        try (
            PreparedStatement ps = connection.prepareStatement(
                "SELECT id, reporter_uuid, reporter_name, target_name, reason, detail, server, " +
                    "created_at, status, resolver_name FROM ip_reports WHERE status = 'PENDING'"
            )
        ) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Report report = new Report(
                        rs.getString("id"),
                        UUID.fromString(rs.getString("reporter_uuid")),
                        rs.getString("reporter_name"),
                        rs.getString("target_name"),
                        rs.getString("reason"),
                        rs.getString("detail"),
                        rs.getString("server"),
                        rs.getLong("created_at"),
                        ReportStatus.valueOf(rs.getString("status")),
                        rs.getString("resolver_name")
                    );
                    byId.put(report.id(), report);
                }
            }
        }
    }

    private void upsert(Connection connection, Report report) throws SQLException {
        String sql = """
        INSERT INTO ip_reports (id, reporter_uuid, reporter_name, target_name, reason, detail, server,
        created_at, status, resolver_name)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE status = VALUES(status), resolver_name = VALUES(resolver_name)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, report.id());
            ps.setString(2, report.reporterUuid().toString());
            ps.setString(3, report.reporterName());
            ps.setString(4, report.targetName());
            ps.setString(5, report.reason());
            ps.setString(6, report.detail());
            ps.setString(7, report.server());
            ps.setLong(8, report.createdAt());
            ps.setString(9, report.status().name());
            ps.setString(10, report.resolverName());
            ps.executeUpdate();
        }
    }
}
