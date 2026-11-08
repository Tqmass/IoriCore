package us.iorilabs.proxy;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.proxy.adminchat.AdminChatService;
import us.iorilabs.proxy.alts.AltService;
import us.iorilabs.proxy.analytics.AnalyticsService;
import us.iorilabs.proxy.announcements.AnnouncementService;
import us.iorilabs.proxy.blacklist.BlacklistService;
import us.iorilabs.proxy.commands.AdminChatCommand;
import us.iorilabs.proxy.commands.AlertCommand;
import us.iorilabs.proxy.commands.AltsCommand;
import us.iorilabs.proxy.commands.AnalyticsCommand;
import us.iorilabs.proxy.commands.AnnounceCommand;
import us.iorilabs.proxy.commands.BlacklistCommand;
import us.iorilabs.proxy.commands.CommandBlockerService;
import us.iorilabs.proxy.commands.CommandRegistry;
import us.iorilabs.proxy.commands.DonationCommand;
import us.iorilabs.proxy.commands.FindCommand;
import us.iorilabs.proxy.commands.FriendsCommand;
import us.iorilabs.proxy.commands.GgWaveCommand;
import us.iorilabs.proxy.commands.GlistCommand;
import us.iorilabs.proxy.commands.GotoCommand;
import us.iorilabs.proxy.commands.HubCommand;
import us.iorilabs.proxy.commands.MaintenanceCommand;
import us.iorilabs.proxy.commands.PingCommand;
import us.iorilabs.proxy.commands.PlaytimeCommand;
import us.iorilabs.proxy.commands.ProfileCommand;
import us.iorilabs.proxy.commands.ProxyCommand;
import us.iorilabs.proxy.commands.RedisTestCommand;
import us.iorilabs.proxy.commands.ReportCommand;
import us.iorilabs.proxy.commands.ReportsCommand;
import us.iorilabs.proxy.commands.RequestCommand;
import us.iorilabs.proxy.commands.RootCommand;
import us.iorilabs.proxy.commands.SeenCommand;
import us.iorilabs.proxy.commands.SendCommand;
import us.iorilabs.proxy.commands.ServersCommand;
import us.iorilabs.proxy.commands.StaffChatCommand;
import us.iorilabs.proxy.commands.StaffRegCommand;
import us.iorilabs.proxy.commands.StreamCommand;
import us.iorilabs.proxy.commands.SyncCommand;
import us.iorilabs.proxy.commands.TabCompleteFilterService;
import us.iorilabs.proxy.commands.TransferCommand;
import us.iorilabs.proxy.commands.UnsyncCommand;
import us.iorilabs.proxy.config.ConfigManager;
import us.iorilabs.proxy.database.NoopStorageService;
import us.iorilabs.proxy.database.StorageFactory;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.discord.DiscordBotService;
import us.iorilabs.proxy.donations.DonationService;
import us.iorilabs.proxy.helpop.HelpOpService;
import us.iorilabs.proxy.hook.luckperms.LuckPermsHook;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.LibraryLogSilence;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.maintenance.MaintenanceService;
import us.iorilabs.proxy.modules.ModuleManager;
import us.iorilabs.proxy.motd.MotdService;
import us.iorilabs.proxy.placeholders.PlaceholderService;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.PlayerDataService;
import us.iorilabs.proxy.player.friends.FriendService;
import us.iorilabs.proxy.player.playtime.PlaytimeService;
import us.iorilabs.proxy.player.transfer.AccountTransferService;
import us.iorilabs.proxy.punishment.BanCheckService;
import us.iorilabs.proxy.punishment.MuteCheckService;
import us.iorilabs.proxy.redis.JedisRedisService;
import us.iorilabs.proxy.redis.NoopRedisService;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.report.ReportService;
import us.iorilabs.proxy.server.PresenceService;
import us.iorilabs.proxy.server.ServerRouter;
import us.iorilabs.proxy.staff.StaffAlertService;
import us.iorilabs.proxy.staff.StaffGroups;
import us.iorilabs.proxy.staffchat.StaffChatService;
import us.iorilabs.proxy.staffreg.DiscordBridgeConsumer;
import us.iorilabs.proxy.staffreg.StaffRegService;
import us.iorilabs.proxy.stream.StreamService;
import us.iorilabs.proxy.utilities.Validate;
import us.iorilabs.proxy.vanish.VanishPresenceService;
import us.iorilabs.proxy.webhooks.StatusWebhookService;
import us.iorilabs.proxy.webhooks.WebhookRegistry;

