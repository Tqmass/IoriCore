package us.iorilabs.proxy.bungee;

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
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import us.iorilabs.proxy.bungee.command.BungeeProxyCommand;
import us.iorilabs.proxy.bungee.player.BungeeProxyPlayer;
import us.iorilabs.proxy.commands.CommandRegistry;
import us.iorilabs.proxy.commands.ProxyCommand;
import us.iorilabs.proxy.platform.BackendServer;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.ColorUtil;

@Accessors(fluent = true)
public final class BungeePlatform implements ProxyPlatform {

    @Getter
    private final IoriProxyBungee plugin;

    @Getter
    private final BungeeAudiences audiences;

    private final Map<String, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public BungeePlatform(IoriProxyBungee plugin) {
        this.plugin = plugin;
        this.audiences = BungeeAudiences.create(plugin);
    }

    @Override
    public String platformName() {
        return "BungeeCord";
    }

    @Override
    public String version() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public Path dataDirectory() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public Logger julLogger() {
        return plugin.getLogger();
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void warn(String message) {
        plugin.getLogger().warning(message);
    }

    @Override
    public void error(String message) {
        plugin.getLogger().severe(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        plugin.getLogger().log(java.util.logging.Level.SEVERE, message, throwable);
    }

    @Override
    public Executor asyncExecutor() {
        return runnable -> plugin.getProxy().getScheduler().runAsync(plugin, runnable);
    }

    @Override
    public void runSync(Runnable task) {
        plugin.getProxy().getScheduler().schedule(plugin, task, 0L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runLater(Runnable task, long delayMs) {
        plugin.getProxy().getScheduler().schedule(plugin, task, delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runTimer(Runnable task, long delayMs, long periodMs, String taskId) {
        cancelTask(taskId);
        ScheduledTask scheduled = plugin
            .getProxy()
            .getScheduler()
            .schedule(plugin, task, delayMs, periodMs, TimeUnit.MILLISECONDS);
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
        return plugin.getResourceAsStream(path);
    }

    @Override
    public void registerCommands(CommandRegistry registry) {
        for (ProxyCommand command : registry.uniqueCommands()) {
            Set<String> labels = new LinkedHashSet<>();
            labels.add(command.name().toLowerCase(Locale.ROOT));

            for (String alias : command.aliases()) {
                labels.add(alias.toLowerCase(Locale.ROOT));
            }

            for (String configured : plugin.core().configs().commands().aliases(command.name())) {
                labels.add(configured.toLowerCase(Locale.ROOT));
            }

            String primary = command.name().toLowerCase(Locale.ROOT);
            String[] aliasArray = labels
                .stream()
                .filter(label -> !label.equals(primary))
                .toArray(String[]::new);
            plugin
                .getProxy()
                .getPluginManager()
                .registerCommand(
                    plugin,
                    new BungeeProxyCommand(plugin.core(), command, audiences, primary, command.permission(), aliasArray)
                );
        }
    }

    @Override
    public Optional<ProxyPlayer> findPlayerByName(String name) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(name);
        if (player == null) return Optional.empty();

        return Optional.of(new BungeeProxyPlayer(plugin.getProxy(), player, audiences));
    }

    @Override
    public Optional<ProxyPlayer> findPlayerByUuid(UUID uuid) {
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            if (player.getUniqueId().equals(uuid)) {
                return Optional.of(new BungeeProxyPlayer(plugin.getProxy(), player, audiences));
            }
        }

        return Optional.empty();
    }

    @Override
    public Collection<ProxyPlayer> onlinePlayers() {
        Collection<ProxyPlayer> players = new ArrayList<>();

        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            players.add(new BungeeProxyPlayer(plugin.getProxy(), player, audiences));
        }

        return players;
    }

    @Override
    public Collection<BackendServer> registeredServers() {
        Collection<BackendServer> backends = new ArrayList<>();

        for (ServerInfo info : plugin.getProxy().getServers().values()) {
            backends.add(new BackendServer(info.getName(), true, info.getPlayers().size()));
        }

        return backends;
    }

    @Override
    public Optional<BackendServer> server(String name) {
        ServerInfo info = plugin.getProxy().getServerInfo(name);
        if (info == null) return Optional.empty();

        return Optional.of(new BackendServer(info.getName(), true, info.getPlayers().size()));
    }

    @Override
    public boolean connectPlayer(UUID uuid, String serverName) {
        Optional<ProxyPlayer> player = findPlayerByUuid(uuid);
        ServerInfo info = plugin.getProxy().getServerInfo(serverName);
        if (player.isEmpty() || info == null) return false;

        player.get().connect(serverName);
        return true;
    }

    @Override
    public void kickPlayer(UUID uuid, String miniOrLegacy) {
        findPlayerByUuid(uuid).ifPresent(player ->
            ((BungeeProxyPlayer) player)
                .handle()
                .disconnect(
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ColorUtil.toLegacySection(miniOrLegacy))
                )
        );
    }

    @Override
    public void broadcast(String miniOrLegacy) {
        var component = ColorUtil.parse(miniOrLegacy);

        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            audiences.player(player).sendMessage(component);
        }
    }

    @Override
    public void broadcastToServer(String serverName, String miniOrLegacy) {
        ServerInfo info = plugin.getProxy().getServerInfo(serverName);
        if (info == null) return;

        var component = ColorUtil.parse(miniOrLegacy);

        for (ProxiedPlayer player : info.getPlayers()) {
            audiences.player(player).sendMessage(component);
        }
    }

    @Override
    public void sendActionBar(UUID uuid, String miniOrLegacy) {
        findPlayerByUuid(uuid).ifPresent(player ->
            audiences.player(((BungeeProxyPlayer) player).handle()).sendActionBar(ColorUtil.parse(miniOrLegacy))
        );
    }

    @Override
    public void sendTitle(UUID uuid, String titleMini, String subtitleMini) {
        findPlayerByUuid(uuid).ifPresent(player ->
            audiences
                .player(((BungeeProxyPlayer) player).handle())
                .showTitle(
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
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
        if (player == null) return false;

        try {
            audiences.player(player).playSound(Sound.sound(Key.key(soundKey), Sound.Source.MASTER, volume, pitch));
            return true;
        } catch (InvalidKeyException ex) {
            return false;
        }
    }

    @Override
    public boolean sendBackendPluginMessage(UUID uuid, byte[] data) {
        if (uuid == null || data == null || data.length == 0) return false;

        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
        if (player == null || player.getServer() == null) return false;

        player.getServer().sendData(us.iorilabs.api.messaging.PluginChannels.MAIN, data);
        return true;
    }

    public void close() {
        for (String id : Set.copyOf(tasks.keySet())) {
            cancelTask(id);
        }

        audiences.close();
    }
}
