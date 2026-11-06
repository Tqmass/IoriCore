package us.iorilabs.proxy.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.ColorUtil;
import us.iorilabs.proxy.utilities.TextNames;

@RequiredArgsConstructor
public final class ServerHealthMonitor {

    public interface Pinger {
        void ping(String serverName, Runnable onSuccess, Runnable onFailure);
    }

    private final ProxyPlatform platform;
    private final LanguageManager language;
    private final ProxyLogger logger;
    private final String adminPermission;
    private final Pinger pinger;
    private final Map<String, Boolean> onlineByServer = new ConcurrentHashMap<>();

    public void start() {
        platform.runTimer(this::tick, 15_000L, 15_000L, "ioriproxy-server-health");
    }

    private void tick() {
        for (var server : platform.registeredServers()) {
            String name = server.name();
            pinger.ping(name, () -> mark(name, true), () -> mark(name, false));
        }
    }

    private void mark(String server, boolean online) {
        Boolean previous = onlineByServer.put(server, online);
        if (previous == null) return;

        if (previous && !online) {
            alertDown(server);
        } else if (!previous && online) {
            alertUp(server);
        }
    }

    private void alertDown(String server) {
        String line = language.raw("alerts.server_down", Map.of("server", TextNames.displayServer(server)));
        platform.info(ColorUtil.toAnsi(line));
        for (ProxyPlayer online : platform.onlinePlayers()) {
            if (online.hasPermission(adminPermission)) {
                online.sendMessage(line);
            }
        }

        logger.module("Health", "server down: " + server);
    }

    private void alertUp(String server) {
        String line = language.raw("alerts.server_up", Map.of("server", TextNames.displayServer(server)));
        platform.info(ColorUtil.toAnsi(line));
        for (ProxyPlayer online : platform.onlinePlayers()) {
            if (online.hasPermission(adminPermission)) {
                online.sendMessage(line);
            }
        }
    }
}
