package us.iorilabs.bukkit.commands.core;

import java.util.Arrays;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.core.argument.CoreModulesArgument;
import us.iorilabs.bukkit.commands.core.argument.CoreReloadArgument;
import us.iorilabs.bukkit.commands.core.argument.CoreVersionArgument;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

public final class IoriCoreCommand extends CommandParent {

    public static final String PERMISSION = "ioricore.admin";

    private final IoriCorePlugin plugin;

    public IoriCoreCommand(IoriCorePlugin plugin) {
        super("ioricore", Arrays.asList("icore"), PERMISSION);
        this.plugin = plugin;
        register(new CoreReloadArgument(plugin));
        register(new CoreVersionArgument(plugin));
        register(new CoreModulesArgument(plugin));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        for (String line : plugin.language().list(source, "CORE.USAGE")) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return plugin.language().raw("GENERAL.NO-PERMISSION");
    }
}
