package us.iorilabs.bukkit.ranks.argument;

import us.iorilabs.bukkit.ranks.RanksCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class RanksListArgument extends CommandArgument {

    private final RanksCommand parent;

    public RanksListArgument(RanksCommand parent) {
        super("list", null);
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        parent.openMenu(source);
    }
}
