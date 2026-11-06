package us.iorilabs.proxy.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import us.iorilabs.proxy.utilities.ColorUtil;

public final class VelocityCommandSource implements us.iorilabs.proxy.commands.CommandSource {

    private final CommandSource source;

    public VelocityCommandSource(CommandSource source) {
        this.source = source;
    }

    @Override
    public String name() {
        if (source instanceof Player player) return player.getUsername();

        return "CONSOLE";
    }

    @Override
    public boolean isConsole() {
        return !(source instanceof Player);
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(String miniOrLegacy) {
        source.sendMessage(ColorUtil.parse(miniOrLegacy));
    }

    @Override
    public void sendMessage(Component component) {
        source.sendMessage(component);
    }

    @Override
    public UUID uuidOrNull() {
        if (source instanceof Player player) return player.getUniqueId();

        return null;
    }
}
