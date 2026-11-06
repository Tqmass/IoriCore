package us.iorilabs.proxy.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.webhooks.WebhookClient;

public final class DiscordWebhookExecutor {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private DiscordWebhookExecutor() {}

    public static void sendContent(
        ProxyPlatform platform,
        ProxyLogger logger,
        String webhookUrl,
        String username,
        String avatarUrl,
        String content,
        Map<String, String> ph
    ) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        String user = WebhookClient.apply(username, ph);
        String avatar = WebhookClient.apply(avatarUrl, ph);
        String text = WebhookClient.apply(content, ph);
        if (text == null || text.isBlank()) return;

        JsonObject root = new JsonObject();
        if (user != null && !user.isBlank()) root.addProperty("username", user);
        if (avatar != null && !avatar.isBlank()) root.addProperty("avatar_url", avatar);
        root.addProperty("content", text);

        JsonObject allowed = new JsonObject();
        allowed.add("parse", new JsonArray());
        root.add("allowed_mentions", allowed);

        post(platform, logger, webhookUrl, root);
    }

    private static void post(ProxyPlatform platform, ProxyLogger logger, String webhookUrl, JsonObject body) {
        platform.asyncExecutor().execute(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    logger.warn("Staff-chat webhook HTTP " + response.statusCode());
                }
            } catch (Exception ex) {
                logger.warn("Staff-chat webhook failed: " + ex.getMessage());
            }
        });
    }
}
