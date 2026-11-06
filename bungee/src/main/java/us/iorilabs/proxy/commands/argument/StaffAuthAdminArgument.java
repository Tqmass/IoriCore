package us.iorilabs.proxy.commands.argument;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;
import us.iorilabs.staffreg.StaffSession;

public final class StaffAuthAdminArgument extends CommandArgument {

    private final IoriProxy proxy;
    private final Mode mode;

    public StaffAuthAdminArgument(IoriProxy proxy, Mode mode) {
        super(mode.label, proxy.configs().commands().permission("staffauth"));
        this.proxy = proxy;
        this.mode = mode;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (proxy.staffReg() == null) {
            source.sendMessage(proxy.language().raw("general.feature_disabled"));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(proxy.language().raw("staff-auth.usage"));
            return;
        }

        UUID actor = source.uuidOrNull();
        UUID target = parseUuid(args[1]);
        if (target == null) {
            source.sendMessage(proxy.language().raw("staffreg.invalid-target"));
            return;
        }

        switch (mode) {
            case LOCK -> proxy
                .staffReg()
                .lock(target, actor)
                .thenAccept(ok -> send(source, ok ? "staffreg.lock-ok" : "staffreg.not-found", target));
            case UNLOCK -> proxy
                .staffReg()
                .unlock(target, actor)
                .thenAccept(ok -> send(source, ok ? "staffreg.unlock-ok" : "staffreg.not-found", target));
            case SESSIONS -> proxy
                .staffReg()
                .listSessions(target)
                .thenAccept(sessions -> {
                    proxy
                        .platform()
                        .asyncExecutor()
                        .execute(() -> {
                            source.sendMessage(
                                proxy
                                    .language()
                                    .raw(
                                        "staffreg.sessions-header",
                                        Map.of("uuid", target.toString(), "count", String.valueOf(sessions.size()))
                                    )
                            );
                            for (StaffSession session : sessions) {
                                source.sendMessage(
                                    proxy
                                        .language()
                                        .raw(
                                            "staffreg.sessions-line",
                                            Map.of(
                                                "id",
                                                session.sessionId() == null ? "?" : session.sessionId().toString(),
                                                "proxy",
                                                session.proxyId() == null ? "?" : session.proxyId()
                                            )
                                        )
                                );
                            }
                        });
                });
            case RESET -> proxy
                .staffReg()
                .resetAccount(target, actor, args.length > 2 ? args[2] : null)
                .thenAccept(stub ->
                    proxy
                        .platform()
                        .asyncExecutor()
                        .execute(() -> source.sendMessage(proxy.language().raw(stub.messageKey())))
                );
            case UNLINK -> proxy
                .staffReg()
                .unlinkDiscord(target, actor, args.length > 2 ? args[2] : null)
                .thenAccept(stub ->
                    proxy
                        .platform()
                        .asyncExecutor()
                        .execute(() -> source.sendMessage(proxy.language().raw(stub.messageKey())))
                );
        }
    }

    private void send(CommandSource source, String key, UUID target) {
        proxy
            .platform()
            .asyncExecutor()
            .execute(() -> source.sendMessage(proxy.language().raw(key, Map.of("uuid", target.toString()))));
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public enum Mode {
        LOCK("lock"),
        UNLOCK("unlock"),
        SESSIONS("sessions"),
        RESET("reset"),
        UNLINK("unlink");

        private final String label;

        Mode(String label) {
            this.label = label;
        }
    }
}
