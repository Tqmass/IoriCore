package us.iorilabs.bukkit.commands.core.argument;

import java.util.Map;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.core.IoriCoreCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class CoreReloadArgument extends CommandArgument {

    private final IoriCorePlugin plugin;

    public CoreReloadArgument(IoriCorePlugin plugin) {
        super("reload", IoriCoreCommand.PERMISSION);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        source.sendMessage(plugin.language().raw(source, "CORE.RELOADING"));
        long start = System.currentTimeMillis();
        try {
            int modules = plugin.reloadCore();
            source.sendMessage(
                plugin
                    .language()
                    .raw(
                        source,
                        "CORE.RELOADED",
                        Map.of(
                            "time",
                            String.valueOf(System.currentTimeMillis() - start),
                            "modules",
                            String.valueOf(modules)
                        )
                    )
            );
        } catch (RuntimeException ex) {
            plugin.coreLogger().warn("Reload failed: " + ex.getMessage());
            source.sendMessage(
                plugin.language().raw(source, "CORE.RELOAD-FAILED", Map.of("error", String.valueOf(ex.getMessage())))
            );
        }
    }
}
