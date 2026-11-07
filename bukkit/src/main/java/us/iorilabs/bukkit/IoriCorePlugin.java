package us.iorilabs.bukkit;

import java.io.File;
import java.io.IOException;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import us.iorilabs.abstraction.VersionWrapper;
import us.iorilabs.api.hook.ApolloHook;
import us.iorilabs.api.hook.GeyserHook;
import us.iorilabs.api.hook.LuckPermsHook;
import us.iorilabs.api.hook.NLoginHook;
import us.iorilabs.api.hook.NoopApolloHook;
import us.iorilabs.api.hook.NoopGeyserHook;
import us.iorilabs.api.hook.NoopLuckPermsHook;
import us.iorilabs.api.hook.NoopNLoginHook;
import us.iorilabs.api.hook.NoopVaultHook;
import us.iorilabs.api.hook.VaultHook;
import us.iorilabs.api.storage.SqlStorage;
import us.iorilabs.api.storage.StorageBackend;
import us.iorilabs.bukkit.abstraction.VersionMatcher;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.commands.core.IoriCoreCommand;
import us.iorilabs.bukkit.discord.DiscordModule;
import us.iorilabs.bukkit.donations.GgWaveRedisBridge;
import us.iorilabs.bukkit.fx.ActionBarService;
import us.iorilabs.bukkit.fx.SoundService;
import us.iorilabs.bukkit.fx.TitleService;
import us.iorilabs.bukkit.hook.SoftApolloHook;
import us.iorilabs.bukkit.hook.SoftGeyserHook;
import us.iorilabs.bukkit.hook.SoftLuckPermsHook;
import us.iorilabs.bukkit.hook.SoftNLoginHook;
import us.iorilabs.bukkit.hook.SoftVaultHook;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.logging.BukkitLogger;
import us.iorilabs.bukkit.menu.MenuListener;
import us.iorilabs.bukkit.menu.api.MenuApi;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;
import us.iorilabs.bukkit.menu.api.yaml.internal.MenuYamlBootstrap;
import us.iorilabs.bukkit.messaging.PluginMessagingModule;
import us.iorilabs.bukkit.modules.ModuleRegistry;
import us.iorilabs.bukkit.modules.accountbrand.AccountBrandModule;
import us.iorilabs.bukkit.modules.actionbar.ActionBarRotatorModule;
import us.iorilabs.bukkit.modules.chatformat.ChatFormatModule;
import us.iorilabs.bukkit.modules.chatgames.ChatGamesModule;
import us.iorilabs.bukkit.modules.chatlogs.ChatLogsModule;
import us.iorilabs.bukkit.modules.coinshop.CoinshopModule;
import us.iorilabs.bukkit.modules.core.CoreFeaturesModule;
import us.iorilabs.bukkit.modules.customjoins.CustomJoinsModule;
import us.iorilabs.bukkit.modules.economy.EconomyModule;
import us.iorilabs.bukkit.modules.gender.GenderModule;
import us.iorilabs.bukkit.modules.lobby.LobbyModule;
import us.iorilabs.bukkit.modules.nametag.NametagModule;
import us.iorilabs.bukkit.modules.scoreboard.ScoreboardModule;
import us.iorilabs.bukkit.modules.settings.PlayerSettingsModule;
import us.iorilabs.bukkit.modules.staffauth.StaffAuthEnforceModule;
import us.iorilabs.bukkit.modules.staffmode.StaffModeModule;
import us.iorilabs.bukkit.modules.staffmode.StaffModeRedisBridge;
import us.iorilabs.bukkit.modules.tab.TabModule;
import us.iorilabs.bukkit.modules.tags.TagsModule;
import us.iorilabs.bukkit.protocol.ClientProtocolService;
import us.iorilabs.bukkit.punishment.PunishmentsModule;
import us.iorilabs.bukkit.ranks.RanksModule;
import us.iorilabs.bukkit.redis.CoreRedisService;
import us.iorilabs.bukkit.redis.PunishmentRedisBridge;
import us.iorilabs.bukkit.redis.RedisSettings;
import us.iorilabs.bukkit.storage.CoreSqlStorage;
import us.iorilabs.bukkit.storage.DatabaseConfig;
import us.iorilabs.bukkit.storage.SoftMongoStorage;
import us.iorilabs.bukkit.util.PluginResources;
import us.iorilabs.proxy.utilities.ProtocolVersions;

@Getter
@Accessors(fluent = true)
public final class IoriCorePlugin extends JavaPlugin {

    public static final String DEFAULT_SERVER_NAME = "lobby-1";

