package us.iorilabs.bukkit.commands;

import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import us.iorilabs.proxy.commands.CommandSource;

public final class BukkitCommandSource implements CommandSource {

    private final CommandSender sender;
    private final String sourceNode;
    private final String targetNode;

    public BukkitCommandSource(CommandSender sender) {
        this(sender, null, null);
    }

    public BukkitCommandSource(CommandSender sender, String sourceNode, String targetNode) {
        this.sender = sender;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
    }

    public CommandSender handle() {
        return sender;
    }

    public Player playerOrNull() {
        return sender instanceof Player ? (Player) sender : null;
    }

    @Override
    public String name() {
        return sender.getName();
    }

    @Override
    public boolean isConsole() {
        return sender instanceof ConsoleCommandSender;
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) return true;
        if (sourceNode != null && targetNode != null && sourceNode.equals(permission)) {
            return sender.hasPermission(targetNode);
        }
        return sender.hasPermission(permission);
    }

    @Override
    public void sendMessage(String miniOrLegacy) {
        if (miniOrLegacy == null || miniOrLegacy.isEmpty()) return;
        sender.sendMessage(miniOrLegacy);
    }

    @Override
    public UUID uuidOrNull() {
        return sender instanceof Player ? ((Player) sender).getUniqueId() : null;
    }
}
