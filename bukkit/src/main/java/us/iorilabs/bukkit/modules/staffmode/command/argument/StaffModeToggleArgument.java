package us.iorilabs.bukkit.modules.staffmode.command.argument;

import java.util.Collections;
import java.util.List;
import us.iorilabs.bukkit.modules.staffmode.command.StaffModeCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class StaffModeToggleArgument extends CommandArgument {

    private final StaffModeCommand parent;

    public StaffModeToggleArgument(StaffModeCommand parent) {
        super("toggle", "ioricore.staffmode");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        parent.toggle(source);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