    private BukkitLogger coreLogger;
    private FileConfiguration settings;
    private FileConfiguration commands;
    private FileConfiguration permissions;
    private SqlStorage storage;
    private SoftMongoStorage softMongo;
    private CoreRedisService redis;
    private PunishmentRedisBridge punishmentSync;
    private StaffModeRedisBridge staffModeSync;
    private ClientProtocolService clientProtocols;
    private ModuleRegistry modules;
    private CoreLanguage language;
    private GeyserHook geyserHook = new NoopGeyserHook();
    private VaultHook vaultHook = new NoopVaultHook();
    private LuckPermsHook luckPermsHook = new NoopLuckPermsHook();
    private ApolloHook apolloHook = new NoopApolloHook();
    private NLoginHook nLoginHook = new NoopNLoginHook();
    private VersionWrapper abstraction;
    private SoundService sounds;
    private TitleService titles;
    private ActionBarService actionBars;
    private YamlMenuService yamlMenus;

    @Override
    public void onEnable() {
        coreLogger = new BukkitLogger(this);
        coreLogger.banner(getDescription().getVersion(), "Bukkit");

        saveDefaults(
            "settings.yml",
            "ranks.yml",
            "data/tags.yml",
            "data/chatgames.yml",
            "punishments.yml",
            "staff-mode.yml",
            "punishment.yml",
            "chat-formats.yml",
            "commands.yml",
            "database.yml",
            "redis.yml",
            "permissions.yml",
            "sounds.yml",
            "discord.yml",
            "groups.yml",
            "hotbar.yml",
            "tab.yml"
        );
        us.iorilabs.bukkit.storage.StoragePaths.migrateLegacyRoots(this);

        settings = loadYaml("settings.yml");
        commands = loadYaml("commands.yml");
        permissions = loadYaml("permissions.yml");

        language = new CoreLanguage(this);
        language.load(settings.getString("SETTINGS.LANGUAGE", "ES"), ProtocolVersions.LEGACY_MAX_PROTOCOL);

        DatabaseConfig database = DatabaseConfig.load(getDataFolder(), settings, getLogger());
        storage = CoreSqlStorage.open(getDataFolder(), database, getLogger());
        softMongo = SoftMongoStorage.tryOpen(settings, database.yaml(), getLogger());
        FileConfiguration redisYaml = loadYaml("redis.yml");
        redis = new CoreRedisService(serverName(), RedisSettings.fromRedisFile(redisYaml, settings), getLogger());
        punishmentSync = new PunishmentRedisBridge(this, redis);
        punishmentSync.register();
        staffModeSync = new StaffModeRedisBridge(this, redis);
        staffModeSync.register();
        clientProtocols = new ClientProtocolService(this);
        clientProtocols.start(redis);
        language.protocolResolver(clientProtocols::protocol);
        us.iorilabs.bukkit.util.ProtocolProbe.resolver(clientProtocols::protocol);
        redis.start();
        GgWaveRedisBridge.register(this);
        abstraction = VersionMatcher.load(getLogger());

        sounds = new SoundService(this);
        sounds.load();
        titles = new TitleService(this, sounds);
        actionBars = new ActionBarService(this, sounds);

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        MenuApi.enable(this);
        yamlMenus = new YamlMenuService(this);
        yamlMenus.reload();

        vaultHook = new SoftVaultHook(getLogger(), settings.getBoolean("MODULES.VAULT-HOOK", true));
        luckPermsHook = new SoftLuckPermsHook(getLogger(), settings.getBoolean("MODULES.LUCKPERMS-HOOK", true));
        apolloHook = new SoftApolloHook(getLogger(), settings.getBoolean("MODULES.APOLLO-HOOK", true));
        nLoginHook = new SoftNLoginHook(getLogger(), settings.getBoolean("MODULES.NLOGIN-HOOK", true));
        geyserHook = new SoftGeyserHook(getLogger(), settings.getBoolean("MODULES.GEYSER-HOOK", true));

        String mongo = softMongo != null && softMongo.available() ? "on" : "off";
        coreLogger.info(
            "Boot · lang=&f" +
                language.language() +
                " &7· storage=&f" +
                storage.backend().name() +
                " &7· mongo=&f" +
                mongo +
                " &7· redis=&f" +
                redis.statusLine() +
                " &7· nms=&f" +
                abstraction.revision() +
                " &7· origin=&f" +
                serverName()
        );
        coreLogger.info(
            "Hooks · Vault=&f" +
                vaultHook.available() +
                " &7LP=&f" +
                luckPermsHook.available() +
                " &7Apollo=&f" +
                apolloHook.available() +
                " &7nLogin=&f" +
                nLoginHook.available() +
                " &7Geyser=&f" +
                geyserHook.available() +
                " &7· sounds=&f" +
                sounds.entries().size() +
                " &7· menus=&f" +
                yamlMenus.size()
        );

        modules = new ModuleRegistry(coreLogger);
        modules.register(new EconomyModule(this));
        modules.register(new ChatGamesModule(this));
        modules.register(new GenderModule(this, settings, coreLogger));
        modules.register(new TagsModule(this));
        modules.register(new CoinshopModule(this));
        modules.register(new StaffModeModule(this));
        modules.register(new ChatLogsModule(this, settings));
        modules.register(new ChatFormatModule(this));
        modules.register(new CoreFeaturesModule(this));
        modules.register(new RanksModule(this));
        modules.register(new LobbyModule(this));
        modules.register(new NametagModule(this));
        modules.register(new PunishmentsModule(this));
        modules.register(new StaffAuthEnforceModule(this, settings));
        modules.register(new PluginMessagingModule(this, settings));
        modules.register(new PlayerSettingsModule(this));
        modules.register(new ScoreboardModule(this));
        modules.register(new TabModule(this));
        modules.register(new CustomJoinsModule(this));
        modules.register(new AccountBrandModule(this));
        modules.register(new ActionBarRotatorModule(this));
        modules.register(new DiscordModule(this));
        modules.enableAll();
        StaffAuthEnforceModule.registerCommands(this, settings);
        MenuYamlBootstrap.register(this, yamlMenus);
        BukkitCommandBridge.register(this, new IoriCoreCommand(this));
        coreLogger.success("IoriCore enabled");
    }

