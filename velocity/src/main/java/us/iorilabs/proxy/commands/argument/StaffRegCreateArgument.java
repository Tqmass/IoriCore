package us.iorilabs.proxy.commands.argument;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;
import us.iorilabs.proxy.config.StaffRegConfig;

public final class StaffRegCreateArgument extends CommandArgument {

    private final IoriProxy proxy;

    public StaffRegCreateArgument(IoriProxy proxy) {
        super("create", null);
        this.proxy = proxy;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (proxy.staffReg() == null) {
            source.sendMessage(proxy.language().raw("general.feature_disabled"));
            return;
        }

        StaffRegConfig cfg = proxy.configs().staffReg();
        String perm = cfg.createPermission();
        if (!source.hasPermission(perm) && !source.hasPermission("ioricore.staffreg.create")) {
            source.sendMessage(proxy.language().raw("general.no_permission"));
            return;
        }

        if (args.length < 3) {
            source.sendMessage(proxy.language().raw("staffreg.create-usage"));
            return;
        }

        String nick = args[1].trim();
        String password = args[2];
        if (!nick.matches("[A-Za-z0-9_]{3,16}")) {
            source.sendMessage(proxy.language().raw("staffreg.invalid-nick"));
            return;
        }

        UUID actor = source.uuidOrNull();
        proxy
            .staffReg()
            .create(nick, password, actor)
            .thenAccept(result ->
                proxy
                    .platform()
                    .asyncExecutor()
                    .execute(() -> {
                        if (result.success()) {
                            source.sendMessage(
                                proxy
                                    .language()
                                    .raw(
                                        result.messageKey(),
                                        Map.of(
                                            "player",
                                            result.username() == null ? nick : result.username(),
                                            "uuid",
                                            result.uuid() == null ? "?" : result.uuid().toString()
                                        )
                                    )
                            );
                        } else {
                            source.sendMessage(proxy.language().raw(result.messageKey()));
                        }
                    })
            );
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return proxy
                .platform()
                .onlinePlayers()
                .stream()
                .map(player -> player.name())
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .limit(20)
                .toList();
        }
        return java.util.List.of();
    }
}