@Getter
@Accessors(fluent = true)
public final class IoriProxy {

    public static final String NAME = "IoriProxy";
    public static final String PERMISSION_ROOT = "ioriproxy";

    private static IoriProxy instance;

    private final ProxyPlatform platform;
    private final ProxyLogger logger;
    private final ConfigManager configs;
    private final LanguageManager language;
    private final ModuleManager modules;
    private final CommandRegistry commandRegistry;
    private final WebhookRegistry webhooks;
    private RedisService redis;
    private StorageService storage;
    private PresenceService presence;
    private ServerRouter router;
    private MaintenanceService maintenance;
    private MotdService motd;
    private BlacklistService blacklist;
    private CommandBlockerService commandBlocker;
    private LuckPermsHook luckPerms;
    private StaffGroups staffGroups;
    private StaffAlertService staffAlerts;
    private StaffChatService staffChat;
    private AdminChatService adminChat;
    private StreamService streamService;
    private AnnouncementService announcements;
    private TabCompleteFilterService tabCompleteFilter;
    private ReportService reportService;
    private HelpOpService helpOpService;
    private DonationService donationService;
    private StatusWebhookService statusWebhook;
    private DiscordBotService discordBot;
    private us.iorilabs.proxy.discord.link.DiscordLinkService discordLinks;
    private PlaytimeService playtime;
    private AnalyticsService analytics;
    private PlaceholderService placeholders;
    private FriendService friendService;
    private PlayerDataService playerData;
    private AltService alts;
    private AccountTransferService accountTransfer;
    private StaffRegService staffReg;
    private DiscordBridgeConsumer staffRegBridge;
    private BanCheckService banCheck;
    private MuteCheckService muteCheck;
    private VanishPresenceService vanishPresence;
    private boolean enabled;

    public IoriProxy(ProxyPlatform platform) {
        Validate.notNull(platform, "platform");
        this.platform = platform;
        this.logger = new ProxyLogger(platform);
        this.configs = new ConfigManager(platform, logger);
        this.language = new LanguageManager(platform, logger);
        this.modules = new ModuleManager(logger);
        this.commandRegistry = new CommandRegistry();
        this.webhooks = new WebhookRegistry(platform, logger);
        this.redis = new NoopRedisService(us.iorilabs.proxy.config.SettingsConfig.ORIGIN_PROXY_ID);
        this.storage = new NoopStorageService();
        instance = this;
    }

    public static IoriProxy get() {
        return instance;
    }

