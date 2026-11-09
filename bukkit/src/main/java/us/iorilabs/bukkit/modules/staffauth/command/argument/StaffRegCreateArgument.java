package us.iorilabs.bukkit.modules.staffauth.command.argument;

import java.util.List;
import us.iorilabs.bukkit.modules.staffauth.StaffRegService;
import us.iorilabs.bukkit.modules.staffauth.command.StaffRegCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class StaffRegCreateArgument extends CommandArgument {

    private final StaffRegCommand parent;

    public StaffRegCreateArgument(StaffRegCommand parent, StaffRegService service) {
        super("create", "royalctw.staffreg.create");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        parent.send(source, "STAFFREG.PROXY-ONLY");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        return List.of();
    }
}
