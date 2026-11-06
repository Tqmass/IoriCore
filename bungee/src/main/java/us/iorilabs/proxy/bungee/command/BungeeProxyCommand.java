package us.iorilabs.proxy.bungee.command;

import java.util.Locale;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.ProxyCommand;

public final class BungeeProxyCommand extends Command implements TabExecutor {

    private final IoriProxy proxy;
    private final ProxyCommand command;
    private final BungeeAudiences audiences;

    public BungeeProxyCommand(
        IoriProxy proxy,
        ProxyCommand command,
        BungeeAudiences audiences,
        String name,
        String permission,
        String... aliases
    ) {
        super(name.toLowerCase(Locale.ROOT), permission, lower(aliases));
        this.proxy = proxy;
        this.command = command;
        this.audiences = audiences;
    }

    private static String[] lower(String[] aliases) {
        String[] out = new String[aliases.length];

        for (int i = 0; i < aliases.length; i++) {
            out[i] = aliases[i].toLowerCase(Locale.ROOT);
        }

        return out;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        BungeeCommandSource source = new BungeeCommandSource(sender, audiences);
        String permission = command.permission();
        if (permission != null && !permission.isBlank() && !source.hasPermission(permission) && !source.isConsole()) {
            source.sendMessage(proxy.language().raw("general.no_permission"));
            return;
        }

        command.execute(source, getName(), args == null ? new String[0] : args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return command.suggest(
            new BungeeCommandSource(sender, audiences),
            getName(),
            args == null ? new String[0] : args
        );
    }
}