    public void enable() {
        if (enabled) return;

        LibraryLogSilence.apply();
        logger.banner(platform.version(), platform.platformName());
        logger.info("Loading configuration...");
        configs.loadAll();
        loadLanguage();
        webhooks.load();

        logger.info("Proxy id: &f" + configs.settings().proxyId());
        logger.info("Language: &f" + configs.settings().language());

        connectStorage();
        connectRedis();

        presence = new PresenceService(redis, logger, configs.settings().proxyId(), configs.redis().keyPrefix());
        router = new ServerRouter(platform, presence, configs.lobby());
        maintenance = new MaintenanceService(
            platform,
            redis,
            configs.maintenance(),
            language,
            logger,
            configs.settings().proxyId()
        );
        motd = new MotdService(configs.motd(), maintenance, language, configs.settings());
        blacklist = new BlacklistService(
            platform,
            redis,
            storage,
            configs.blacklist(),
            language,
            logger,
            configs.settings().proxyId()
        );
        vanishPresence = new VanishPresenceService(redis);
        luckPerms = new LuckPermsHook();
        commandBlocker = new CommandBlockerService(configs.commandWhitelist(), commandRegistry);
        staffGroups = new StaffGroups(configs.permissions(), configs.commands().permission("staff"), luckPerms);
        staffAlerts = new StaffAlertService(platform, language, configs.commands());

        if (configs.settings().moduleEnabled("staffchat")) {
            staffChat = new StaffChatService(
                platform,
                redis,
                language,
                configs.settings().proxyId(),
                configs.commands(),
                staffGroups
            );
        }

        if (configs.settings().moduleEnabled("adminchat")) {
            adminChat = new AdminChatService(
                platform,
                redis,
                language,
                configs.settings().proxyId(),
                configs.commands(),
                staffGroups
            );
        }

        if (configs.settings().moduleEnabled("stream")) {
            streamService = new StreamService(
                platform,
                redis,
                language,
                logger,
                webhooks,
                configs.settings().proxyId(),
                configs.stream()
            );
        }

        if (configs.settings().moduleEnabled("announcements")) {
            announcements = new AnnouncementService(
                platform,
                redis,
                language,
                logger,
                configs.settings().proxyId(),
                configs.announcements()
            );
            announcements.start();
        }

        tabCompleteFilter = new TabCompleteFilterService(
            configs.commandWhitelist(),
            configs.commands(),
            commandRegistry,
            commandBlocker
        );

        if (configs.settings().moduleEnabled("report")) {
            reportService = new ReportService(
                platform,
                redis,
                language,
                webhooks,
                storage,
                logger,
                configs.commands(),
                configs.settings().proxyId(),
                configs.report()
            );
        }

        if (configs.settings().moduleEnabled("helpop")) {
            helpOpService = new HelpOpService(
                platform,
                redis,
                language,
                webhooks,
                presence,
                storage,
                logger,
                configs.commands(),
                configs.settings().proxyId(),
                configs.helpOp()
            );
        }

        if (configs.settings().moduleEnabled("donations") || configs.settings().moduleEnabled("ggwave")) {
            donationService = new DonationService(
                platform,
                redis,
                language,
                webhooks,
                logger,
                configs.settings().proxyId(),
                configs.donations()
            );
        }

        if (configs.settings().moduleEnabled("discord")) {
            statusWebhook = new StatusWebhookService(
                platform,
                webhooks,
                maintenance,
                logger,
                configs.settings().proxyId()
            );
            statusWebhook.reload();
        }

        if (configs.settings().moduleEnabled("punishments")) {
            banCheck = new BanCheckService(storage, language, logger, configs.settings().banCheckCacheSeconds());
            muteCheck = new MuteCheckService(storage, logger, configs.settings().banCheckCacheSeconds());
            us.iorilabs.proxy.redis.RedisEventListener invalidatePunishment = event -> {
                if (event.data() == null) return;
                String raw = null;
                if (event.data().has("uuid")) raw = event.data().get("uuid").getAsString();
                else if (event.data().has("target_uuid")) raw = event.data().get("target_uuid").getAsString();
                if (raw == null || raw.isBlank()) return;
                try {
                    java.util.UUID id = java.util.UUID.fromString(raw);
                    if (banCheck != null) banCheck.invalidate(id);
                    if (muteCheck != null) muteCheck.invalidate(id);
                } catch (IllegalArgumentException ignored) {}
            };
            redis.subscribe(ProxyEventType.PUNISHMENT_INVALIDATE.id(), invalidatePunishment);
            redis.subscribe(ProxyEventType.PUNISHMENT_APPLIED.id(), invalidatePunishment);
        }

        if (configs.settings().moduleEnabled("discord") || configs.settings().moduleEnabled("sync")) {
            discordLinks = new us.iorilabs.proxy.discord.link.DiscordLinkService(
                platform,
                redis,
                language,
                logger,
                storage,
                configs.settings().proxyId(),
                300,
                30
            );
        }

        if (configs.settings().discordBotEnabled() && configs.discord().enabled()) {
            discordBot = new DiscordBotService(
                platform,
                logger,
                language,
                webhooks,
                maintenance,
                storage,
                redis,
                banCheck,
                configs.settings().proxyId(),
                configs.discord()
            );
            webhooks.botMirror(discordBot::mirrorWebhook);
            if (staffChat != null) {
                discordBot.bindStaffChat(staffChat);
                staffChat.discordSink(discordBot::onStaffChatFromMinecraft);
            }
        }

        if (configs.settings().moduleEnabled("playtime")) {
            playtime = new PlaytimeService(platform, storage, configs.maintenance(), configs.playtime(), logger);
        }

        playerData = new PlayerDataService(platform, storage, logger);

        if (configs.settings().moduleEnabled("analytics")) {
            analytics = new AnalyticsService(
                platform,
                storage,
                configs.analytics(),
                logger,
                configs.settings().proxyId()
            );
        }

        if (configs.settings().moduleEnabled("placeholders")) {
            placeholders = new PlaceholderService(this);
        }

        if (announcements != null && placeholders != null) {
            announcements.setPlaceholders(placeholders);
        }

        if (configs.settings().moduleEnabled("friends")) {
            friendService = new FriendService(
                platform,
                redis,
                language,
                presence,
                storage,
                logger,
                configs.settings().proxyId(),
                configs.friends()
            );
        }

        if (configs.settings().moduleEnabled("alts")) {
            alts = new AltService(platform, storage, logger, language, blacklist, staffAlerts);
        }

        if (configs.settings().moduleEnabled("transfer")) {
            accountTransfer = new AccountTransferService(
                platform,
                storage,
                logger,
                friendService,
                playtime,
                playerData,
                maintenance,
                alts
            );
        }

        if (configs.settings().moduleEnabled("staff-auth")) {
            staffReg = new StaffRegService(
                platform,
                logger,
                language,
                staffGroups,
                configs.staffReg(),
                storage,
                redis,
                configs.settings().proxyId()
            );
            staffReg.bindBanCheck(banCheck);
            String bridgeSecret = bridgeSecret(configs.discord());
            if (discordBot != null) {
                staffReg.bindDiscord(discordBot, discordLinks, bridgeSecret);
                staffRegBridge = new DiscordBridgeConsumer(
                    platform,
                    redis,
                    discordBot,
                    configs.discord(),
                    logger,
                    bridgeSecret
                );
                staffRegBridge.start();
            } else {
                staffReg.bindDiscord(null, discordLinks, bridgeSecret);
            }
        }

        if (discordBot != null) {
            discordBot.start();
        }

        registerCommands();
        modules.registerDefaults(this);
        modules.enableAll(this);
        enabled = true;

        logger.module("Status", "Redis: &f" + redis.statusLine());
        logger.module("Status", "Storage: &f" + storage.statusLine());
        logger.success("IoriProxy enabled on &f" + platform.platformName());
    }

