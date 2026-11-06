package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.proxy.commands.CommandSource;

public final class BroadcastCommand extends EssentialsCommand {

    public BroadcastCommand(IoriCorePlugin plugin) {
        super(plugin, "broadcast", Arrays.asList("bc", "bcast"), "ioricore.essentials.broadcast");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.broadcast")) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) builder.append(' ');
            builder.append(args[i]);
        }
        String message = builder.toString().trim();
        if (message.isEmpty()) {
            sendUsage(source);
            return;
        }
        if (message.length() > 256) {
            send(source, "ESSENTIALS.BROADCAST.TOO-LONG");
            return;
        }

        Map<String, String> placeholders = Map.of("message", message, "sender", source.name());
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(language.raw(online, "ESSENTIALS.BROADCAST.FORMAT", placeholders));
        }
        Bukkit.getConsoleSender().sendMessage(language.raw("ESSENTIALS.BROADCAST.FORMAT", placeholders));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.BROADCAST.USAGE");
    }
}
