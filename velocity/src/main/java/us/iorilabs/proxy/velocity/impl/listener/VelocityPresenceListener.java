package us.iorilabs.proxy.velocity.impl.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.maintenance.MaintenanceState;
import us.iorilabs.proxy.utilities.ColorUtil;
import us.iorilabs.proxy.velocity.player.VelocityProxyPlayer;

public final class VelocityPresenceListener {

    private final IoriProxy core;
    private final ProxyServer proxy;

    public VelocityPresenceListener(IoriProxy core, ProxyServer proxy) {
        this.core = core;
        this.proxy = proxy;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();

        if (
            core.blacklist() != null &&
            core.blacklist().enabled() &&
            core.blacklist().isBlacklisted(player.getUniqueId(), player.getUsername())
        ) {
            event.setResult(LoginEvent.ComponentResult.denied(ColorUtil.parse(core.blacklist().denyMessage())));
            return;
        }

        if (core.banCheck() != null) {
            Optional<String> banDeny = core.banCheck().denyJoinMessage(player.getUniqueId());
            if (banDeny.isPresent()) {
                event.setResult(LoginEvent.ComponentResult.denied(ColorUtil.parse(banDeny.get())));
                return;
            }
        }

        if (core.maintenance() != null && core.maintenance().isGlobalActive()) {
            VelocityProxyPlayer wrapped = new VelocityProxyPlayer(proxy, player);
            if (!core.maintenance().canBypass(wrapped)) {
                Optional<MaintenanceState> state = core.maintenance().blockingState(null);
                String scope = state.map(s -> s.scopeId()).orElse("global");
                if (core.staffAlerts() != null) {
                    core.staffAlerts().maintenanceAttempt(player.getUsername(), scope);
                }

                String message = state
                    .map(s -> core.maintenance().kickMessage(s, player.getProtocolVersion().getProtocol()))
                    .orElse(
                        core.language().raw("maintenance.kick", Map.of("reason", "Maintenance", "scope", "global"))
                    );
                event.setResult(LoginEvent.ComponentResult.denied(ColorUtil.parse(message)));
            }
        }
    }

    @Subscribe
    public void onConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String server = event.getServer().getServerInfo().getName();
        if (core.maintenance() != null && core.maintenance().isServerBlocked(server)) {
            VelocityProxyPlayer wrapped = new VelocityProxyPlayer(proxy, player);
            if (!core.maintenance().canBypass(wrapped)) {
                if (core.staffAlerts() != null) {
                    core.staffAlerts().maintenanceAttempt(player.getUsername(), server);
                }

                Optional<String> fallback = core.router().pickFallback(server);
                if (fallback.isPresent()) {
                    proxy
                        .getServer(fallback.get())
                        .ifPresent(target -> player.createConnectionRequest(target).fireAndForget());
                } else {
                    core.maintenance()
                        .blockingState(server)
                        .ifPresent(state -> player.disconnect(ColorUtil.parse(core.maintenance().kickMessage(state))));
                }

                return;
            }
        }

        if (event.getPreviousServer().isEmpty()) {
            core.presence().localJoin(player.getUniqueId(), player.getUsername(), server);

            if (core.analytics() != null) {
                core.analytics().recordJoin();
            }

            if (core.playtime() != null) {
                core.playtime().onJoin(player.getUniqueId(), server);
            }

            String ip =
                player.getRemoteAddress() != null && player.getRemoteAddress().getAddress() != null
                    ? player.getRemoteAddress().getAddress().getHostAddress()
                    : "";

            if (core.playerData() != null) {
                core.playerData().onJoin(player.getUniqueId(), player.getUsername(), ip);
            }

            if (core.alts() != null) {
                core.alts().onJoin(player.getUniqueId(), player.getUsername(), ip);
            }

            if (core.staffReg() != null) {
                core.staffReg().onStaffJoin(new VelocityProxyPlayer(proxy, player), ip);
            }

            if (core.friendService() != null) {
                core.friendService().onLocalJoin(player.getUniqueId(), player.getUsername(), server);
            }

            if (core.staffAlerts() != null) {
                core.staffAlerts().join(player.getUsername(), server);
            }

            if (core.discordBot() != null) {
                core.discordBot().onPlayerJoin(new VelocityProxyPlayer(proxy, player), server);
            }
        } else {
            String from = event.getPreviousServer().get().getServerInfo().getName();
            if (!from.equalsIgnoreCase(server)) {
                core.presence().localSwitch(player.getUniqueId(), player.getUsername(), server);

                if (core.playtime() != null) {
                    core.playtime().onSwitch(player.getUniqueId(), server);
                }

                if (core.friendService() != null) {
                    core.friendService().onLocalSwitch(player.getUniqueId(), player.getUsername(), server);
                }

                if (core.staffAlerts() != null) {
                    core.staffAlerts().switchServer(player.getUsername(), from, server);
                }
            }
        }