    public void disable() {
        if (!enabled) return;

        if (presence != null) {
            presence.clearLocalOwnedByThisProxy();
        }

        if (announcements != null) {
            announcements.stop();
        }

        if (statusWebhook != null) {
            statusWebhook.stop();
        }

        if (staffRegBridge != null) {
            staffRegBridge.stop();
            staffRegBridge = null;
        }

        if (discordBot != null) {
            discordBot.stop();
            discordBot = null;
        }

        modules.disableAll();

        if (banCheck != null) {
            banCheck.shutdown();
            banCheck = null;
        }

        if (muteCheck != null) {
            muteCheck.shutdown();
            muteCheck = null;
        }

        try {
            redis.close();
        } catch (Exception ex) {
            logger.error("Error closing Redis", ex);
        }

        try {
            storage.close();
        } catch (Exception ex) {
            logger.error("Error closing storage", ex);
        }

        enabled = false;
        logger.warn("IoriProxy disabled.");
        instance = null;
    }

    public void reload() {
        configs.reloadAll();
        loadLanguage();
        webhooks.load();
        luckPerms = new LuckPermsHook();

        if (presence != null) {
            router = new ServerRouter(platform, presence, configs.lobby());
        }

        if (maintenance != null) {
            maintenance.reloadConfig(configs.maintenance());
        }

        if (motd != null) {
            motd.reloadConfig(configs.motd());
        }

        if (blacklist != null) {
            blacklist.reloadConfig(configs.blacklist());
        }

        if (banCheck != null) {
            banCheck.cacheTtl(configs.settings().banCheckCacheSeconds());
        }

        if (muteCheck != null) {
            muteCheck.cacheTtl(configs.settings().banCheckCacheSeconds());
        }

        if (commandBlocker != null) {
            commandBlocker.reload(configs.commandWhitelist());
        }

        staffGroups = new StaffGroups(configs.permissions(), configs.commands().permission("staff"), luckPerms);

        if (streamService != null) {
            streamService.reloadConfig(configs.stream());
        }

        if (announcements != null) {
            announcements.reload(configs.announcements());
            announcements.publishReloadSignal();
        }

        if (tabCompleteFilter != null) {
            tabCompleteFilter.reload(configs.commandWhitelist());
        }

        if (reportService != null) {
            reportService.reloadConfig(configs.report());
        }

        if (helpOpService != null) {
            helpOpService.reloadConfig(configs.helpOp());
        }

        if (donationService != null) {
            donationService.reload(configs.donations());
        }

        if (statusWebhook != null && configs.settings().moduleEnabled("discord")) {
            statusWebhook.reload();
        }

        if (discordBot != null && configs.settings().discordBotEnabled() && configs.discord().enabled()) {
            discordBot.reload(configs.discord());
            webhooks.botMirror(discordBot::mirrorWebhook);
            if (staffChat != null) {
                discordBot.bindStaffChat(staffChat);
                staffChat.discordSink(discordBot::onStaffChatFromMinecraft);
            }
        } else {
            if (discordBot != null) {
                discordBot.stop();
            }
            webhooks.botMirror(null);
            if (staffChat != null) {
                staffChat.discordSink(null);
            }
        }

        if (friendService != null) {
            friendService.reloadConfig(configs.friends());
        }

        if (staffReg != null) {
            staffReg.reload(configs.staffReg());
            String bridgeSecret = bridgeSecret(configs.discord());
            staffReg.bindDiscord(discordBot, discordLinks, bridgeSecret);
        }

        modules.reloadAll(this);
        logger.module("Status", "Redis: &f" + redis.statusLine());
        logger.module("Status", "Storage: &f" + storage.statusLine());
        logger.success("Configuration reloaded.");
    }

