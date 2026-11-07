package us.iorilabs.proxy.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;

@Accessors(fluent = true)
public final class ConfigManager {

    private static final String[] FILES = {
        "settings.yml",
        "redis.yml",
        "database.yml",
        "permissions.yml",
        "motd.yml",
        "fallback.yml",
        "commands.yml",
        "maintenance.yml",
        "donations.yml",
        "discord.yml",
    };

    private final ProxyPlatform platform;
    private final ProxyLogger logger;
    private final Map<String, YamlConfig> files = new LinkedHashMap<>();

    @Getter
    private SettingsConfig settings;

    @Getter
    private RedisConfig redis;

    @Getter
    private DatabaseConfig database;

    @Getter
    private PermissionsConfig permissions;

    @Getter
    private MotdConfig motd;

    @Getter
    private LobbyConfig lobby;

    @Getter
    private CommandsConfig commands;

    @Getter
    private CommandWhitelistConfig commandWhitelist;

    @Getter
    private MaintenanceConfig maintenance;

    @Getter
    private PlaytimeConfig playtime;

    @Getter
    private AnnouncementsConfig announcements;

    @Getter
    private StreamConfig stream;

    @Getter
    private ReportConfig report;

    @Getter
    private HelpOpConfig helpOp;

    @Getter
    private BlacklistConfig blacklist;

    @Getter
    private DonationsConfig donations;

    @Getter
    private FriendsConfig friends;

    @Getter
    private AnalyticsConfig analytics;

    @Getter
    private StaffAuthConfig staffAuth;

    @Getter
    private DiscordConfig discord;

    @Getter
    private StaffRegConfig staffReg;

    public ConfigManager(ProxyPlatform platform, ProxyLogger logger) {
        this.platform = platform;
        this.logger = logger;
    }

    public void loadAll() {
        files.clear();

        for (String file : FILES) {
            YamlConfig config = new YamlConfig(platform, logger, file);
            config.load();
            files.put(file, config);
        }

        YamlConfig settingsYaml = require("settings.yml");
        YamlConfig discordYaml = require("discord.yml");

        settings = new SettingsConfig(settingsYaml, discordYaml);
        redis = new RedisConfig(require("redis.yml"));
        database = new DatabaseConfig(require("database.yml"));
        permissions = new PermissionsConfig(require("permissions.yml"));
        motd = new MotdConfig(require("motd.yml"));
        lobby = new LobbyConfig(require("fallback.yml"));
        commands = new CommandsConfig(require("commands.yml"), permissions);
        commandWhitelist = new CommandWhitelistConfig(
            require("settings.yml").section("command-whitelist"),
            permissions
        );
        maintenance = new MaintenanceConfig(require("maintenance.yml"));
        playtime = new PlaytimeConfig(require("settings.yml"));
        announcements = new AnnouncementsConfig(require("settings.yml").section("announcements"));
        stream = new StreamConfig(settings);
        report = new ReportConfig(settings);
        helpOp = new HelpOpConfig(settings);
        blacklist = new BlacklistConfig();
        donations = new DonationsConfig(require("donations.yml"));
        friends = new FriendsConfig(require("settings.yml"), settings);
        analytics = new AnalyticsConfig(require("settings.yml").section("analytics"));
        staffAuth = new StaffAuthConfig(discordYaml);
        discord = new DiscordConfig(
            discordYaml,
            require("donations.yml"),
            settings.moduleEnabled("donations"),
            settings.moduleEnabled("punishments"),
            settings.moduleEnabled("staff-auth")
        );
        staffReg = new StaffRegConfig(discordYaml);

        ConfigValidation validation = new ConfigValidation();
        settings.validate(validation);

        if (redis.enabled()) {
            redis.validate(validation);
        }

        if (validation.hasErrors()) {
            validation.report(logger);
            logger.warn("Configuration loaded with validation warnings.");
        } else {
            logger.success("All configuration files loaded (" + FILES.length + ").");
        }
    }

    public void reloadAll() {
        loadAll();
    }

    public YamlConfig require(String fileName) {
        YamlConfig config = files.get(fileName);
        if (config == null) {
            throw new IllegalStateException("Missing config: " + fileName);
        }

        return config;
    }

    public YamlConfig get(String fileName) {
        return files.get(fileName);
    }
}
