package us.iorilabs.proxy.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.proxy.discord.enums.DiscordPunishmentType;

@Getter
@Accessors(fluent = true)
public final class DiscordConfig {

    private final boolean enabled;
    private final String token;
    private final String guildId;
    private final String prefix;
    private final boolean sendWebhookEvents;
    private final boolean slashCommands;
    private final boolean prefixCommands;
    private final String commandsChannelId;
    private final List<String> commandRoleIds;
    private final Map<String, String> channels;
    private final Map<String, Boolean> commandToggles;
    private final Map<String, EmbedTemplate> embeds;
    private final BridgeConfig bridge;
    private final StaffChatBlock staffChat;
    private final SimpleDiscordEvent welcome;
    private final SimpleDiscordEvent goodbye;
    private final SimpleDiscordEvent boost;
    private final StoreBlock store;
    private final StatusBlock status;
    private final PunishmentsBlock punishments;
    private final StaffAuthFlowBlock staffAuthFlow;

    public DiscordConfig(
        YamlConfig discordYaml,
        YamlConfig donationsYaml,
        boolean donationsModule,
        boolean punishmentsModule,
        boolean staffAuthModule
    ) {
        YamlConfig primary = discordYaml.section("BOTS").section("PRIMARY");
        YamlConfig loose = discordYaml.section("BOT");

        boolean rootEnabled = discordYaml.getBoolean("ENABLED", false);
        String rootToken = discordYaml.getString("TOKEN", "").trim();
        String rootGuild = discordYaml.getString("GUILD-ID", "").trim();

        boolean primaryEnabled = primary.getBoolean("ENABLED", false);
        String primaryToken = primary.getString("TOKEN", "").trim();
        String primaryGuild = primary.getString("GUILD-ID", "").trim();

        boolean looseEnabled = loose.getBoolean("ENABLED", false);

        this.enabled = rootEnabled || primaryEnabled || looseEnabled;
        if (!rootToken.isBlank()) {
            this.token = rootToken;
        } else if (!primaryToken.isBlank()) {
            this.token = primaryToken;
        } else {
            this.token = loose.getString("TOKEN", "").trim();
        }

        if (!rootGuild.isBlank()) {
            this.guildId = rootGuild;
        } else if (!primaryGuild.isBlank()) {
            this.guildId = primaryGuild;
        } else {
            this.guildId = loose.getString("GUILD-ID", "").trim();
        }

        this.prefix = discordYaml.getString("PREFIX", loose.getString("PREFIX", "!")).trim();
        this.sendWebhookEvents = discordYaml.getBoolean(
            "SEND-WEBHOOK-EVENTS",
            loose.getBoolean("SEND-WEBHOOK-EVENTS", true)
        );

        boolean slash = discordYaml.getBoolean("SLASH-COMMANDS", true);
        if (loose.contains("SLASH-COMMANDS")) {
            slash = loose.getBoolean("SLASH-COMMANDS", slash);
        }

        this.slashCommands = slash;
        this.prefixCommands = discordYaml.getBoolean("PREFIX-COMMANDS", loose.getBoolean("PREFIX-COMMANDS", true));

        YamlConfig topChannels = discordYaml.section("CHANNELS");
        Map<String, String> channelMap = new LinkedHashMap<>();
        for (String key : List.of(
            "staff-chat",
            "punishments",
            "welcome",
            "goodbye",
            "boost",
            "status",
            "staff-auth",
            "security"
        )) {
            channelMap.put(key, topChannels.getString(key, "").trim());
        }
        YamlConfig staffChatYaml = discordYaml.section("STAFF-CHAT");
        if (!staffChatYaml.getString("CHANNEL-ID", "").trim().isBlank()) {
            channelMap.put("staff-chat", staffChatYaml.getString("CHANNEL-ID", "").trim());
        }
        YamlConfig welcomeYaml = discordYaml.section("WELCOME");
        if (!welcomeYaml.getString("CHANNEL-ID", "").trim().isBlank()) {
            channelMap.put("welcome", welcomeYaml.getString("CHANNEL-ID", "").trim());
        }
        YamlConfig goodbyeYaml = discordYaml.section("GOODBYE");
        if (!goodbyeYaml.getString("CHANNEL-ID", "").trim().isBlank()) {
            channelMap.put("goodbye", goodbyeYaml.getString("CHANNEL-ID", "").trim());
        }
        YamlConfig boostYaml = discordYaml.section("BOOST");
        if (!boostYaml.getString("CHANNEL-ID", "").trim().isBlank()) {
            channelMap.put("boost", boostYaml.getString("CHANNEL-ID", "").trim());
        }
        YamlConfig donationsDiscord = donationsYaml == null ? null : donationsYaml.section("DISCORD");
        String storeChannel = donationsDiscord == null ? "" : donationsDiscord.getString("CHANNEL-ID", "").trim();
        if (storeChannel.isBlank()) {
            storeChannel = discordYaml.section("STORE").getString("CHANNEL-ID", "").trim();
        }
        if (!storeChannel.isBlank()) channelMap.put("store", storeChannel);
        YamlConfig statusYaml = discordYaml.section("STATUS");
        if (!statusYaml.getString("CHANNEL-ID", "").trim().isBlank()) {
            channelMap.put("status", statusYaml.getString("CHANNEL-ID", "").trim());
        }
        YamlConfig punishYaml = discordYaml.section("PUNISHMENTS");
        if (!punishYaml.getString("DEFAULT-CHANNEL-ID", "").trim().isBlank()) {
            channelMap.put("punishments", punishYaml.getString("DEFAULT-CHANNEL-ID", "").trim());
        }
        YamlConfig staffAuthFlowYaml = resolveStaffAuthFlow(discordYaml);
        String staffAuthChannel = staffAuthFlowYaml.section("NOTIFY").getString("CHANNEL-ID", "").trim();
        if (staffAuthChannel.isBlank()) staffAuthChannel = topChannels.getString("staff-auth", "").trim();
        if (!staffAuthChannel.isBlank()) channelMap.put("staff-auth", staffAuthChannel);
        this.channels = Map.copyOf(channelMap);

        YamlConfig cmdsLoose = loose.section("COMMANDS");
        YamlConfig cmdsTop = discordYaml.section("COMMANDS");
        String cmdChan = cmdsTop.getString("CHANNEL-ID", "").trim();
        if (cmdChan.isBlank()) cmdChan = cmdsLoose.getString("CHANNEL-ID", "").trim();
        this.commandsChannelId = cmdChan;

        List<String> roles = cmdsTop.getStringList("ROLE-IDS");
        if (roles.isEmpty()) roles = cmdsLoose.getStringList("ROLE-IDS");
        this.commandRoleIds = List.copyOf(roles);

        Map<String, Boolean> toggles = new LinkedHashMap<>();
        for (String key : List.of("ban", "tempban", "unban", "mute", "tempmute", "unmute", "warn", "kick")) {
            String nk = YamlConfig.normalizeKey(key);
            boolean def = cmdsTop.getBoolean(nk, true);
            toggles.put(key, cmdsLoose.getBoolean(key, def));
        }
        this.commandToggles = Map.copyOf(toggles);
        this.embeds = loadEmbeds(discordYaml.section("EMBEDS"), discordYaml.section("PUNISHMENTS"), donationsYaml);
        YamlConfig bridgeYaml = discordYaml.section("BRIDGE");
        this.bridge = new BridgeConfig(
            bridgeYaml.getBoolean("ENABLED", true),
            bridgeYaml.getString("CHANNEL-IN", "royalctw:discord:in").trim(),
            bridgeYaml.getString("CHANNEL-OUT", "royalctw:discord:out").trim(),
            bridgeYaml.getBoolean("SIGN-EVENTS", true),
            bridgeYaml.getString("SECRET-ENV", "ROYALCTW_BRIDGE_SECRET").trim(),
            bridgeYaml.getInt("QUEUE-MAX", 500),
            bridgeYaml.getInt("DROP-AFTER-SECONDS", 120)
        );

        this.staffChat = new StaffChatBlock(
            staffChatYaml.getBoolean("ENABLED", true),
            staffChatYaml.getString("CHANNEL-ID", "").trim(),
            staffChatYaml.getString("WEBHOOK-URL", "").trim(),
            staffChatYaml.getBoolean("AUTO-CREATE-WEBHOOK", false),
            staffChatYaml.getString("USERNAME", "%player%").trim(),
            staffChatYaml.getString("AVATAR-URL", "https://visage.surgeplay.com/bust/152/%player%").trim(),
            staffChatYaml.getString("FORMAT", "**[%server%]** %message%").trim()
        );

        this.welcome = new SimpleDiscordEvent(welcomeYaml.getBoolean("ENABLED", true));
        this.goodbye = new SimpleDiscordEvent(goodbyeYaml.getBoolean("ENABLED", true));
        this.boost = new SimpleDiscordEvent(boostYaml.getBoolean("ENABLED", true));

        boolean storeEnabled = donationsDiscord != null && donationsDiscord.getBoolean("ENABLED", true);
        if (donationsDiscord == null || !donationsDiscord.contains("ENABLED")) {
            storeEnabled = discordYaml.section("STORE").getBoolean("ENABLED", true);
        }
        this.store = new StoreBlock(storeEnabled, donationsModule);

        this.status = new StatusBlock(statusYaml.getBoolean("ENABLED", true));

        Map<String, String> punishmentChannels = new LinkedHashMap<>();
        for (DiscordPunishmentType t : DiscordPunishmentType.values()) {
            String nk = t.name().toLowerCase(Locale.ROOT).replace("_", "-");
            YamlConfig typeYaml = punishYaml.section(t.name());
            String cid = typeYaml.getString("CHANNEL-ID", "").trim();
            punishmentChannels.put(nk, cid);
        }
        this.punishments = new PunishmentsBlock(
            punishYaml.getBoolean("ENABLED", punishmentsModule),
            punishYaml.getString("DEFAULT-CHANNEL-ID", "").trim(),
            Map.copyOf(punishmentChannels)
        );

        YamlConfig overrideYaml = staffAuthFlowYaml.section("OVERRIDE");
        this.staffAuthFlow = new StaffAuthFlowBlock(
            staffAuthFlowYaml.getBoolean("ENABLED", staffAuthModule),
            staffAuthFlowYaml.section("NOTIFY").getString("CHANNEL-ID", "").trim(),
            overrideYaml.getStringList("ROLE-IDS"),
            overrideYaml.getString("MC-PERMISSION", "ioriproxy.staffauth.override").trim(),
            overrideYaml.getString("LOG-CHANNEL-ID", "").trim(),
            staffAuthFlowYaml.section("DM").getBoolean("ENABLED", true),
            discordYaml.getString("BUTTON-SECRET-ENV", "ROYALCTW_BRIDGE_SECRET").trim()
        );
    }