    private void registerConfigured(ProxyCommand command) {
        commandRegistry.registerConfigured(command, configs.commands().aliases(command.name()));
    }

    private void loadLanguage() {
        language.load(configs.settings().language(), configs.settings().legacyProtocolMax());
    }

    private void connectStorage() {
        storage = StorageFactory.create(configs.database(), logger, platform.dataDirectory());
    }

    private void connectRedis() {
        if (!configs.redis().enabled()) {
            redis = new NoopRedisService(configs.settings().proxyId(), configs.redis().keyPrefix());
            logger.warn("Redis disabled in redis.yml");
            return;
        }

        try {
            JedisRedisService jedis = new JedisRedisService(configs.settings().proxyId(), configs.redis(), logger);
            jedis.connect();
            jedis.subscribe(ProxyEventType.REDIS_TEST.id(), event -> {
                JsonObject data = event.data();
                String sender = data.has("sender") ? data.get("sender").getAsString() : "?";
                logger.module("Redis", "REDIS_TEST from &f" + event.originProxyId() + " &7by &f" + sender);
            });
            redis = jedis;
        } catch (Exception ex) {
            logger.error("Redis connection failed — continuing without Redis", ex);
            redis = new NoopRedisService(configs.settings().proxyId(), configs.redis().keyPrefix());
        }
    }

