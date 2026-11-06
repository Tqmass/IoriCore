package us.iorilabs.proxy.commands;

import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.argument.StaffAuthAdminArgument;
import us.iorilabs.proxy.commands.argument.StaffAuthApproveArgument;

public final class StaffAuthCommand extends CommandParent {

    private final IoriProxy proxy;

    public StaffAuthCommand(IoriProxy proxy) {
        super(
            "staffauth",
            proxy.configs().commands().aliases("staffauth"),
            proxy.configs().commands().permission("staffauth")
        );
        this.proxy = proxy;
        register(new StaffAuthApproveArgument(proxy));
        register(new StaffAuthAdminArgument(proxy, StaffAuthAdminArgument.Mode.LOCK));
        register(new StaffAuthAdminArgument(proxy, StaffAuthAdminArgument.Mode.UNLOCK));
        register(new StaffAuthAdminArgument(proxy, StaffAuthAdminArgument.Mode.SESSIONS));
        register(new StaffAuthAdminArgument(proxy, StaffAuthAdminArgument.Mode.RESET));
        register(new StaffAuthAdminArgument(proxy, StaffAuthAdminArgument.Mode.UNLINK));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        source.sendMessage(proxy.language().raw("staff-auth.usage"));
    }

    @Override
    protected String noPermissionMessage() {
        return proxy.language().raw("general.no_permission");
    }
}
