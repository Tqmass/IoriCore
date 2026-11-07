package us.iorilabs.bukkit.discord;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

@Getter
@Accessors(fluent = true)
public final class DiscordConfig {

    private final boolean enabled;
    private final String token;
    private final String guildId;
    private final boolean slashCommands;
    private final boolean bridgeEnabled;
    private final String bridgeChannelIn;
    private final String bridgeChannelOut;
    private final boolean bridgeSignEvents;
    private final String bridgeSecretEnv;
    private final int codeTtlSeconds;
    private final int cooldownSeconds;
    private final int redeemCooldownSeconds;
    private final String commandsChannelId;
    private final List<String> commandRoleIds;
    private final Map<String, String> channels;
    private final Map<String, BotEntry> bots;
    private final Map<String, EmbedTemplate> embeds;
    private final Map<String, String> replies;

    public static DiscordConfig fromRedisBridge(FileConfiguration redis) {
        YamlConfiguration view = new YamlConfiguration();
        if (redis != null) {
            view.set("ENABLED", false);
            view.set("BRIDGE.ENABLED", redis.getBoolean("DISCORD-BRIDGE.ENABLED", true));
            view.set("BRIDGE.CHANNEL-IN", redis.getString("DISCORD-BRIDGE.CHANNEL-IN", "royalctw:discord:in"));
            view.set("BRIDGE.CHANNEL-OUT", redis.getString("DISCORD-BRIDGE.CHANNEL-OUT", "royalctw:discord:out"));
            view.set("BRIDGE.SIGN-EVENTS", redis.getBoolean("DISCORD-BRIDGE.SIGN-EVENTS", true));
            view.set("BRIDGE.SECRET-ENV", redis.getString("DISCORD-BRIDGE.SECRET-ENV", "ROYALCTW_BRIDGE_SECRET"));
            view.set("SYNC.CODE-TTL-SECONDS", redis.getInt("DISCORD-BRIDGE.CODE-TTL-SECONDS", 300));
            view.set("SYNC.COOLDOWN-SECONDS", redis.getInt("DISCORD-BRIDGE.COOLDOWN-SECONDS", 30));
            view.set("SYNC.REDEEM-COOLDOWN-SECONDS", redis.getInt("DISCORD-BRIDGE.REDEEM-COOLDOWN-SECONDS", 5));
        }
        return new DiscordConfig(view);
    }

    public DiscordConfig(FileConfiguration yaml) {
        this.enabled = yaml != null && yaml.getBoolean("ENABLED", false);
        this.token = yaml == null ? "" : yaml.getString("TOKEN", "").trim();
        this.guildId = yaml == null ? "" : yaml.getString("GUILD-ID", "").trim();
        this.slashCommands = yaml == null || yaml.getBoolean("SLASH-COMMANDS", true);
        this.bridgeEnabled = yaml != null && yaml.getBoolean("BRIDGE.ENABLED", true);
        this.bridgeChannelIn =
            yaml == null ? "royalctw:discord:in" : yaml.getString("BRIDGE.CHANNEL-IN", "royalctw:discord:in").trim();
        this.bridgeChannelOut =
            yaml == null ? "royalctw:discord:out" : yaml.getString("BRIDGE.CHANNEL-OUT", "royalctw:discord:out").trim();
        this.bridgeSignEvents = yaml == null || yaml.getBoolean("BRIDGE.SIGN-EVENTS", true);
        this.bridgeSecretEnv =
            yaml == null
                ? "ROYALCTW_BRIDGE_SECRET"
                : yaml.getString("BRIDGE.SECRET-ENV", "ROYALCTW_BRIDGE_SECRET").trim();
        this.codeTtlSeconds = yaml == null ? 300 : Math.max(60, yaml.getInt("SYNC.CODE-TTL-SECONDS", 300));
        this.cooldownSeconds = yaml == null ? 30 : Math.max(0, yaml.getInt("SYNC.COOLDOWN-SECONDS", 30));
        this.redeemCooldownSeconds = yaml == null ? 5 : Math.max(1, yaml.getInt("SYNC.REDEEM-COOLDOWN-SECONDS", 5));
        this.commandsChannelId = yaml == null ? "" : yaml.getString("COMMANDS.CHANNEL-ID", "").trim();
        this.commandRoleIds = yaml == null ? List.of() : List.copyOf(yaml.getStringList("COMMANDS.ROLE-IDS"));
        this.channels = loadChannels(yaml);
        this.bots = loadBots(yaml);
        this.embeds = loadEmbeds(yaml);
        this.replies = loadReplies(yaml);
    }

    public boolean useBridgeOnly() {
        return bridgeEnabled;
    }

    public boolean hasToken() {
        return resolveToken() != null && !resolveToken().isBlank();
    }