    @Override
    public void onDisable() {
        MenuApi.disable();
        if (modules != null) modules.disableAll();
        if (clientProtocols != null) {
            us.iorilabs.bukkit.util.ProtocolProbe.resolver(null);
            clientProtocols.stop();
            clientProtocols = null;
        }
        if (redis != null) {
            redis.close();
            redis = null;
        }
        if (softMongo != null) {
            softMongo.close();
            softMongo = null;
        }
        if (storage != null) {
            storage.close();
            storage = null;
        }
        if (coreLogger != null) {
            coreLogger.info("IoriCore disabled");
        } else {
            getLogger().info("IoriCore disabled");
        }
    }

    public int reloadCore() {
        reloadYaml(settings, "settings.yml");
        reloadYaml(commands, "commands.yml");
        reloadYaml(permissions, "permissions.yml");
        language.load(settings.getString("SETTINGS.LANGUAGE", "ES"), ProtocolVersions.LEGACY_MAX_PROTOCOL);
        sounds.load();
        if (yamlMenus != null) yamlMenus.reload();
        int reloaded = modules.reloadAll(settings);
        BukkitCommandBridge.register(this, new IoriCoreCommand(this));
        StaffAuthEnforceModule.registerCommands(this, settings);
        BukkitCommandBridge.syncCommands();
        coreLogger.info("Reloaded · " + reloaded + " modules");
        return reloaded;
    }

    private void reloadYaml(FileConfiguration config, String name) {
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }
        try {
            config.load(new File(getDataFolder(), name));
        } catch (IOException | InvalidConfigurationException ex) {
            coreLogger.warn("Reload " + name + ": " + ex.getMessage());
        }
    }

    public String serverName() {
        String name = settings == null ? null : settings.getString("SETTINGS.SERVER-NAME");
        return name == null || name.trim().isEmpty() ? DEFAULT_SERVER_NAME : name.trim();
    }

    public StorageBackend storageBackend() {
        return storage == null ? StorageBackend.NONE : storage.backend();
    }

    private void saveDefaults(String... paths) {
        if (paths == null) return;
        for (String path : paths) {
            PluginResources.saveDefault(this, path);
        }
    }

    private FileConfiguration loadYaml(String name) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), name));
    }

    public int fallbackProtocol() {
        if (settings == null) return ProtocolVersions.maxProtocol("1.8.9");
        String version = settings.getString("SETTINGS.FALLBACK-VERSION", "");
        if (version != null && !version.trim().isEmpty()) {
            return ProtocolVersions.maxProtocol(version);
        }
        if (settings.contains("SETTINGS.FALLBACK-PROTOCOL")) {
            return ProtocolVersions.maxProtocol(String.valueOf(settings.getInt("SETTINGS.FALLBACK-PROTOCOL", 47)));
        }
        return ProtocolVersions.maxProtocol("1.8.9");
    }
}