        if (core.analytics() != null) {
            core.analytics().recordOnlineCount(proxy.getPlayerCount());
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (core.playtime() != null) {
            core.playtime().onQuit(event.getPlayer().getUniqueId());
        }

        if (core.friendService() != null) {
            core.friendService().onLocalQuit(event.getPlayer().getUniqueId(), event.getPlayer().getUsername());
        }

        String quitServer = event
            .getPlayer()
            .getCurrentServer()
            .map(s -> s.getServerInfo().getName())
            .orElse("?");

        if (core.staffAlerts() != null) {
            core.staffAlerts().quit(event.getPlayer().getUsername(), quitServer);
        }

        if (core.discordBot() != null) {
            core.discordBot().onPlayerQuit(event.getPlayer().getUsername(), quitServer);
        }

        core.presence().localQuit(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onKicked(KickedFromServerEvent event) {
        if (!core.configs().lobby().onServerKick()) return;

        String reasonPlain = event
            .getServerKickReason()
            .map(component ->
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component)
            )
            .orElse("");
        if (core.configs().lobby().isIgnoredKickReason(reasonPlain)) return;

        String from = event.getServer().getServerInfo().getName();
        Optional<String> fallback = core.router().pickFallback(from);
        if (fallback.isEmpty()) return;

        Optional<RegisteredServer> server = proxy.getServer(fallback.get());
        if (server.isEmpty()) return;

        Player player = event.getPlayer();
        if (core.configs().lobby().clearChat()) {
            for (int i = 0; i < 100; i++) {
                player.sendMessage(net.kyori.adventure.text.Component.empty());
            }
        }

        String title = core.language().raw("lobby.title_fallback");
        String subtitle = core.language().raw("lobby.subtitle_fallback");
        if (!title.isBlank() || !subtitle.isBlank()) {
            core.platform().sendTitle(player.getUniqueId(), title, subtitle);
        }

        player.sendMessage(
            ColorUtil.parse(core.language().raw("lobby.fallback_kick", Map.of("server", fallback.get())))
        );
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(server.get()));
    }

    @Subscribe
    public void onPing(ProxyPingEvent event) {
        if (core.motd() == null || !core.motd().enabled()) return;

        int protocol = event.getConnection().getProtocolVersion().getProtocol();
        String description = core.motd().description(protocol);
        ServerPing.Builder builder = event.getPing().asBuilder();

        if (!description.isBlank()) {
            builder.description(ColorUtil.parse(description));
        }

        core.motd()
            .maintenanceCountMessage()
            .ifPresent(message ->
                builder.version(new ServerPing.Version(protocol, ColorUtil.toLegacySection(message)))
            );

        List<String> samples = core.motd().maintenanceSampleNames();
        if (!samples.isEmpty()) {
            builder.clearSamplePlayers();
            List<ServerPing.SamplePlayer> players = new ArrayList<>(samples.size());
            for (String name : samples) {
                players.add(new ServerPing.SamplePlayer(ColorUtil.toLegacySection(name), UUID.randomUUID()));
            }
            builder.samplePlayers(players);
        }

        event.setPing(builder.build());
    }
}
