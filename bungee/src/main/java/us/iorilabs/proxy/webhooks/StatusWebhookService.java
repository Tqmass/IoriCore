package us.iorilabs.proxy.webhooks;

import java.util.Map;
import us.iorilabs.proxy.config.YamlConfig;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.maintenance.MaintenanceService;
import us.iorilabs.proxy.platform.ProxyPlatform;

public final class StatusWebhookService {

    private static final String TASK_ID = "discord-status-webhook";

    private final ProxyPlatform platform;
    private final WebhookRegistry webhooks;
    private final MaintenanceService maintenance;
    private final ProxyLogger logger;
    private final String proxyId;
    private boolean enabled;
    private int intervalSeconds;

    public StatusWebhookService(
        ProxyPlatform platform,
        WebhookRegistry webhooks,
        MaintenanceService maintenance,
        ProxyLogger logger,
        String proxyId
    ) {
        this.platform = platform;
        this.webhooks = webhooks;
        this.maintenance = maintenance;
        this.logger = logger;
        this.proxyId = proxyId;
    }

    public void reload() {
        stop();
        YamlConfig yaml = new YamlConfig(platform, logger, "webhooks/status.yml");
        yaml.load();
        enabled = yaml.getBoolean("ENABLED", false);
        intervalSeconds = Math.max(15, yaml.getInt("UPDATE-INTERVAL-SECONDS", 60));

        if (enabled && webhooks.get("status").filter(WebhookEndpoint::isConfigured).isPresent()) {
            long periodMs = intervalSeconds * 1000L;
            platform.runTimer(this::tick, periodMs, periodMs, TASK_ID);
            tick();
            logger.module("Discord", "status webhook every " + intervalSeconds + "s");
        }
    }

    public void stop() {
        platform.cancelTask(TASK_ID);
    }

    private void tick() {
        if (!enabled) return;

        webhooks.send(
            "status",
            Map.of(
                "online",
                String.valueOf(platform.onlinePlayers().size()),
                "proxy",
                proxyId,
                "servers",
                String.valueOf(platform.registeredServers().size()),
                "maintenance",
                maintenance.isGlobalActive() ? "Yes" : "No"
            )
        );
    }
}
