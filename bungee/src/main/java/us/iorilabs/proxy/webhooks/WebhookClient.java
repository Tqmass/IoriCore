package us.iorilabs.proxy.webhooks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;

public final class WebhookClient {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final ProxyPlatform platform;
    private final ProxyLogger logger;

    public WebhookClient(ProxyPlatform platform, ProxyLogger logger) {
        this.platform = platform;
        this.logger = logger;
    }

    public void sendContent(WebhookEndpoint endpoint, String content) {
        if (endpoint == null || !endpoint.isConfigured()) return;

        JsonObject root = basePayload(endpoint);
        root.addProperty("content", content == null ? "" : content);
        post(endpoint, root);
    }

    public void sendFormatted(WebhookEndpoint endpoint, WebhookFormat format, Map<String, String> placeholders) {
        if (endpoint == null || !endpoint.isConfigured() || format == null) return;

        Map<String, String> ph = placeholders == null ? Map.of() : placeholders;
        JsonObject root = basePayload(endpoint, ph);
        String content = apply(format.content(), ph);
        if (!content.isBlank()) {
            root.addProperty("content", content);
        }

        JsonObject embed = new JsonObject();
        int color = format.color() != 0 ? format.color() : endpoint.color();
        embed.addProperty("color", color);

        String authorName = apply(format.authorName(), ph);
        if (!authorName.isBlank()) {
            JsonObject author = new JsonObject();
            author.addProperty("name", authorName);
            String authorUrl = apply(format.authorUrl(), ph);
            String authorIcon = apply(format.authorIconUrl(), ph);
            if (!authorUrl.isBlank()) author.addProperty("url", authorUrl);
            if (!authorIcon.isBlank()) author.addProperty("icon_url", authorIcon);
            embed.add("author", author);
        }

        String title = apply(format.title(), ph);
        if (!title.isBlank()) {
            embed.addProperty("title", title);
            String titleUrl = apply(format.titleUrl(), ph);
            if (!titleUrl.isBlank()) embed.addProperty("url", titleUrl);
        }

        String description = apply(format.description(), ph);
        if (!description.isBlank()) {
            embed.addProperty("description", description);
        }

        String footerText = apply(format.footer(), ph);
        if (!footerText.isBlank()) {
            JsonObject footer = new JsonObject();
            footer.addProperty("text", footerText);
            String footerIcon = apply(format.footerIconUrl(), ph);
            if (!footerIcon.isBlank()) footer.addProperty("icon_url", footerIcon);
            embed.add("footer", footer);
        }

        String thumbnail = apply(format.thumbnailUrl(), ph);
        if (!thumbnail.isBlank()) {
            JsonObject thumb = new JsonObject();
            thumb.addProperty("url", thumbnail);
            embed.add("thumbnail", thumb);
        }

        String imageUrl = apply(format.imageUrl(), ph);
        if (!imageUrl.isBlank()) {
            JsonObject image = new JsonObject();
            image.addProperty("url", imageUrl);
            embed.add("image", image);
        }

        if (format.timestamp()) {
            embed.addProperty("timestamp", Instant.now().toString());
        }

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        root.add("embeds", embeds);
        post(endpoint, root);
    }

    private JsonObject basePayload(WebhookEndpoint endpoint) {
        return basePayload(endpoint, Map.of());
    }

    private JsonObject basePayload(WebhookEndpoint endpoint, Map<String, String> placeholders) {
        JsonObject root = new JsonObject();
        if (endpoint.username() != null && !endpoint.username().isBlank()) {
            root.addProperty("username", apply(endpoint.username(), placeholders));
        }
        if (endpoint.avatarUrl() != null && !endpoint.avatarUrl().isBlank()) {
            String avatar = apply(endpoint.avatarUrl(), placeholders);
            if (!avatar.isBlank()) {
                root.addProperty("avatar_url", avatar);
            }
        }
        return root;
    }

    private void post(WebhookEndpoint endpoint, JsonObject body) {
        platform.asyncExecutor().execute(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint.url()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    logger.warn("Webhook " + endpoint.name() + " HTTP " + response.statusCode());
                }
            } catch (Exception ex) {
                logger.error("Webhook " + endpoint.name() + " failed", ex);
            }
        });
    }

    public static String apply(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty()) return "";
        String value = template;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String replacement = entry.getValue() == null ? "" : entry.getValue();
                value = value.replace("<" + entry.getKey() + ">", replacement);
                value = value.replace("{" + entry.getKey() + "}", replacement);
                value = value.replace("%" + entry.getKey() + "%", replacement);
            }
        }
        return value;
    }
}
