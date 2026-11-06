package us.iorilabs.bukkit.discord;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.discord.link.DiscordLinkService;
import us.iorilabs.bukkit.logging.BukkitLogger;
import us.iorilabs.bukkit.redis.CoreRedisEvent;
import us.iorilabs.bukkit.redis.CoreRedisService;

public final class StaffAuthDiscordBridge {

    public static final String APPROVE_PREFIX = "staffauth:approve:";
    public static final String DENY_PREFIX = "staffauth:deny:";

    private final IoriCorePlugin plugin;
    private final DiscordBotService bot;
    private final DiscordLinkService links;
    private final BukkitLogger logger;
    private final Map<String, UUID> pendingByToken = new ConcurrentHashMap<>();

    public StaffAuthDiscordBridge(IoriCorePlugin plugin, DiscordBotService bot, DiscordLinkService links) {
        this.plugin = plugin;
        this.bot = bot;
        this.links = links;
        this.logger = plugin.coreLogger();

        CoreRedisService redis = plugin.redis();
        if (redis != null) {
            redis.subscribe(NetworkEventType.STAFF_AUTH_REQUEST, this::onRequest);
        }
    }

    private void onRequest(CoreRedisEvent event) {
        if (event == null || event.data() == null || !bot.ready()) return;

        JsonObject data = event.data();
        String uuidRaw = string(data, NetworkEventKeys.UUID);
        String token = string(data, NetworkEventKeys.TOKEN);
        if (uuidRaw == null || token == null || token.isBlank()) return;

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidRaw);
        } catch (IllegalArgumentException ex) {
            return;
        }

        pendingByToken.put(token, uuid);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", stringOr(data, NetworkEventKeys.PLAYER, "?"));
        placeholders.put("uuid", uuid.toString());
        placeholders.put("ip", stringOr(data, NetworkEventKeys.IP, "?"));
        placeholders.put("country", stringOr(data, NetworkEventKeys.COUNTRY, "?"));
        placeholders.put("previous_country", stringOr(data, "previous_country", "?"));
        placeholders.put("token", token);

        Optional<String> discordId = links.discordId(uuid);
        if (discordId.isPresent()) {
            sendDm(discordId.get(), token, placeholders);
            return;
        }

        sendChannelFallback(token, placeholders);
    }

    private void sendDm(String discordId, String token, Map<String, String> placeholders) {
        User user = bot.jda().getUserById(discordId);
        if (user == null) {
            bot.jda()
                .retrieveUserById(discordId)
                .queue(
                    retrieved -> openDm(retrieved, token, placeholders),
                    err -> sendChannelFallback(token, placeholders)
                );
            return;
        }
        openDm(user, token, placeholders);
    }

    private void openDm(User user, String token, Map<String, String> placeholders) {
        if (user == null) {
            sendChannelFallback(token, placeholders);
            return;
        }

        user.openPrivateChannel().queue(
            channel -> sendAuthMessage(channel, token, placeholders),
            err -> {
                logger.warn("Discord staff-auth DM failed · fallback channel");
                sendChannelFallback(token, placeholders);
            }
        );
    }

    private void sendChannelFallback(String token, Map<String, String> placeholders) {
        String channelId = bot.config().channel("staff-auth");
        if (channelId == null || channelId.isBlank()) {
            logger.warn("Discord staff-auth: no linked Discord and CHANNELS.STAFF-AUTH empty");
            return;
        }

        TextChannel channel = bot.jda().getTextChannelById(channelId);
        if (channel == null) {
            logger.warn("Discord staff-auth channel not found: " + channelId);
            return;
        }
        sendAuthMessage(channel, token, placeholders);
    }

    private void sendAuthMessage(
        net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel,
        String token,
        Map<String, String> placeholders
    ) {
        var embed = DiscordEmbeds.fromTemplate(bot.config().embed("staff-auth"), placeholders);
        channel
            .sendMessageEmbeds(embed)
            .setActionRow(
                Button.success(APPROVE_PREFIX + token, "Approve").withEmoji(Emoji.fromUnicode("✅")),
                Button.danger(DENY_PREFIX + token, "Deny").withEmoji(Emoji.fromUnicode("⛔"))
            )
            .queue(null, err -> logger.warn("Discord staff-auth send failed: " + err.getMessage()));
    }

    public void sendRegistrationAck(String discordId, String playerName) {
        if (!bot.ready() || discordId == null || discordId.isBlank()) return;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName == null ? "?" : playerName);
        placeholders.put("discord_id", discordId);

        bot.jda()
            .retrieveUserById(discordId)
            .queue(
                user ->
                    user.openPrivateChannel().queue(
                        channel -> {
                            var embed = DiscordEmbeds.fromTemplate(bot.config().embed("staff-reg"), placeholders);
                            channel
                                .sendMessageEmbeds(embed)
                                .queue(null, err -> logger.warn("Discord staff-reg DM failed"));
                        },
                        err -> logger.warn("Discord staff-reg DM open failed")
                    ),
                err -> logger.warn("Discord staff-reg user lookup failed")
            );
    }

    public ButtonResult handleButton(String componentId, User clicker) {
        if (componentId == null || clicker == null) return ButtonResult.FAIL;

        boolean approve = componentId.startsWith(APPROVE_PREFIX);
        boolean deny = componentId.startsWith(DENY_PREFIX);
        if (!approve && !deny) return ButtonResult.FAIL;

        String token = componentId.substring((approve ? APPROVE_PREFIX : DENY_PREFIX).length()).trim();
        if (token.isBlank()) return ButtonResult.FAIL;

        UUID targetUuid = pendingByToken.get(token);
        Optional<UUID> actorUuid = links.uuidByDiscord(clicker.getId());
        if (approve && targetUuid != null && actorUuid.filter(targetUuid::equals).isPresent()) {
            return ButtonResult.SELF_APPROVE;
        }

        pendingByToken.remove(token);
        publishResponse(token, approve, clicker.getName(), actorUuid.orElse(null), clicker.getId());
        return approve ? ButtonResult.APPROVED : ButtonResult.DENIED;
    }

    public void publishResponse(String token, boolean approved, String actor, UUID actorUuid, String discordId) {
        CoreRedisService redis = plugin.redis();
        if (redis == null || !redis.enabled()) return;

        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.TOKEN, token);
        data.addProperty(NetworkEventKeys.APPROVED, approved);
        data.addProperty(NetworkEventKeys.ACTOR, actor == null ? "Discord" : actor);
        if (actorUuid != null) data.addProperty("actor_uuid", actorUuid.toString());
        if (discordId != null) data.addProperty(NetworkEventKeys.DISCORD_ID, discordId);
        redis.publish(NetworkEventType.STAFF_AUTH_RESPONSE, data);
        logger.module(
            "Discord",
            "staff-auth " + (approved ? "approve" : "deny") + " by &f" + (actor == null ? "?" : actor)
        );
    }

    private static String string(JsonObject data, String key) {
        if (data == null || key == null || !data.has(key) || data.get(key).isJsonNull()) return null;
        return data.get(key).getAsString();
    }

    private static String stringOr(JsonObject data, String key, String fallback) {
        String value = string(data, key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public enum ButtonResult {
        APPROVED,
        DENIED,
        SELF_APPROVE,
        FAIL,
    }
}
