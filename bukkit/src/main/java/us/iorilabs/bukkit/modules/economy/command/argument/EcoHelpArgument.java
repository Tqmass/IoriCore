package us.iorilabs.bukkit.modules.economy.command.argument;

import us.iorilabs.bukkit.modules.economy.command.EcoCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class EcoHelpArgument extends CommandArgument {

    private final EcoCommand parent;

    public EcoHelpArgument(EcoCommand parent) {
        super("help", null);
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        parent.sendUsage(source);
    }
}
