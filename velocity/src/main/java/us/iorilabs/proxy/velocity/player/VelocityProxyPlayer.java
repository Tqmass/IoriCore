package us.iorilabs.proxy.velocity.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.ColorUtil;

public final class VelocityProxyPlayer implements ProxyPlayer {

    private final ProxyServer proxy;
    private final Player player;

    public VelocityProxyPlayer(ProxyServer proxy, Player player) {
        this.proxy = proxy;
        this.player = player;
    }

    @Override
    public UUID uuid() {
        return player.getUniqueId();
    }

    @Override
    public String name() {
        return player.getUsername();
    }

    @Override
    public String currentServer() {
        return player
            .getCurrentServer()
            .map(connection -> connection.getServerInfo().getName())
            .orElse("");
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(String miniOrLegacy) {
        player.sendMessage(ColorUtil.parse(miniOrLegacy));
    }

    @Override
    public void sendMessage(Component component) {
        player.sendMessage(component);
    }

    @Override
    public void connect(String serverName) {
        proxy.getServer(serverName).ifPresent(server -> player.createConnectionRequest(server).fireAndForget());
    }

    @Override
    public int protocolVersion() {
        return player.getProtocolVersion().getProtocol();
    }

    @Override
    public int ping() {
        return (int) player.getPing();
    }

    @Override
    public java.util.Optional<String> ipAddress() {
        var address = player.getRemoteAddress();
        if (address == null || address.getAddress() == null) return java.util.Optional.empty();

        return java.util.Optional.ofNullable(address.getAddress().getHostAddress());
    }

    @Override
    public java.util.Optional<String> clientBrand() {
        String brand = player.getClientBrand();
        if (brand == null || brand.isBlank()) return java.util.Optional.empty();
        return java.util.Optional.of(brand.trim());
    }

    public Player handle() {
        return player;
    }
}
