package us.iorilabs.proxy.bungee.impl.listener;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import us.iorilabs.api.messaging.ClientProtocolPayload;
import us.iorilabs.api.messaging.PluginChannels;
import us.iorilabs.api.messaging.PluginMessageType;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.bungee.player.BungeeProxyPlayer;
import us.iorilabs.proxy.redis.RedisEvent;

public final class BungeePluginMessageListener implements Listener {

    private final IoriProxy core;
    private final ProxyServer proxy;
    private final BungeeAudiences audiences;
    private boolean registered;

    public BungeePluginMessageListener(IoriProxy core, ProxyServer proxy, BungeeAudiences audiences) {
        this.core = core;
        this.proxy = proxy;
        this.audiences = audiences;
    }

    public void enable() {
        if (registered) return;
        if (!core.configs().settings().moduleEnabled("plugin_messaging")) return;

        proxy.registerChannel(PluginChannels.MAIN);
        registered = true;
    }

    public void disable() {
        if (!registered) return;
        proxy.unregisterChannel(PluginChannels.MAIN);
        registered = false;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!PluginChannels.MAIN.equals(event.getTag())) return;

        event.setCancelled(true);

        if (!(event.getSender() instanceof Server)) return;

        byte[] data = event.getData();
        if (data == null || data.length < 1) return;

        PluginMessageType type = PluginMessageType.fromId(data[0] & 0xFF);
        if (type == null) return;

        if (type == PluginMessageType.HANDSHAKE_PONG && core.configs().settings().debug()) {
            Server server = (Server) event.getSender();
            core.logger().info("[messaging] handshake pong from " + server.getInfo().getName());
        }
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        if (!registered) return;

        event.getServer().sendData(PluginChannels.MAIN, new byte[] { PluginMessageType.HANDSHAKE_PING.id() });
        pushClientProtocol(event.getPlayer(), event.getServer());
        pushStaffAuthSession(event.getPlayer(), event.getServer());
    }

    private void pushClientProtocol(ProxiedPlayer player, Server server) {
        if (server == null) return;

        int protocol = player.getPendingConnection().getVersion();
        server.sendData(PluginChannels.MAIN, ClientProtocolPayload.encode(protocol));

        if (core.redis() == null || !core.redis().isConnected()) return;
        String target = server.getInfo().getName();
        RedisEvent redisEvent = RedisEvent.clientProtocol(
            core.redis().proxyId(),
            player.getUniqueId(),
            protocol,
            target
        );
        core.platform()
            .asyncExecutor()
            .execute(() -> core.redis().publish(redisEvent));
    }

    private void pushStaffAuthSession(ProxiedPlayer player, Server server) {
        if (server == null || core.staffReg() == null) return;

        BungeeProxyPlayer wrapped = new BungeeProxyPlayer(proxy, player, audiences);
        boolean staff = core.staffGroups() != null && core.staffGroups().isStaff(wrapped);
        if (!staff) return;

        byte ok = (byte) (core.staffReg().isRestricted(player.getUniqueId()) ? 0 : 1);
        server.sendData(PluginChannels.MAIN, new byte[] { PluginMessageType.STAFF_AUTH_SESSION.id(), ok });
    }
}

