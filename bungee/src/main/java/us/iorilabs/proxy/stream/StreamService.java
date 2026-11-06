package us.iorilabs.proxy.stream;

import com.google.gson.JsonObject;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import us.iorilabs.proxy.config.StreamConfig;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.utilities.ColorUtil;
import us.iorilabs.proxy.webhooks.WebhookRegistry;

public final class StreamService {

    public static final String CLICK_TOKEN = "<stream_click>";

    private static final String[] YOUTUBE_HOSTS = { "youtube.com", "youtu.be", "youtube-nocookie.com" };
    private static final String[] TWITCH_HOSTS = { "twitch.tv" };
    private static final String[] KICK_HOSTS = { "kick.com" };

    private final ProxyPlatform platform;
    private final RedisService redis;
    private final LanguageManager language;
    private final ProxyLogger logger;
    private final WebhookRegistry webhooks;
    private final String proxyId;
    private final Map<UUID, Long> lastBroadcastMs = new ConcurrentHashMap<>();
    private StreamConfig config;

    public StreamService(
        ProxyPlatform platform,
        RedisService redis,
        LanguageManager language,
        ProxyLogger logger,
        WebhookRegistry webhooks,
        String proxyId,
        StreamConfig config
    ) {
        this.platform = platform;
        this.redis = redis;
        this.language = language;
        this.logger = logger;
        this.webhooks = webhooks;
        this.proxyId = proxyId;
        this.config = config;
        redis.subscribe(ProxyEventType.STREAM_BROADCAST.id(), this::onRemote);
    }

    public void reloadConfig(StreamConfig config) {
        this.config = config;
    }

    public Optional<Long> cooldownRemainingSeconds(UUID uuid) {
        Long last = lastBroadcastMs.get(uuid);
        if (last == null) return Optional.empty();

        long elapsed = System.currentTimeMillis() - last;
        long cooldownMs = config.cooldownSeconds() * 1000L;
        if (elapsed >= cooldownMs) return Optional.empty();

        return Optional.of((cooldownMs - elapsed + 999) / 1000);
    }

    public boolean isValidStreamUrl(String raw) {
        if (raw == null || raw.isBlank()) return false;

        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return false;

            String host = uri.getHost();
            if (host == null || host.isBlank()) return false;

            return isAllowedHost(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public void broadcast(ProxyPlayer player, String url, boolean video) {
        String safeUrl = url.trim();
        lastBroadcastMs.put(player.uuid(), System.currentTimeMillis());
        broadcastLocal(player.name(), safeUrl, video);
        JsonObject data = new JsonObject();
        data.addProperty("player", player.name());
        data.addProperty("url", safeUrl);
        data.addProperty("video", video);
        data.addProperty("title", video ? "video" : "stream");
        redis.publish(new RedisEvent(ProxyEventType.STREAM_BROADCAST.id(), proxyId, data));
        fireWebhook(player.name(), safeUrl, video);
        logger.module(video ? "Video" : "Stream", player.name() + " -> " + safeUrl);
    }

    private void onRemote(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        JsonObject data = event.data();
        String player = data.has("player") ? data.get("player").getAsString() : "?";
        String url = data.has("url") ? data.get("url").getAsString() : "";
        boolean video = data.has("video") && data.get("video").getAsBoolean();
        if (!video && data.has("title")) {
            video =
                "Video".equalsIgnoreCase(data.get("title").getAsString()) ||
                "video".equalsIgnoreCase(data.get("title").getAsString());
        }
        broadcastLocal(player, url, video);
    }

    private void broadcastLocal(String playerName, String url, boolean video) {
        Map<String, String> ph = Map.of("player", playerName);
        String key = video ? "stream.broadcast_video" : "stream.broadcast_live";
        for (ProxyPlayer online : platform.onlinePlayers()) {
            int protocol = online.protocolVersion();
            String clickLabel = language.raw(video ? "stream.click_video" : "stream.click_live", null, protocol);
            String hover = language.raw("stream.click_hover", null, protocol);
            List<String> lines = language.rawList(key, ph, protocol);
            if (lines.isEmpty()) {
                lines = language.rawList("stream.broadcast", ph, protocol);
            }

            for (String line : lines) {
                if (line == null) continue;
                if (line.isBlank()) {
                    online.sendMessage(" ");
                    continue;
                }

                if (line.contains(CLICK_TOKEN)) {
                    String prefix = line.substring(0, line.indexOf(CLICK_TOKEN));
                    online.sendMessage(clickableLine(prefix, url, clickLabel, hover));
                } else {
                    online.sendMessage(line);
                }
            }
        }
    }

    private static Component clickableLine(String prefixLegacy, String url, String labelText, String hoverText) {
        Component prefix = prefixLegacy.isBlank() ? Component.empty() : ColorUtil.parse(prefixLegacy);
        Component label = ColorUtil.parse(labelText == null ? "" : labelText)
            .clickEvent(ClickEvent.openUrl(url))
            .hoverEvent(
                HoverEvent.showText(Component.text(url + "\n", NamedTextColor.AQUA).append(ColorUtil.parse(hoverText)))
            );
        return Component.empty().append(prefix).append(label);
    }

    private void fireWebhook(String playerName, String url, boolean video) {
        webhooks.send(video ? "video" : "stream", Map.of("player", playerName, "url", url));
    }

    private static boolean isAllowedHost(String host) {
        return hostMatches(host, YOUTUBE_HOSTS) || hostMatches(host, TWITCH_HOSTS) || hostMatches(host, KICK_HOSTS);
    }

    private static boolean hostMatches(String host, String... roots) {
        for (String root : roots) {
            if (host.equals(root) || host.endsWith("." + root)) return true;
        }
        return false;
    }
}
