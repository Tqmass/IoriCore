package us.iorilabs.proxy.bungee.impl.listener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.bungee.player.BungeeProxyPlayer;
import us.iorilabs.proxy.maintenance.MaintenanceState;
import us.iorilabs.proxy.punishment.BanCheckService;
import us.iorilabs.proxy.utilities.ColorUtil;

public final class BungeePresenceListener implements Listener {

    private final Plugin plugin;
    private final IoriProxy core;
    private final ProxyServer proxy;
    private final BungeeAudiences audiences;

    public BungeePresenceListener(Plugin plugin, IoriProxy core, ProxyServer proxy, BungeeAudiences audiences) {
        this.plugin = plugin;
        this.core = core;
        this.proxy = proxy;
        this.audiences = audiences;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(LoginEvent event) {
        String name = event.getConnection().getName();
        UUID uuid = event.getConnection().getUniqueId();

        if (
            core.blacklist() != null &&
            core.blacklist().enabled() &&
            uuid != null &&
            core.blacklist().isBlacklisted(uuid, name)
        ) {
            event.setCancelled(true);
            event.setCancelReason(
                TextComponent.fromLegacyText(ColorUtil.toLegacySection(core.blacklist().denyMessage()))
            );
            return;
        }

        BanCheckService banCheck = core.banCheck();
        if (banCheck == null || uuid == null) return;

        event.registerIntent(plugin);
        proxy.getScheduler().runAsync(plugin, () -> {
            try {
                banCheck.denyJoinMessage(uuid).ifPresent(deny -> {
                    event.setCancelled(true);
                    event.setCancelReason(TextComponent.fromLegacyText(ColorUtil.toLegacySection(deny)));
                });
            } finally {
                event.completeIntent(plugin);
            }
        });
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (core.maintenance() == null || !core.maintenance().isGlobalActive()) return;

        BungeeProxyPlayer wrapped = new BungeeProxyPlayer(proxy, player, audiences);
        if (core.maintenance().canBypass(wrapped)) return;

        Optional<MaintenanceState> state = core.maintenance().blockingState(null);
        String scope = state.map(s -> s.scopeId()).orElse("global");
        if (core.staffAlerts() != null) {
            core.staffAlerts().maintenanceAttempt(player.getName(), scope);
        }

        String message = state
            .map(core.maintenance()::kickMessage)
            .orElse(core.language().raw("maintenance.kick", Map.of("reason", "Maintenance", "scope", "global")));
        player.disconnect(TextComponent.fromLegacyText(ColorUtil.toLegacySection(message)));
    }

    @EventHandler
    public void onConnected(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String server = event.getServer().getInfo().getName();
        BungeeProxyPlayer wrapped = new BungeeProxyPlayer(proxy, player, audiences);

        if (
            core.maintenance() != null &&
            core.maintenance().isServerBlocked(server) &&
            !core.maintenance().canBypass(wrapped)
        ) {
            if (core.staffAlerts() != null) {
                core.staffAlerts().maintenanceAttempt(player.getName(), server);
            }

            Optional<String> fallback = core.router().pickFallback(server);
            if (fallback.isPresent() && proxy.getServerInfo(fallback.get()) != null) {
                player.connect(proxy.getServerInfo(fallback.get()));
            } else {
                Optional<MaintenanceState> state = core.maintenance().blockingState(server);
                String message = state
                    .map(core.maintenance()::kickMessage)
                    .orElse(core.language().raw("maintenance.kick", Map.of("reason", "Maintenance", "scope", server)));
                player.disconnect(TextComponent.fromLegacyText(ColorUtil.toLegacySection(message)));
            }

            return;
        }

        if (core.presence().findByUuid(player.getUniqueId()).isEmpty()) {
            core.presence().localJoin(player.getUniqueId(), player.getName(), server);

            if (core.analytics() != null) {
                core.analytics().recordJoin();
            }

            if (core.playtime() != null) {
                core.playtime().onJoin(player.getUniqueId(), server);
            }

            String ip =
                player.getAddress() != null && player.getAddress().getAddress() != null
                    ? player.getAddress().getAddress().getHostAddress()
                    : "";

            if (core.playerData() != null) {
                core.playerData().onJoin(player.getUniqueId(), player.getName(), ip);
            }

            if (core.alts() != null) {
                core.alts().onJoin(player.getUniqueId(), player.getName(), ip);
            }

            if (core.staffReg() != null) {
                core.staffReg().onStaffJoin(wrapped, ip);
            }

            if (core.friendService() != null) {
                core.friendService().onLocalJoin(player.getUniqueId(), player.getName(), server);
            }

            if (core.staffAlerts() != null) {
                core.staffAlerts().join(player.getName(), server);
            }

            if (core.discordBot() != null) {
                core.discordBot().onPlayerJoin(wrapped, server);
            }
        } else {
            String from = core
                .presence()
                .findByUuid(player.getUniqueId())
                .map(p -> p.server())
                .orElse("?");
            if (!from.equalsIgnoreCase(server)) {
                core.presence().localSwitch(player.getUniqueId(), player.getName(), server);

                if (core.playtime() != null) {
                    core.playtime().onSwitch(player.getUniqueId(), server);
                }

                if (core.friendService() != null) {
                    core.friendService().onLocalSwitch(player.getUniqueId(), player.getName(), server);
                }

                if (core.staffAlerts() != null) {
                    core.staffAlerts().switchServer(player.getName(), from, server);
                }
            }
        }

        if (core.analytics() != null) {
            core.analytics().recordOnlineCount(proxy.getPlayers().size());
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        if (core.playtime() != null) {
            core.playtime().onQuit(event.getPlayer().getUniqueId());
        }

        if (core.friendService() != null) {
            core.friendService().onLocalQuit(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        }

        String quitServer =
            event.getPlayer().getServer() == null ? "?" : event.getPlayer().getServer().getInfo().getName();

        if (core.staffAlerts() != null) {
            core.staffAlerts().quit(event.getPlayer().getName(), quitServer);
        }

        if (core.discordBot() != null) {
            core.discordBot().onPlayerQuit(event.getPlayer().getName(), quitServer);
        }

        core.presence().localQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(ServerKickEvent event) {
        if (!core.configs().lobby().onServerKick()) return;

        String reasonPlain = net.md_5.bungee.api.chat.TextComponent.toLegacyText(event.getKickReasonComponent());
        if (core.configs().lobby().isIgnoredKickReason(reasonPlain)) return;

        String from = event.getKickedFrom() == null ? "" : event.getKickedFrom().getName();
        Optional<String> fallback = core.router().pickFallback(from);
        if (fallback.isEmpty()) return;

        ServerInfo info = proxy.getServerInfo(fallback.get());
        if (info == null) return;

        if (core.configs().lobby().clearChat()) {
            for (int i = 0; i < 100; i++) {
                audiences.player(event.getPlayer()).sendMessage(net.kyori.adventure.text.Component.empty());
            }
        }

        String title = core.language().raw("lobby.title_fallback");
        String subtitle = core.language().raw("lobby.subtitle_fallback");
        if (!title.isBlank() || !subtitle.isBlank()) {
            core.platform().sendTitle(event.getPlayer().getUniqueId(), title, subtitle);
        }

        audiences
            .player(event.getPlayer())
            .sendMessage(ColorUtil.parse(core.language().raw("lobby.fallback_kick", Map.of("server", fallback.get()))));
        event.setCancelled(true);
        event.setCancelServer(info);
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        if (core.motd() == null || !core.motd().enabled()) return;

        int protocol = event.getConnection().getVersion();
        String description = core.motd().description(protocol);
        ServerPing ping = event.getResponse();

        if (!description.isBlank()) {
            ping.setDescriptionComponent(new TextComponent(ColorUtil.toLegacySection(description)));
        }

        core.motd()
            .maintenanceCountMessage()
            .ifPresent(message ->
                ping.setVersion(new ServerPing.Protocol(ColorUtil.toLegacySection(message), protocol))
            );

        List<String> samples = core.motd().maintenanceSampleNames();
        if (!samples.isEmpty()) {
            ServerPing.PlayerInfo[] infos = new ServerPing.PlayerInfo[samples.size()];
            for (int i = 0; i < samples.size(); i++) {
                infos[i] = new ServerPing.PlayerInfo(ColorUtil.toLegacySection(samples.get(i)), UUID.randomUUID());
            }
            ServerPing.Players players = ping.getPlayers();
            int online = players == null ? 0 : players.getOnline();
            int max = players == null ? 0 : players.getMax();
            ping.setPlayers(new ServerPing.Players(max, online, infos));
        }

        event.setResponse(ping);
    }
}
