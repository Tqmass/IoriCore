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

    private static final String FILE = "discord.yml";

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private DiscordBotService bot;
    private DiscordConfig config;
    private DiscordLinkService links;
    private StaffRegRedisPublisher staffRegPublisher;

    public DiscordModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.DISCORD", false);
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
        us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, FILE);
        FileConfiguration yaml = loadYaml();
        config = new DiscordConfig(yaml);
        DiscordLinkSql sql = new DiscordLinkSql(plugin.storage(), plugin.coreLogger());
        links = new DiscordLinkService(
            plugin,
            sql,
            config.codeTtlSeconds(),
            config.cooldownSeconds(),
            config.redeemCooldownSeconds()
        );
        bot = new DiscordBotService(plugin, config);
        bot.bind(links);
        staffRegPublisher = new StaffRegRedisPublisher(plugin, yaml);
        bot.start();

        BukkitCommandBridge.register(plugin, new SyncCommand(plugin, links));
        BukkitCommandBridge.register(plugin, new UnsyncCommand(plugin, links));
    }

    @Override
    public void disable() {
        if (bot != null) {
            bot.stop();
            bot = null;
        }
        staffRegPublisher = null;
        links = null;
        config = null;
    }

    @Override
    public void reload() {
        if (!enabledFlag) return;

        us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, FILE);
        FileConfiguration yaml = loadYaml();
        config = new DiscordConfig(yaml);
        staffRegPublisher = new StaffRegRedisPublisher(plugin, yaml);
        if (links != null) {
            links.reload(config.codeTtlSeconds(), config.cooldownSeconds(), config.redeemCooldownSeconds());
        }
        if (bot == null) {
            bot = new DiscordBotService(plugin, config);
            if (links != null) bot.bind(links);
            bot.start();
            return;
        }
        bot.reload(config);
    }

    private FileConfiguration loadYaml() {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), FILE));
    }
}
