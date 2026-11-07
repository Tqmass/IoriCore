package us.iorilabs.bukkit.discord;

import java.io.File;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.discord.command.SyncCommand;
import us.iorilabs.bukkit.discord.command.UnsyncCommand;
import us.iorilabs.bukkit.discord.link.DiscordLinkService;
import us.iorilabs.bukkit.discord.link.DiscordLinkSql;
import us.iorilabs.bukkit.modules.CoreModule;

@Getter
@Accessors(fluent = true)
public final class DiscordModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private DiscordConfig config;
    private DiscordLinkService links;
    private StaffRegRedisPublisher redisPublisher;

    public DiscordModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        FileConfiguration redis = loadRedisYaml(plugin);
        this.enabledFlag = redis.getBoolean("DISCORD-BRIDGE.ENABLED", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.DISCORD;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        FileConfiguration redis = loadRedisYaml(plugin);
        config = DiscordConfig.fromRedisBridge(redis);
        if (!config.bridgeEnabled()) return;

        DiscordLinkSql sql = new DiscordLinkSql(plugin.storage(), plugin.coreLogger());
        links = new DiscordLinkService(
            plugin,
            sql,
            config.codeTtlSeconds(),
            config.cooldownSeconds(),
            config.redeemCooldownSeconds()
        );
        redisPublisher = new StaffRegRedisPublisher(plugin, redis);

        BukkitCommandBridge.register(plugin, new SyncCommand(plugin, links));
        BukkitCommandBridge.register(plugin, new UnsyncCommand(plugin, links));
    }

    @Override
    public void disable() {
        redisPublisher = null;
        links = null;
        config = null;
    }

    @Override
    public void reload() {
        if (!enabledFlag) return;

        FileConfiguration redis = loadRedisYaml(plugin);
        config = DiscordConfig.fromRedisBridge(redis);
        if (links != null) {
            links.reload(config.codeTtlSeconds(), config.cooldownSeconds(), config.redeemCooldownSeconds());
        }
    }

    public boolean bridgeReady() {
        return config != null && config.bridgeEnabled() && plugin.redis() != null && plugin.redis().enabled();
    }

    private static FileConfiguration loadRedisYaml(IoriCorePlugin plugin) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "redis.yml"));
    }
}
