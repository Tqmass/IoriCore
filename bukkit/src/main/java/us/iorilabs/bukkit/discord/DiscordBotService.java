package us.iorilabs.bukkit.discord;

import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.discord.link.DiscordLinkService;
import us.iorilabs.bukkit.logging.BukkitLogger;

@Getter
@Accessors(fluent = true)
public final class DiscordBotService {

    private final IoriCorePlugin plugin;
    private final BukkitLogger logger;

    private volatile DiscordConfig config;
    private volatile DiscordLinkService links;
    private volatile boolean bridgeReady;

    public DiscordBotService(IoriCorePlugin plugin, DiscordConfig config) {
        this.plugin = plugin;
        this.logger = plugin.coreLogger();
        this.config = config;
    }

    public void bind(DiscordLinkService links) {
        this.links = links;
    }

    public void start() {
        stop();
        DiscordConfig cfg = config;
        if (cfg == null || !cfg.enabled()) {
            logger.module("Discord", "bot disabled");
            bridgeReady = false;
            return;
        }
        bridgeReady = cfg.bridgeEnabled();
        if (bridgeReady) {
            logger.module("Discord", "BRIDGE-only · JDA owned by proxy");
        } else {
            logger.module("Discord", "enabled without BRIDGE · Discord features limited to link SQL");
        }
    }

    public void reload(DiscordConfig config) {
        this.config = config;
        start();
    }

    public void stop() {
        bridgeReady = false;
    }

    public boolean ready() {
        return bridgeReady && config != null && config.enabled();
    }
}
