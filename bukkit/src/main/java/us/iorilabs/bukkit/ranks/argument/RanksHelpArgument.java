package us.iorilabs.bukkit.ranks.argument;

import java.util.List;
import us.iorilabs.bukkit.ranks.RanksCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class RanksHelpArgument extends CommandArgument {

    private final RanksCommand parent;

    public RanksHelpArgument(RanksCommand parent) {
        super("help", null);
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        List<String> lines = parent.language().list("RANKS.USAGE");
        for (String line : lines) {
            source.sendMessage(line);
        }
    }
}
