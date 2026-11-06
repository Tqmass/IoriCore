package us.iorilabs.proxy.velocity.impl.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import us.iorilabs.api.messaging.ClientProtocolPayload;
import us.iorilabs.api.messaging.PluginChannels;
import us.iorilabs.api.messaging.PluginMessageType;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.velocity.player.VelocityProxyPlayer;

public final class VelocityPluginMessageListener {

    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(PluginChannels.MAIN);

    private final IoriProxy core;
    private final ProxyServer proxy;
    private boolean registered;

    public VelocityPluginMessageListener(IoriProxy core, ProxyServer proxy) {
        this.core = core;
        this.proxy = proxy;
    }

    public void enable() {
        if (registered) return;
        if (!core.configs().settings().moduleEnabled("plugin_messaging")) return;

        proxy.getChannelRegistrar().register(CHANNEL);
        registered = true;
    }

    public void disable() {
        if (!registered) return;
        proxy.getChannelRegistrar().unregister(CHANNEL);
        registered = false;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection backend)) return;

        byte[] data = event.getData();
        if (data == null || data.length < 1) return;

        PluginMessageType type = PluginMessageType.fromId(data[0] & 0xFF);
        if (type == null) return;

        if (type == PluginMessageType.HANDSHAKE_PONG && core.configs().settings().debug()) {
            core.logger().info("[messaging] handshake pong from " + backend.getServerInfo().getName());
        }
    }

    @Subscribe
    public void onServerConnected(ServerPostConnectEvent event) {
        if (!registered) return;

        Player player = event.getPlayer();
        player.getCurrentServer().ifPresent(connection -> {
            connection.sendPluginMessage(CHANNEL, new byte[] { PluginMessageType.HANDSHAKE_PING.id() });
            pushClientProtocol(player, connection);
            pushStaffAuthSession(player, connection);
        });
    }

    private void pushClientProtocol(Player player, ServerConnection server) {
        int protocol = player.getProtocolVersion().getProtocol();
        server.sendPluginMessage(CHANNEL, ClientProtocolPayload.encode(protocol));

        if (core.redis() == null || !core.redis().isConnected()) return;
        String target = server.getServerInfo().getName();
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

    private void pushStaffAuthSession(Player player, ServerConnection server) {
        if (core.staffAuth() == null) return;

        VelocityProxyPlayer wrapped = new VelocityProxyPlayer(proxy, player);
        if (!core.staffAuth().isStaff(wrapped)) return;

        server.sendPluginMessage(CHANNEL, core.staffAuth().encodeSessionPayload(player.getUniqueId()));
    }
}
