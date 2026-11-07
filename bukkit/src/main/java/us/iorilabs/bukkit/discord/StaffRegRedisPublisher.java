package us.iorilabs.bukkit.discord;

import com.google.gson.JsonObject;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.staffreg.DiscordEmbedDto;
import us.iorilabs.staffreg.DiscordMessageRequest;
import us.iorilabs.staffreg.crypto.BridgeEventSigner;

public final class StaffRegRedisPublisher {

    private final IoriCorePlugin plugin;
    private final FileConfiguration redisConfig;

    public StaffRegRedisPublisher(IoriCorePlugin plugin, FileConfiguration redisConfig) {
        this.plugin = plugin;
        this.redisConfig = redisConfig;
    }

    public void publish(DiscordMessageRequest request, boolean direct) {
        if (plugin.redis() == null || !plugin.redis().enabled()) return;
        if (redisConfig == null) return;

        String channelIn = redisConfig.getString("DISCORD-BRIDGE.CHANNEL-IN", "royalctw:discord:in");
        boolean sign = redisConfig.getBoolean("DISCORD-BRIDGE.SIGN-EVENTS", true);
        String secretEnv = redisConfig.getString("DISCORD-BRIDGE.SECRET-ENV", "ROYALCTW_BRIDGE_SECRET");
        String secret = secretEnv == null || secretEnv.isBlank() ? "" : System.getenv(secretEnv);
        if (secret == null) secret = "";

        JsonObject payload = new JsonObject();
        payload.addProperty("kind", direct ? "dm" : "channel");
        if (request.discordUserId() != null) payload.addProperty("discord_id", request.discordUserId());
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

        JsonObject envelope = new JsonObject();
        envelope.addProperty("type", "staffreg_discord");
        envelope.add("payload", payload);
        if (sign && !secret.isBlank()) {
            envelope.addProperty("sig", BridgeEventSigner.sign(secret, payload.toString()));
        }

        plugin.redis().publishRaw(channelIn, envelope.toString());
    }
}
