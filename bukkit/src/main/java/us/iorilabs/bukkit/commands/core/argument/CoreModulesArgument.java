package us.iorilabs.bukkit.commands.core.argument;

import java.util.Collection;
import java.util.Map;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.core.IoriCoreCommand;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class CoreModulesArgument extends CommandArgument {

    private final IoriCorePlugin plugin;

    public CoreModulesArgument(IoriCorePlugin plugin) {
        super("modules", IoriCoreCommand.PERMISSION);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        Collection<CoreModule> modules = plugin.modules().all();
        long on = modules.stream().filter(CoreModule::enabled).count();
        source.sendMessage(
            plugin
                .language()
                .raw(
                    source,
                    "CORE.MODULES-HEADER",
                    Map.of("on", String.valueOf(on), "total", String.valueOf(modules.size()))
                )
        );
        String enabled = plugin.language().raw(source, "CORE.MODULE-ON");
        String disabled = plugin.language().raw(source, "CORE.MODULE-OFF");
        for (CoreModule module : modules) {
            source.sendMessage(
                plugin
                    .language()
                    .raw(
                        source,
                        "CORE.MODULE-LINE",
                        Map.of("module", module.id().name(), "state", module.enabled() ? enabled : disabled)
                    )
            );
        }
    }
}
