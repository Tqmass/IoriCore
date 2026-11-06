package us.iorilabs.proxy.commands.argument;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class StaffAuthApproveArgument extends CommandArgument {

    private final IoriProxy proxy;

    public StaffAuthApproveArgument(IoriProxy proxy) {
        super("approve", proxy.configs().commands().permission("staffauth"));
        this.proxy = proxy;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (proxy.staffAuth() == null) {
            source.sendMessage(proxy.language().raw("general.feature_disabled"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("staff-auth.usage"));
            return;
        }

        String token = args[0].trim();
        if (token.length() < 6 || token.length() > 64 || !token.matches("[A-Za-z0-9_-]+")) {
            source.sendMessage(proxy.language().raw("staff-auth.invalid-token"));
            return;
        }

        UUID actorUuid = source.uuidOrNull();
        if (proxy.staffAuth().ownsToken(token, actorUuid)) {
            source.sendMessage(proxy.language().raw("staff-auth.self-approve"));
            return;
        }

        Optional<String> approved = proxy.staffAuth().approve(token, source.name(), actorUuid);
        if (approved.isEmpty()) {
            source.sendMessage(proxy.language().raw("staff-auth.unknown-token"));
            return;
        }

        source.sendMessage(proxy.language().raw("staff-auth.approve-ok", Map.of("uuid", approved.get())));
    }
}
