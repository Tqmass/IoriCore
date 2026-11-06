package us.iorilabs.proxy.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.config.LobbyConfig;
import us.iorilabs.proxy.platform.BackendServer;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.PlayerPresence;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class ServerRouter {

    private final ProxyPlatform platform;
    private final PresenceService presence;
    private final LobbyConfig lobbyConfig;

    public ServerRouter(ProxyPlatform platform, PresenceService presence, LobbyConfig lobbyConfig) {
        this.platform = platform;
        this.presence = presence;
        this.lobbyConfig = lobbyConfig;
    }

    public Optional<BackendServer> resolveServer(String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        return platform.server(name);
    }

    public boolean send(UUID playerId, String serverName) {
        Optional<BackendServer> server = resolveServer(serverName);
        if (server.isEmpty() || !server.get().online()) return false;

        return platform.connectPlayer(playerId, server.get().name());
    }

    public Optional<PlayerPresence> locate(String playerName) {
        Optional<ProxyPlayer> local = platform.findPlayerByName(playerName);
        if (local.isPresent()) {
            ProxyPlayer player = local.get();
            return Optional.of(
                new PlayerPresence(
                    player.uuid(),
                    player.name(),
                    player.currentServer(),
                    "local",
                    System.currentTimeMillis()
                )
            );
        }

        return presence.findByName(playerName);
    }

    public Optional<String> pickLobby(UUID excludeIfAlreadyOn) {
        List<String> candidates = lobbyConfig.multilobbyEnabled()
            ? lobbyConfig.lobbies()
            : lobbyConfig.fallbackServers();
        List<BackendServer> online = new ArrayList<>();

        for (String name : candidates) {
            platform.server(name).filter(BackendServer::online).ifPresent(online::add);
        }

        if (online.isEmpty()) return Optional.empty();

        if ("random".equals(lobbyConfig.balancing())) {
            return Optional.of(online.get((int) (Math.random() * online.size())).name());
        }

        online.sort(Comparator.comparingInt(BackendServer::players));

        if (excludeIfAlreadyOn != null) {
            Optional<ProxyPlayer> player = platform.findPlayerByUuid(excludeIfAlreadyOn);
            if (player.isPresent()) {
                String current = player.get().currentServer();
                if (current != null && !current.isBlank() && online.size() > 1) {
                    online.removeIf(server -> server.name().equalsIgnoreCase(current));
                }
            }
        }

        return online.stream().findFirst().map(BackendServer::name);
    }

    public Optional<String> pickFallback(String excludeServer) {
        List<BackendServer> online = new ArrayList<>();

        for (String name : lobbyConfig.fallbackServers()) {
            if (excludeServer != null && name.equalsIgnoreCase(excludeServer)) continue;

            platform.server(name).filter(BackendServer::online).ifPresent(online::add);
        }

        online.sort(Comparator.comparingInt(BackendServer::players));
        return online.stream().findFirst().map(BackendServer::name);
    }

    public List<BackendServer> listServers() {
        return List.copyOf(platform.registeredServers());
    }

    public static String normalizeServer(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }
}
