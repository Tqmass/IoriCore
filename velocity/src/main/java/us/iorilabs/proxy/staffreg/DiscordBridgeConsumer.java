package us.iorilabs.proxy.staffreg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import redis.clients.jedis.JedisPubSub;
import us.iorilabs.proxy.config.DiscordConfig;
import us.iorilabs.proxy.discord.DiscordBotService;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.staffreg.DiscordEmbedDto;
import us.iorilabs.staffreg.DiscordMessageRequest;
import us.iorilabs.staffreg.crypto.BridgeEventSigner;

public final class DiscordBridgeConsumer {

    private final Executor executor;
    private final RedisService redis;
    private final DiscordBotService bot;
    private final DiscordConfig config;
    private final ProxyLogger logger;
    private final String bridgeSecret;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DiscordBridgeConsumer(
        ProxyPlatform platform,
        RedisService redis,
        DiscordBotService bot,
        DiscordConfig config,
        ProxyLogger logger,
        String bridgeSecret
    ) {
        this.executor = platform.asyncExecutor();
        this.redis = redis;
        this.bot = bot;
        this.config = config;
        this.logger = logger;
        this.bridgeSecret = bridgeSecret == null ? "" : bridgeSecret;
    }

    public void start() {
        if (config == null || config.bridge() == null || !config.bridge().enabled()) return;
        if (redis == null || !redis.isConnected()) return;
        if (!running.compareAndSet(false, true)) return;

        String channel = config.bridge().channelIn();
        executor.execute(() -> {
            while (running.get()) {
                redis.withJedis(jedis -> {
                    JedisPubSub pubSub = new JedisPubSub() {
                        @Override
                        public void onMessage(String ch, String message) {
                            handle(message);
                        }
                    };
                    jedis.subscribe(pubSub, channel);
                    return null;
                });
            }
        });
    }

    public void stop() {
        running.set(false);
    }

    private void handle(String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            JsonObject envelope = JsonParser.parseString(raw).getAsJsonObject();
            if (!envelope.has("payload")) return;
            JsonObject payload = envelope.getAsJsonObject("payload");
            if (config.bridge().signEvents() && !bridgeSecret.isBlank()) {
                String sig = envelope.has("sig") ? envelope.get("sig").getAsString() : "";
                if (!BridgeEventSigner.verify(bridgeSecret, payload.toString(), sig)) {
                    logger.warn("StaffReg bridge event rejected (bad signature)");
                    return;
                }
            }
            if (bot == null || !bot.ready()) return;

            DiscordMessageRequest request = new DiscordMessageRequest();
            if (payload.has("discord_id")) request.discordUserId(payload.get("discord_id").getAsString());
            DiscordEmbedDto embed = new DiscordEmbedDto();
            if (payload.has("embed_key")) embed.templateKey(payload.get("embed_key").getAsString());
            if (payload.has("placeholders") && payload.get("placeholders").isJsonObject()) {
                Map<String, String> map = new HashMap<>();
                payload
                    .getAsJsonObject("placeholders")
                    .entrySet()
                    .forEach(entry -> map.put(entry.getKey(), entry.getValue().getAsString()));
                embed.placeholders(map);
            }
            request.embed(embed);
            boolean direct = payload.has("kind") && "dm".equalsIgnoreCase(payload.get("kind").getAsString());
            bot.sendStaffRegMessage(request, direct);
        } catch (Exception ex) {
            logger.warn("StaffReg bridge parse failed: " + ex.getMessage());
        }
    }
}
