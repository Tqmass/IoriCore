package us.iorilabs.bukkit.commands.core.argument;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.core.IoriCoreCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class CoreVersionArgument extends CommandArgument {

    private final IoriCorePlugin plugin;

    public CoreVersionArgument(IoriCorePlugin plugin) {
        super("version", IoriCoreCommand.PERMISSION);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("version", plugin.getDescription().getVersion());
        placeholders.put("server", Bukkit.getName() + " " + Bukkit.getBukkitVersion());
        placeholders.put("revision", plugin.abstraction() == null ? "-" : plugin.abstraction().revision());
        placeholders.put("storage", plugin.storageBackend().name());
        for (String line : plugin.language().list(source, "CORE.VERSION", placeholders)) {
            source.sendMessage(line);
        }
    }
}
