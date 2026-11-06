package us.iorilabs.proxy.velocity;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.slf4j.Logger;
import us.iorilabs.proxy.commands.CommandRegistry;
import us.iorilabs.proxy.commands.ProxyCommand;
import us.iorilabs.proxy.platform.BackendServer;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.ColorUtil;
import us.iorilabs.proxy.velocity.command.VelocityBrigadierCommand;
import us.iorilabs.proxy.velocity.player.VelocityProxyPlayer;

@Accessors(fluent = true)
public final class VelocityPlatform implements ProxyPlatform {

    @Getter
    private final IoriProxyVelocity plugin;

    @Getter
    private final ProxyServer server;

    private final Logger logger;

    @Getter
    private final Path dataDirectory;

    @Getter
    private final java.util.logging.Logger julLogger;

    private final Map<String, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public VelocityPlatform(IoriProxyVelocity plugin, ProxyServer server, Logger logger, Path dataDirectory) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.julLogger = java.util.logging.Logger.getLogger("IoriProxy");
    }

    @Override
    public String platformName() {
        return "Velocity";
    }

    @Override
    public String version() {
        return "1.0.0-SNAPSHOT";
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public Executor asyncExecutor() {
        return runnable -> server.getScheduler().buildTask(plugin, runnable).schedule();
    }

    @Override
    public void runSync(Runnable task) {
        server.getScheduler().buildTask(plugin, task).delay(0, TimeUnit.MILLISECONDS).schedule();
    }

    @Override
    public void runLater(Runnable task, long delayMs) {
        server.getScheduler().buildTask(plugin, task).delay(delayMs, TimeUnit.MILLISECONDS).schedule();
    }

    @Override
    public void runTimer(Runnable task, long delayMs, long periodMs, String taskId) {
        cancelTask(taskId);
        ScheduledTask scheduled = server
            .getScheduler()
            .buildTask(plugin, task)
            .delay(delayMs, TimeUnit.MILLISECONDS)
            .repeat(periodMs, TimeUnit.MILLISECONDS)
            .schedule();
        tasks.put(taskId, scheduled);
    }

    @Override
    public void cancelTask(String taskId) {
        ScheduledTask existing = tasks.remove(taskId);
        if (existing != null) {
            existing.cancel();
        }
    }

    @Override
    public InputStream getResource(String path) {
        return IoriProxyVelocity.class.getClassLoader().getResourceAsStream(path);
    }

    @Override
    public void registerCommands(CommandRegistry registry) {
        for (ProxyCommand command : registry.uniqueCommands()) {
            Set<String> labels = new LinkedHashSet<>();
            labels.add(command.name().toLowerCase(Locale.ROOT));

            for (String alias : command.aliases()) {
                labels.add(alias.toLowerCase(Locale.ROOT));
            }

            for (String configuredAlias : plugin.core().configs().commands().aliases(command.name())) {
                labels.add(configuredAlias.toLowerCase(Locale.ROOT));
            }

            String primary = command.name().toLowerCase(Locale.ROOT);
            String[] aliasArray = labels
                .stream()
                .filter(label -> !label.equals(primary))
                .toArray(String[]::new);
            CommandMeta meta = server
                .getCommandManager()
                .metaBuilder(primary)
                .aliases(aliasArray)
                .plugin(plugin)
                .build();
            server.getCommandManager().register(meta, VelocityBrigadierCommand.create(plugin.core(), command, primary));
        }
    }

    @Override
    public Optional<ProxyPlayer> findPlayerByName(String name) {
        return server.getPlayer(name).map(player -> new VelocityProxyPlayer(server, player));
    }

    @Override
    public Optional<ProxyPlayer> findPlayerByUuid(UUID uuid) {
        return server.getPlayer(uuid).map(player -> new VelocityProxyPlayer(server, player));
    }

    @Override
    public Collection<ProxyPlayer> onlinePlayers() {
        Collection<ProxyPlayer> players = new ArrayList<>();

        for (Player player : server.getAllPlayers()) {
            players.add(new VelocityProxyPlayer(server, player));
        }

        return players;
    }

    @Override
    public Collection<BackendServer> registeredServers() {
        Collection<BackendServer> backends = new ArrayList<>();

        for (RegisteredServer registered : server.getAllServers()) {
            backends.add(toBackend(registered));
        }

        return backends;
    }

    @Override
    public Optional<BackendServer> server(String name) {
        return server.getServer(name).map(this::toBackend);
    }

    @Override
    public boolean connectPlayer(UUID uuid, String serverName) {
        Optional<Player> player = server.getPlayer(uuid);
        Optional<RegisteredServer> target = server.getServer(serverName);
        if (player.isEmpty() || target.isEmpty()) return false;

        player.get().createConnectionRequest(target.get()).fireAndForget();
        return true;
    }

    @Override
    public void kickPlayer(UUID uuid, String miniOrLegacy) {
        server.getPlayer(uuid).ifPresent(player -> player.disconnect(ColorUtil.parse(miniOrLegacy)));
    }

    @Override
    public void broadcast(String miniOrLegacy) {
        var component = ColorUtil.parse(miniOrLegacy);

        for (Player player : server.getAllPlayers()) {
            player.sendMessage(component);
        }
    }

    @Override
    public void broadcastToServer(String serverName, String miniOrLegacy) {
        var component = ColorUtil.parse(miniOrLegacy);
        server.getServer(serverName).ifPresent(registered -> {
            for (Player player : registered.getPlayersConnected()) {
                player.sendMessage(component);
            }
        });
    }

    @Override
    public void sendActionBar(UUID uuid, String miniOrLegacy) {
        server.getPlayer(uuid).ifPresent(player -> player.sendActionBar(ColorUtil.parse(miniOrLegacy)));
    }

    @Override
    public void sendTitle(UUID uuid, String titleMini, String subtitleMini) {
        server
            .getPlayer(uuid)
            .ifPresent(player ->
                player.showTitle(
                    Title.title(
                        ColorUtil.parse(titleMini),
                        ColorUtil.parse(subtitleMini),
                        Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(250))
                    )
                )
            );
    }

    @Override
    public boolean playSound(UUID uuid, String soundKey, float volume, float pitch) {
        Optional<Player> player = server.getPlayer(uuid);
        if (player.isEmpty()) return false;

        try {
            player.get().playSound(Sound.sound(Key.key(soundKey), Sound.Source.MASTER, volume, pitch));
            return true;
        } catch (InvalidKeyException ex) {
            return false;
        }
    }

    @Override
    public boolean sendBackendPluginMessage(UUID uuid, byte[] data) {
        if (uuid == null || data == null || data.length == 0) return false;

        Optional<Player> player = server.getPlayer(uuid);
        if (player.isEmpty()) return false;

        return player
            .get()
            .getCurrentServer()
            .map(connection ->
                connection.sendPluginMessage(
                    us.iorilabs.proxy.velocity.impl.listener.VelocityPluginMessageListener.CHANNEL,
                    data
                )
            )
            .orElse(false);
    }

    private BackendServer toBackend(RegisteredServer registered) {
        return new BackendServer(registered.getServerInfo().getName(), true, registered.getPlayersConnected().size());
    }
}
