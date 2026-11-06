package us.iorilabs.proxy.analytics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import us.iorilabs.proxy.config.AnalyticsConfig;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;

public final class AnalyticsService {

    private final ProxyPlatform platform;
    private final StorageService storage;
    private final AnalyticsConfig config;
    private final ProxyLogger logger;
    private final String proxyId;
    private final AtomicLong joinCount = new AtomicLong();
    private final AtomicInteger peakCount = new AtomicInteger();
    private volatile LocalDate lastPersistDay = LocalDate.now(ZoneOffset.UTC);

    public AnalyticsService(
        ProxyPlatform platform,
        StorageService storage,
        AnalyticsConfig config,
        ProxyLogger logger,
        String proxyId
    ) {
        this.platform = platform;
        this.storage = storage;
        this.config = config;
        this.logger = logger;
        this.proxyId = proxyId;
        platform.runTimer(this::tick, 60_000L, 60_000L, "ioriproxy-analytics");
    }

    public void recordJoin() {
        joinCount.incrementAndGet();
    }

    public void recordOnlineCount(int online) {
        peakCount.updateAndGet(current -> Math.max(current, online));
    }

    public long joins() {
        return joinCount.get();
    }

    public int peakOnline() {
        return peakCount.get();
    }

    public String summary() {
        return "joins=" + joinCount.get() + ", peak=" + peakCount.get();
    }

    public void persistDay(LocalDate day) {
        if (!storage.isConnected()) return;

        platform.asyncExecutor().execute(() ->
            storage.mysql().ifPresent(mysql -> {
                try {
                    mysql.call(connection -> {
                        try {
                            upsertDay(connection, day);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }

                        return null;
                    });
                } catch (Exception ex) {
                    logger.error("Analytics SQL persist failed", ex);
                }
            })
        );
    }

    private void tick() {
        recordOnlineCount(platform.onlinePlayers().size());
        if (!config.persistDailySql() || !storage.isConnected()) return;

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (today.equals(lastPersistDay)) return;

        persistDay(lastPersistDay);
        lastPersistDay = today;
    }

    private void upsertDay(Connection connection, LocalDate day) throws Exception {
        long joinDelta = joinCount.get();
        int peak = peakCount.get();
        try (
            PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO ip_analytics (stat_day, proxy_id, joins, peak_online)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  joins = joins + ?,
                  peak_online = CASE WHEN peak_online > ? THEN peak_online ELSE ? END
                """
            )
        ) {
            ps.setDate(1, java.sql.Date.valueOf(day));
            ps.setString(2, proxyId);
            ps.setLong(3, joinDelta);
            ps.setInt(4, peak);
            ps.setLong(5, joinDelta);
            ps.setInt(6, peak);
            ps.setInt(7, peak);
            ps.executeUpdate();
        }
    }
}
