package us.iorilabs.proxy.webhooks;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.proxy.config.YamlConfig;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;

@Accessors(fluent = true)
public final class WebhookRegistry {

    private static final String[] FILES = {
        "reports",
        "request",
        "maintenance",
        "stream",
        "donations",
        "blacklist",
        "status",
        "staff-auth",
    };

    private final ProxyPlatform platform;
    private final ProxyLogger logger;

    @Getter
    private final WebhookClient client;

    private final Map<String, WebhookEndpoint> endpoints = new LinkedHashMap<>();
    private final Map<String, WebhookFormat> formats = new LinkedHashMap<>();
    private volatile BiConsumer<String, Map<String, String>> botMirror;

    public WebhookRegistry(ProxyPlatform platform, ProxyLogger logger) {
        this.platform = platform;
        this.logger = logger;
        this.client = new WebhookClient(platform, logger);
    }

    public void botMirror(BiConsumer<String, Map<String, String>> mirror) {
        this.botMirror = mirror;
    }

    public void load() {
        endpoints.clear();
        formats.clear();

        for (String name : FILES) {
            YamlConfig yaml = new YamlConfig(platform, logger, "webhooks/" + name + ".yml");
            yaml.load();
            loadEndpoint(name, yaml);

            if ("stream".equals(name)) {
                formats.put("stream", readFormat(yaml, "STREAM."));
                formats.put("video", readFormat(yaml, "VIDEO."));
                endpoints.put("video", endpoints.get("stream"));
            } else {
                formats.put(name.toLowerCase(Locale.ROOT), readFormat(yaml, ""));
            }
        }

        long configured = endpoints.values().stream().filter(WebhookEndpoint::isConfigured).distinct().count();
        logger.module("Webhooks", "loaded " + formats.size() + " format(s), " + configured + " endpoint(s) with URL");
    }

    private void loadEndpoint(String name, YamlConfig yaml) {
        endpoints.put(
            name.toLowerCase(Locale.ROOT),
            new WebhookEndpoint(
                name,
                yaml.getBoolean("ENABLED", false),
                yaml.getString("URL", ""),
                yaml.getString("USERNAME", "IoriProxy"),
                yaml.getString("AVATAR-URL", ""),
                parseColor(yaml.getString("EMBED.COLOR", yaml.getString("STREAM.EMBED.COLOR", "#5865F2")), 5814783),
                "",
                ""
            )
        );
    }

    private WebhookFormat readFormat(YamlConfig yaml, String prefix) {
        String embed = prefix + "EMBED.";
        String contentPath = prefix.isBlank() ? "CONTENT" : prefix + "CONTENT";
        String colorPath = prefix.isBlank() ? "EMBED.COLOR" : embed + "COLOR";

        return new WebhookFormat(
            yaml.getString(contentPath, yaml.getString("CONTENT", "")),
            parseColor(yaml.getString(colorPath, "#5865F2"), 5814783),
            yaml.getString(embed + "AUTHOR.NAME", ""),
            yaml.getString(embed + "AUTHOR.URL", ""),
            yaml.getString(embed + "AUTHOR.ICON-URL", ""),
            yaml.getString(embed + "TITLE", ""),
            yaml.getString(embed + "TITLE-URL", ""),
            yaml.getString(embed + "DESCRIPTION", ""),
            yaml.getString(embed + "FOOTER", yaml.getString(embed + "FOOTER.TEXT", "")),
            yaml.getString(embed + "FOOTER-ICON-URL", yaml.getString(embed + "FOOTER.ICON-URL", "")),
            yaml.getString(embed + "THUMBNAIL-URL", ""),
            yaml.getString(embed + "IMAGE-URL", ""),
            yaml.getBoolean(embed + "TIMESTAMP", true)
        );
    }

    public Optional<WebhookEndpoint> get(String name) {
        return Optional.ofNullable(endpoints.get(name.toLowerCase(Locale.ROOT)));
    }

    public Optional<WebhookFormat> format(String name) {
        return Optional.ofNullable(formats.get(name.toLowerCase(Locale.ROOT)));
    }

    public Map<String, WebhookEndpoint> all() {
        return Map.copyOf(endpoints);
    }

    public void send(String name, Map<String, String> placeholders) {
        String key = name == null ? "" : name.toLowerCase(Locale.ROOT);
        Map<String, String> ph = placeholders == null ? Map.of() : placeholders;
        Optional<WebhookEndpoint> endpoint = get(key).filter(WebhookEndpoint::isConfigured);
        Optional<WebhookFormat> format = format(key);
        if (endpoint.isPresent() && format.isPresent()) {
            client.sendFormatted(endpoint.get(), format.get(), ph);
        }

        BiConsumer<String, Map<String, String>> mirror = botMirror;
        if (mirror != null) {
            try {
                mirror.accept(key, ph);
            } catch (Exception ignored) {}
        }
    }

    public void sendContent(String name, String content) {
        get(name)
            .filter(WebhookEndpoint::isConfigured)
            .ifPresent(endpoint -> client.sendContent(endpoint, content));
    }

    public void sendEmbed(String name, String title, String description, Map<String, String> fields) {
        Map<String, String> ph = new LinkedHashMap<>();
        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                ph.put(entry.getKey().toLowerCase(Locale.ROOT).replace(' ', '_'), entry.getValue());
                ph.put(entry.getKey(), entry.getValue());
            }
        }
        if (title != null) ph.put("title", title);
        if (description != null) ph.put("description", description);
        send(name, ph);
    }

    public void sendEmbed(String name, String title, String description, int color, Map<String, String> fields) {
        sendEmbed(name, title, description, fields);
    }

    public static int parseColor(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String value = raw.trim();
        try {
            if (value.startsWith("#")) {
                return Integer.parseInt(value.substring(1), 16);
            }
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Integer.parseInt(value.substring(2), 16);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
