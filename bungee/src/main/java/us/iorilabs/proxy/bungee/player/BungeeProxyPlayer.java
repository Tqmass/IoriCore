package us.iorilabs.proxy.bungee.player;

import java.util.UUID;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.ColorUtil;

public final class BungeeProxyPlayer implements ProxyPlayer {

    private final ProxyServer proxy;
    private final ProxiedPlayer player;
    private final BungeeAudiences audiences;

    public BungeeProxyPlayer(ProxyServer proxy, ProxiedPlayer player, BungeeAudiences audiences) {
        this.proxy = proxy;
        this.player = player;
        this.audiences = audiences;
    }

    @Override
    public UUID uuid() {
        return player.getUniqueId();
    }

    @Override
    public String name() {
        return player.getName();
    }

    @Override
    public String currentServer() {
        return player.getServer() == null ? "" : player.getServer().getInfo().getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(String miniOrLegacy) {
        audiences.player(player).sendMessage(ColorUtil.parse(miniOrLegacy));
    }

    @Override
    public void sendMessage(Component component) {
        audiences.player(player).sendMessage(component);
    }

    @Override
    public void connect(String serverName) {
        ServerInfo info = proxy.getServerInfo(serverName);
        if (info != null) {
            player.connect(info);
        }
    }

    @Override
    public int protocolVersion() {
        return player.getPendingConnection().getVersion();
    }

    @Override
    public int ping() {
        return player.getPing();
    }

    @Override
    public java.util.Optional<String> ipAddress() {
        if (player.getAddress() == null || player.getAddress().getAddress() == null) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.ofNullable(player.getAddress().getAddress().getHostAddress());
    }

    @Override
    public java.util.Optional<String> clientBrand() {
        try {
            Object brand = player.getClass().getMethod("getClientBrand").invoke(player);
            if (brand instanceof String text && !text.isBlank()) {
                return java.util.Optional.of(text.trim());
            }
        } catch (ReflectiveOperationException ignored) {}
        return java.util.Optional.empty();
    }

    public ProxiedPlayer handle() {
        return player;
    }
}
