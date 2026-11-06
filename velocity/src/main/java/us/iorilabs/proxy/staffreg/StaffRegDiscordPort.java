package us.iorilabs.proxy.staffreg;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import us.iorilabs.proxy.config.DiscordConfig;
import us.iorilabs.proxy.discord.DiscordBotService;
import us.iorilabs.proxy.discord.DiscordEmbeds;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.staffreg.DiscordButtonDto;
import us.iorilabs.staffreg.DiscordEmbedDto;
import us.iorilabs.staffreg.DiscordMessageRequest;
import us.iorilabs.staffreg.DiscordPort;
import us.iorilabs.staffreg.crypto.BridgeEventSigner;

public final class StaffRegDiscordPort implements DiscordPort {

    private final DiscordBotService bot;
    private final DiscordConfig config;
    private final RedisService redis;
    private final ProxyLogger logger;
    private final String bridgeSecret;

    public StaffRegDiscordPort(
        DiscordBotService bot,
        DiscordConfig config,
        RedisService redis,
        ProxyLogger logger,
        String bridgeSecret
    ) {
        this.bot = bot;
        this.config = config;
        this.redis = redis;
        this.logger = logger;
        this.bridgeSecret = bridgeSecret == null ? "" : bridgeSecret;
    }

    @Override
    public CompletableFuture<String> sendDirectMessage(DiscordMessageRequest request) {
        return send(request, true);
    }

    @Override
    public CompletableFuture<String> sendChannelMessage(DiscordMessageRequest request) {
        return send(request, false);
    }

    @Override
    public CompletableFuture<Void> editMessage(String channelId, String messageId, DiscordMessageRequest request) {
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<String> send(DiscordMessageRequest request, boolean direct) {
        if (request == null) return CompletableFuture.completedFuture("");
        if (bot != null && bot.ready()) {
            return CompletableFuture.supplyAsync(() -> bot.sendStaffRegMessage(request, direct));
        }
        publishBridge(request, direct);
        return CompletableFuture.completedFuture("");
    }

    private void publishBridge(DiscordMessageRequest request, boolean direct) {
        if (redis == null || !redis.isConnected() || config == null || config.bridge() == null) return;
        if (!config.bridge().enabled()) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("kind", direct ? "dm" : "channel");
        if (request.discordUserId() != null) payload.addProperty("discord_id", request.discordUserId());
        if (request.channelId() != null) payload.addProperty("channel_id", request.channelId());
        DiscordEmbedDto embed = request.embed();
        payload.addProperty(
            "embed_key",
            embed == null || embed.templateKey() == null ? "staff-auth" : embed.templateKey()
        );
        JsonObject placeholders = new JsonObject();
        if (embed != null && embed.placeholders() != null) {
            for (Map.Entry<String, String> entry : embed.placeholders().entrySet()) {
                placeholders.addProperty(entry.getKey(), entry.getValue());
            }
        }
        payload.add("placeholders", placeholders);

        String body = payload.toString();
        JsonObject envelope = new JsonObject();
        envelope.addProperty("type", "staffreg_discord");
        envelope.add("payload", payload);
        if (config.bridge().signEvents() && !bridgeSecret.isBlank()) {
            envelope.addProperty("sig", BridgeEventSigner.sign(bridgeSecret, body));
        }

        redis.withJedis(jedis -> {
            jedis.publish(config.bridge().channelIn(), envelope.toString());
            return null;
        });
    }

    public static Map<String, String> placeholdersFrom(DiscordMessageRequest request) {
        if (request.embed() == null || request.embed().placeholders() == null) return Map.of();
        return new HashMap<>(request.embed().placeholders());
    }

    public static net.dv8tion.jda.api.entities.MessageEmbed toJdaEmbed(
        DiscordConfig config,
        DiscordMessageRequest request
    ) {
        DiscordEmbedDto dto = request.embed();
        Map<String, String> map = placeholdersFrom(request);
        if (dto != null) {
            DiscordConfig.EmbedTemplate template = new DiscordConfig.EmbedTemplate(
                dto.color(),
                dto.authorName(),
                dto.authorIconUrl(),
                dto.title(),
                dto.description(),
                dto.footer(),
                dto.thumbnailUrl(),
                dto.timestamp()
            );
            return DiscordEmbeds.fromTemplate(template, map);
        }
        String key =
            request.embed() == null || request.embed().templateKey() == null
                ? "staff-auth"
                : request.embed().templateKey();
        return DiscordEmbeds.fromTemplate(config.embed(key), map);
    }

    public static net.dv8tion.jda.api.interactions.components.buttons.Button[] toButtons(
        DiscordMessageRequest request
    ) {
        if (
            request.buttons() == null || request.buttons().isEmpty()
        ) return new net.dv8tion.jda.api.interactions.components.buttons.Button[0];
        return request
            .buttons()
            .stream()
            .map(StaffRegDiscordPort::mapButton)
            .toArray(net.dv8tion.jda.api.interactions.components.buttons.Button[]::new);
    }

    private static net.dv8tion.jda.api.interactions.components.buttons.Button mapButton(DiscordButtonDto dto) {
        net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle style = switch (
            dto.style() == null ? "PRIMARY" : dto.style().toUpperCase()
        ) {
            case "SUCCESS" -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS;
            case "DANGER" -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.DANGER;
            case "SECONDARY" -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SECONDARY;
            default -> net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.PRIMARY;
        };
        String id = dto.signedCustomId() == null ? "staffreg:noop" : dto.signedCustomId();
        return net.dv8tion.jda.api.interactions.components.buttons.Button.of(style, id, dto.label());
    }
}
