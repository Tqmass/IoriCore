package us.iorilabs.bukkit.modules.staffmode.command.argument;

import java.util.List;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.staffmode.command.StaffModeCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class StaffModeFreezeArgument extends CommandArgument {

    private final StaffModeCommand parent;

    public StaffModeFreezeArgument(StaffModeCommand parent) {
        super("freeze", "ioricore.staffmode.freeze");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        parent.freeze(source, args.length == 0 ? null : args[0]);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) return EssentialsPlayers.suggestOnline(source, args.length == 0 ? "" : args[0]);
        return java.util.Collections.emptyList();
    }
}
