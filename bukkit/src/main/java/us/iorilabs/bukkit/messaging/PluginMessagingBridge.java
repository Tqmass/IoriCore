package us.iorilabs.bukkit.messaging;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;
import us.iorilabs.api.messaging.ClientProtocolPayload;
import us.iorilabs.api.messaging.PluginChannels;
import us.iorilabs.api.messaging.PluginMessageType;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class PluginMessagingBridge implements PluginMessageListener {

    private static final int MAX_LENGTH = 64;

    private final IoriCorePlugin plugin;
    private boolean registered;

    public PluginMessagingBridge(IoriCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (registered) return;

        Messenger messenger = plugin.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, PluginChannels.MAIN);
        messenger.registerIncomingPluginChannel(plugin, PluginChannels.MAIN, this);
        registered = true;
    }

    public void disable() {
        if (!registered) return;

        Messenger messenger = plugin.getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(plugin, PluginChannels.MAIN, this);
        messenger.unregisterOutgoingPluginChannel(plugin, PluginChannels.MAIN);
        registered = false;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!registered || !PluginChannels.MAIN.equals(channel)) return;
        if (player == null || !player.isOnline()) return;
        if (message == null || message.length < 1 || message.length > MAX_LENGTH) return;

        PluginMessageType type = PluginMessageType.fromId(message[0] & 0xFF);
        if (type == null) return;

        switch (type) {
            case HANDSHAKE_PING -> player.sendPluginMessage(plugin, PluginChannels.MAIN, new byte[] {
                PluginMessageType.HANDSHAKE_PONG.id(),
            });
            case STAFF_AUTH_SESSION -> {
            }
            case CLIENT_PROTOCOL -> {
                int protocol = ClientProtocolPayload.decode(message);
                if (protocol > 0 && plugin.clientProtocols() != null) {
                    plugin.clientProtocols().acceptFromConnection(player, protocol);
                }
            }
            default -> {
            }
        }
    }
}