    public String channel(String key) {
        if (key == null) return "";
        return channels.getOrDefault(key.toLowerCase(Locale.ROOT).replace("_", "-"), "");
    }

    public boolean commandEnabled(String name) {
        if (name == null) return false;
        return commandToggles.getOrDefault(name.toLowerCase(Locale.ROOT), false);
    }

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }

    public EmbedTemplate embed(String key) {
        if (key == null) return EmbedTemplate.EMPTY;
        return embeds.getOrDefault(key.toLowerCase(Locale.ROOT).replace("_", "-"), EmbedTemplate.EMPTY);
    }

    public String punishmentChannelId(DiscordPunishmentType type) {
        if (punishments == null || type == null) return "";
        String specific = punishments.channels.get(type.name().toLowerCase(Locale.ROOT).replace("_", "-"));
        if (specific != null && !specific.isBlank()) return specific;
        String def = punishments.defaultChannelId;
        return def == null ? "" : def;
    }

    private static YamlConfig resolveStaffAuthFlow(YamlConfig discordYaml) {
        YamlConfig flow = discordYaml.section("STAFF-AUTH").section("FLOW");
        if (flow.contains("ENABLED") || flow.section("NOTIFY").contains("CHANNEL-ID")) {
            return flow;
        }
        return discordYaml.section("STAFF-AUTH-FLOW");
    }

    private static Map<String, EmbedTemplate> loadEmbeds(
        YamlConfig embedsYaml,
        YamlConfig punishmentsYaml,
        YamlConfig donationsYaml
    ) {
        if (embedsYaml == null) return Map.of();

        Map<String, EmbedTemplate> out = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(
            List.of(
                "staff-chat",
                "welcome",
                "goodbye",
                "boost",
                "status",
                "staff-auth-notify",
                "staff-auth",
                "staff-reg",
                "security"
            )
        );
        if (punishmentsYaml != null) {
            for (DiscordPunishmentType t : DiscordPunishmentType.values()) {
                keys.add("punishment-" + t.name().toLowerCase(Locale.ROOT).replace("_", "-"));
            }
        }
        keys.add("punishment");

        for (String key : keys) {
            EmbedTemplate t = readEmbed(embedsYaml.section(key));
            if (isEmptyTemplate(t) && "punishment".equals(key)) {
                t = readEmbed(embedsYaml.section("PUNISHMENT"));
            }
            if (isEmptyTemplate(t) && "security".equals(key)) {
                t = readEmbed(embedsYaml.section("SECURITY-ALERT"));
            }
            out.put(key, t);
        }
        EmbedTemplate store = EmbedTemplate.EMPTY;
        if (donationsYaml != null) {
            store = readEmbed(donationsYaml.section("DISCORD").section("EMBED"));
        }
        if (isEmptyTemplate(store)) {
            store = readEmbed(embedsYaml.section("STORE"));
        }
        out.put("store", store);
        return Map.copyOf(out);
    }

    private static boolean isEmptyTemplate(EmbedTemplate t) {
        if (t == null) return true;
        return (
            t.authorName().isBlank() &&
            t.authorIconUrl().isBlank() &&
            t.title().isBlank() &&
            t.description().isBlank() &&
            t.footer().isBlank() &&
            t.thumbnailUrl().isBlank()
        );
    }

    @Getter
    @Accessors(fluent = true)
    public static final class StaffChatBlock {

        private final boolean enabled;
        private final String channelId;
        private final String webhookUrl;
        private final boolean autoCreateWebhook;
        private final String username;
        private final String avatarUrl;
        private final String format;

        public StaffChatBlock(
            boolean enabled,
            String channelId,
            String webhookUrl,
            boolean autoCreateWebhook,
            String username,
            String avatarUrl,
            String format
        ) {
            this.enabled = enabled;
            this.channelId = channelId == null ? "" : channelId;
            this.webhookUrl = webhookUrl == null ? "" : webhookUrl;
            this.autoCreateWebhook = autoCreateWebhook;
            this.username = username == null ? "%player%" : username;
            this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
            this.format = format == null ? "" : format;
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static final class SimpleDiscordEvent {

        private final boolean enabled;

        public SimpleDiscordEvent(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static final class StoreBlock {

        private final boolean enabled;
        private final boolean donationsModule;

        public StoreBlock(boolean enabled, boolean donationsModule) {
            this.enabled = enabled;
            this.donationsModule = donationsModule;
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static final class StatusBlock {

        private final boolean enabled;

        public StatusBlock(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static final class PunishmentsBlock {

        private final boolean enabled;
        private final String defaultChannelId;
        private final Map<String, String> channels;

        public PunishmentsBlock(boolean enabled, String defaultChannelId, Map<String, String> channels) {
            this.enabled = enabled;
            this.defaultChannelId = defaultChannelId == null ? "" : defaultChannelId;
            this.channels = channels == null ? Map.of() : channels;
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static final class StaffAuthFlowBlock {

        private final boolean enabled;
        private final String notifyChannelId;
        private final List<String> overrideRoleIds;
        private final String overrideMcPermission;
        private final String overrideLogChannelId;
        private final boolean dmEnabled;
        private final String buttonSecretEnv;

        public StaffAuthFlowBlock(
            boolean enabled,
            String notifyChannelId,
            List<String> overrideRoleIds,
            String overrideMcPermission,
            String overrideLogChannelId,
            boolean dmEnabled,
            String buttonSecretEnv
        ) {
            this.enabled = enabled;
            this.notifyChannelId = notifyChannelId == null ? "" : notifyChannelId;
            this.overrideRoleIds = overrideRoleIds == null ? List.of() : List.copyOf(new ArrayList<>(overrideRoleIds));
            this.overrideMcPermission = overrideMcPermission == null ? "" : overrideMcPermission;
            this.overrideLogChannelId = overrideLogChannelId == null ? "" : overrideLogChannelId;
            this.dmEnabled = dmEnabled;
            this.buttonSecretEnv = buttonSecretEnv == null ? "" : buttonSecretEnv;
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static final class BridgeConfig {

        private final boolean enabled;
        private final String channelIn;
        private final String channelOut;
        private final boolean signEvents;
        private final String secretEnv;
        private final int queueMax;
        private final int dropAfterSeconds;

        public BridgeConfig(
            boolean enabled,
            String channelIn,
            String channelOut,
            boolean signEvents,
            String secretEnv,
            int queueMax,
            int dropAfterSeconds
        ) {
            this.enabled = enabled;
            this.channelIn = channelIn == null ? "" : channelIn;
            this.channelOut = channelOut == null ? "" : channelOut;
            this.signEvents = signEvents;
            this.secretEnv = secretEnv == null ? "ROYALCTW_BRIDGE_SECRET" : secretEnv;
            this.queueMax = queueMax;
            this.dropAfterSeconds = dropAfterSeconds;
        }
    }

    private static EmbedTemplate readEmbed(YamlConfig section) {
        if (section == null) {
            return EmbedTemplate.EMPTY;
        }
        return new EmbedTemplate(
            section.getString("color", "#5865F2"),
            section.getString("author-name", ""),
            section.getString("author-icon-url", ""),
            section.getString("title", ""),
            section.getString("description", ""),
            section.getString("footer", ""),
            section.getString("thumbnail-url", ""),
            section.getBoolean("timestamp", true)
        );
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
            this.authorName = safe(authorName);
            this.authorIconUrl = safe(authorIconUrl);
            this.title = safe(title);
            this.description = safe(description);
            this.footer = safe(footer);
            this.thumbnailUrl = safe(thumbnailUrl);
            this.timestamp = timestamp;
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