    private static String bridgeSecret(us.iorilabs.proxy.config.DiscordConfig discord) {
        if (discord == null || discord.bridge() == null) return "";
        String env = discord.bridge().secretEnv();
        if (env == null || env.isBlank()) return "";
        String value = System.getenv(env);
        return value == null ? "" : value;
    }

    private void registerCommands() {
        registerConfigured(new RootCommand(this));
        if (configs.settings().debug()) {
            registerConfigured(new RedisTestCommand(this));
        }

        if (configs.settings().moduleEnabled("send")) {
            registerConfigured(new SendCommand(this));
            registerConfigured(new GotoCommand(this));
            registerConfigured(new ServersCommand(this));
        }

        if (configs.settings().moduleEnabled("whereis")) {
            registerConfigured(new FindCommand(this));
        }

        if (configs.settings().moduleEnabled("lobby")) {
            registerConfigured(new HubCommand(this));
        }

        if (configs.settings().moduleEnabled("maintenance")) {
            registerConfigured(new MaintenanceCommand(this));
        }

        if (configs.settings().moduleEnabled("blacklist")) {
            registerConfigured(new BlacklistCommand(this, false));
            registerConfigured(new BlacklistCommand(this, true));
        }

        if (configs.settings().moduleEnabled("staffchat") && staffChat != null) {
            registerConfigured(new StaffChatCommand(this, staffChat));
        }

        if (configs.settings().moduleEnabled("adminchat") && adminChat != null) {
            registerConfigured(new AdminChatCommand(this, adminChat));
        }

        if (configs.settings().moduleEnabled("stream") && streamService != null) {
            registerConfigured(new StreamCommand(this, streamService, "stream"));
            registerConfigured(new StreamCommand(this, streamService, "youtube"));
        }

        if (configs.settings().moduleEnabled("report") && reportService != null) {
            registerConfigured(new ReportCommand(this, reportService));
            registerConfigured(new ReportsCommand(this, reportService));
        }

        if (configs.settings().moduleEnabled("helpop") && helpOpService != null) {
            registerConfigured(new RequestCommand(this, helpOpService));
        }

        if (donationService != null) {
            if (configs.settings().moduleEnabled("donations")) {
                registerConfigured(new DonationCommand(this, donationService));
            }

            if (configs.settings().moduleEnabled("ggwave")) {
                registerConfigured(new GgWaveCommand(this, donationService));
            }
        }

        if (configs.settings().moduleEnabled("playtime") && playtime != null) {
            registerConfigured(new PlaytimeCommand(this));
        }

        if (configs.settings().moduleEnabled("analytics") && analytics != null) {
            registerConfigured(new AnalyticsCommand(this));
        }

        if (configs.settings().moduleEnabled("ping")) {
            registerConfigured(new PingCommand(this));
        }

        registerConfigured(new GlistCommand(this));
        registerConfigured(new AnnounceCommand(this));
        registerConfigured(new AlertCommand(this));
        registerConfigured(new SeenCommand(this));

        if (configs.settings().moduleEnabled("alts") && alts != null) {
            registerConfigured(new AltsCommand(this));
        }

        if (configs.settings().moduleEnabled("transfer") && accountTransfer != null) {
            registerConfigured(new TransferCommand(this));
        }

        if (configs.settings().moduleEnabled("friends") && friendService != null) {
            registerConfigured(new FriendsCommand(this, friendService));
        }

        if (configs.settings().moduleEnabled("staff-auth") && staffReg != null) {
            registerConfigured(new StaffRegCommand(this));
        }

        if (discordLinks != null) {
            registerConfigured(new SyncCommand(this, discordLinks));
            registerConfigured(new UnsyncCommand(this, discordLinks));
        }
        registerConfigured(new ProfileCommand(this));

        platform.registerCommands(commandRegistry);
        logger.module(
            "Commands",
            "registered " + commandRegistry.uniqueCommands().size() + " command(s), case-insensitive labels"
        );
    }
}
