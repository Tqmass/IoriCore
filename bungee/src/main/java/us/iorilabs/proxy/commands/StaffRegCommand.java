package us.iorilabs.proxy.commands;

import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.argument.StaffRegCreateArgument;

public final class StaffRegCommand extends CommandParent {

    private final IoriProxy proxy;

    public StaffRegCommand(IoriProxy proxy) {
        super(
            "staffreg",
            proxy.configs().commands().aliases("staffreg"),
            proxy.configs().staffReg().createPermission()
        );
        this.proxy = proxy;
        register(new StaffRegCreateArgument(proxy));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        source.sendMessage(proxy.language().raw("staffreg.create-usage"));
    }

    @Override
    protected String noPermissionMessage() {
        return proxy.language().raw("general.no_permission");
    }
}
