package us.iorilabs.proxy.platform;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import us.iorilabs.proxy.commands.CommandRegistry;
import us.iorilabs.proxy.player.ProxyPlayer;

public interface ProxyPlatform {
    String platformName();

    String version();

    Path dataDirectory();

    Logger julLogger();

    void info(String message);

    void warn(String message);

    void error(String message);

    void error(String message, Throwable throwable);

    Executor asyncExecutor();

    void runSync(Runnable task);

    void runLater(Runnable task, long delayMs);

    void runTimer(Runnable task, long delayMs, long periodMs, String taskId);

    void cancelTask(String taskId);

    InputStream getResource(String path);

    void registerCommands(CommandRegistry registry);

    Optional<ProxyPlayer> findPlayerByName(String name);

    Optional<ProxyPlayer> findPlayerByUuid(UUID uuid);

    Collection<ProxyPlayer> onlinePlayers();

    Collection<BackendServer> registeredServers();

    Optional<BackendServer> server(String name);

    boolean connectPlayer(UUID uuid, String serverName);

    void kickPlayer(UUID uuid, String miniOrLegacy);

    void broadcast(String miniOrLegacy);

    void broadcastToServer(String serverName, String miniOrLegacy);

    void sendActionBar(UUID uuid, String miniOrLegacy);

    void sendTitle(UUID uuid, String titleMini, String subtitleMini);

    default boolean playSound(UUID uuid, String soundKey, float volume, float pitch) {
        return false;
    }

    default boolean sendBackendPluginMessage(UUID uuid, byte[] data) {
        return false;
    }
}