    public String resolveToken() {
        BotEntry primary = bot("primary");
        if (primary != null && primary.enabled() && primary.hasToken()) return primary.token();
        return token == null ? "" : token;
    }

    public String resolveGuildId() {
        BotEntry primary = bot("primary");
        if (primary != null && primary.enabled() && primary.guildId() != null && !primary.guildId().isBlank()) {
            return primary.guildId();
        }
        return guildId == null ? "" : guildId;
    }

    public String channel(String key) {
        if (key == null || key.isBlank()) return "";
        return channels.getOrDefault(normalize(key), "");
    }

    public BotEntry bot(String id) {
        if (id == null || id.isBlank()) return null;
        return bots.get(normalize(id));
    }

    public Set<String> botIds() {
        return bots.keySet();
    }

    public EmbedTemplate embed(String key) {
        if (key == null) return EmbedTemplate.EMPTY;
        return embeds.getOrDefault(normalize(key), EmbedTemplate.EMPTY);
    }

    public String reply(String key, String fallback) {
        if (key == null || key.isBlank()) return fallback == null ? "" : fallback;
        return replies.getOrDefault(normalize(key), fallback == null ? "" : fallback);
    }

    private static Map<String, String> loadChannels(FileConfiguration yaml) {
        if (yaml == null) return Map.of();

        ConfigurationSection section = yaml.getConfigurationSection("CHANNELS");
        if (section == null) return Map.of();

        Map<String, String> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            String value = section.getString(key, "");
            out.put(normalize(key), value == null ? "" : value.trim());
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, BotEntry> loadBots(FileConfiguration yaml) {
        if (yaml == null) return Map.of();

        ConfigurationSection section = yaml.getConfigurationSection("BOTS");
        if (section == null) return Map.of();

        Map<String, BotEntry> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection bot = section.getConfigurationSection(key);
            if (bot == null) continue;
            out.put(
                normalize(key),
                new BotEntry(
                    bot.getBoolean("ENABLED", false),
                    bot.getString("TOKEN", "").trim(),
                    bot.getString("GUILD-ID", "").trim()
                )
            );
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, EmbedTemplate> loadEmbeds(FileConfiguration yaml) {
        if (yaml == null) return Map.of();

        ConfigurationSection section = yaml.getConfigurationSection("EMBEDS");
        if (section == null) return Map.of();

        Map<String, EmbedTemplate> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection embed = section.getConfigurationSection(key);
            if (embed == null) continue;
            out.put(normalize(key), readEmbed(embed));
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, String> loadReplies(FileConfiguration yaml) {
        if (yaml == null) return Map.of();

        ConfigurationSection section = yaml.getConfigurationSection("REPLIES");
        if (section == null) return Map.of();

        Map<String, String> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            String value = section.getString(key, "");
            out.put(normalize(key), value == null ? "" : value);
        }
        return Collections.unmodifiableMap(out);
    }

    private static EmbedTemplate readEmbed(ConfigurationSection section) {
        return new EmbedTemplate(
            section.getString("COLOR", "#5865F2"),
            section.getString("AUTHOR-NAME", ""),
            section.getString("AUTHOR-ICON-URL", ""),
            section.getString("TITLE", ""),
            section.getString("DESCRIPTION", ""),
            section.getString("FOOTER", ""),
            section.getString("THUMBNAIL-URL", ""),
            section.getBoolean("TIMESTAMP", true)
        );
    }

    private static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    @Getter
    @Accessors(fluent = true)
    public static final class BotEntry {

        private final boolean enabled;
        private final String token;
        private final String guildId;

        public BotEntry(boolean enabled, String token, String guildId) {
            this.enabled = enabled;
            this.token = token == null ? "" : token;
            this.guildId = guildId == null ? "" : guildId;
        }

        public boolean hasToken() {
            return token != null && !token.isBlank();
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static final class EmbedTemplate {

        public static final EmbedTemplate EMPTY = new EmbedTemplate("#5865F2", "", "", "", "", "", "", true);

        private final String color;
        private final String authorName;
        private final String authorIconUrl;
        private final String title;
        private final String description;
        private final String footer;
        private final String thumbnailUrl;
        private final boolean timestamp;

        public EmbedTemplate(
            String color,
            String authorName,
            String authorIconUrl,
            String title,
            String description,
            String footer,
            String thumbnailUrl,
            boolean timestamp
        ) {
            this.color = color == null ? "#5865F2" : color;
            this.authorName = authorName == null ? "" : authorName;
            this.authorIconUrl = authorIconUrl == null ? "" : authorIconUrl;
            this.title = title == null ? "" : title;
            this.description = description == null ? "" : description;
            this.footer = footer == null ? "" : footer;
            this.thumbnailUrl = thumbnailUrl == null ? "" : thumbnailUrl;
            this.timestamp = timestamp;
        }
    }
}
