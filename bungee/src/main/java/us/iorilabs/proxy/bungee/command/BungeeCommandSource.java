package us.iorilabs.proxy.bungee.command;

import java.util.UUID;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import us.iorilabs.proxy.utilities.ColorUtil;

public final class BungeeCommandSource implements us.iorilabs.proxy.commands.CommandSource {

    private final CommandSender sender;
    private final BungeeAudiences audiences;

    public BungeeCommandSource(CommandSender sender, BungeeAudiences audiences) {
        this.sender = sender;
        this.audiences = audiences;
    }

    @Override
    public String name() {
        return sender.getName();
    }

    @Override
    public boolean isConsole() {
        return !(sender instanceof ProxiedPlayer);
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public void sendMessage(String miniOrLegacy) {
        audiences.sender(sender).sendMessage(ColorUtil.parse(miniOrLegacy));
    }

    @Override
    public void sendMessage(Component component) {
        audiences.sender(sender).sendMessage(component);
    }

    @Override
    public UUID uuidOrNull() {
        if (sender instanceof ProxiedPlayer player) return player.getUniqueId();

        return null;
    }
}
