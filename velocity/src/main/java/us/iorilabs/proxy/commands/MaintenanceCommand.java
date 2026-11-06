package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.List;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.argument.LambdaCommandArgument;
import us.iorilabs.proxy.commands.argument.MaintenanceOps;
import us.iorilabs.proxy.maintenance.MaintenanceType;

public final class MaintenanceCommand extends CommandParent {

    private final IoriProxy proxy;
    private final MaintenanceOps ops;

    public MaintenanceCommand(IoriProxy proxy) {
        super(
            "maintenance",
            proxy.configs().commands().aliases("maintenance"),
            proxy.configs().commands().permission("maintenance")
        );
        this.proxy = proxy;
        this.ops = new MaintenanceOps(proxy);
        register(
            LambdaCommandArgument.of("on", (source, args) -> ops.enable(source, MaintenanceType.GLOBAL, "global", args))
        );
        register(
            LambdaCommandArgument.of("off", (source, args) ->
                ops.service().disable(MaintenanceType.GLOBAL, "global", source.name())
            )
        );
        register(LambdaCommandArgument.of("list", (source, args) -> ops.showStatus(source)));
        register(LambdaCommandArgument.of("status", (source, args) -> ops.showStatus(source)));
        register(LambdaCommandArgument.of("help", (source, args) -> ops.sendHelp(source)));
        register(LambdaCommandArgument.of("whitelist", (source, args) -> ops.handleWhitelist(source, args)));
        register(
            LambdaCommandArgument.of("reload", (source, args) -> {
                proxy.reload();
                source.sendMessage(proxy.language().raw("general.reload_success"));
            })
        );
        setDefaultArgument(LambdaCommandArgument.of("default", (source, args) -> ops.handleDefault(source, args)));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        ops.sendHelp(source);
    }

    @Override
    protected String noPermissionMessage() {
        return proxy.language().raw("general.no_permission");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        List<String> base = super.suggest(source, label, args);

        if (args.length <= 1) {
            List<String> out = new ArrayList<>(base);
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            proxy
                .maintenance()
                .modes()
                .keySet()
                .stream()
                .filter(m -> m.startsWith(prefix))
                .forEach(out::add);
            proxy
                .platform()
                .registeredServers()
                .forEach(s -> {
                    if (s.name().toLowerCase().startsWith(prefix)) {
                        out.add(s.name());
                    }
                });
            return out;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("whitelist")) {
            String prefix = args[1].toLowerCase();
            List<String> out = new ArrayList<>();
            for (String opt : List.of("add", "remove", "list")) {
                if (opt.startsWith(prefix)) out.add(opt);
            }
            return out;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("whitelist")) {
            String action = args[1].toLowerCase();
            if (action.equals("add") || action.equals("remove")) {
                String prefix = args[2].toLowerCase();
                List<String> out = new ArrayList<>();
                proxy
                    .platform()
                    .onlinePlayers()
                    .forEach(p -> {
                        if (p.name().toLowerCase().startsWith(prefix)) out.add(p.name());
                    });
                if (action.equals("remove")) {
                    proxy
                        .maintenance()
                        .whitelistEntries()
                        .stream()
                        .filter(e -> e.toLowerCase().startsWith(prefix))
                        .forEach(out::add);
                }
                return out;
            }
        }

        if (args.length >= 2) {
            String first = args[0].toLowerCase();
            boolean timedTarget =
                first.equals("on") ||
                proxy.maintenance().modes().containsKey(first) ||
                proxy.platform().server(args[0]).isPresent();
            if (timedTarget) {
                String last = args[args.length - 1].toLowerCase();
                String prev = args.length >= 3 ? args[args.length - 2].toLowerCase() : "";
                List<String> out = new ArrayList<>();
                if (prev.equals("countdown") || prev.equals("-c")) {
                    for (String dur : List.of("10s", "30s", "1m", "5m", "15m")) {
                        if (dur.startsWith(last)) out.add(dur);
                    }
                    return out;
                }
                if ("countdown".startsWith(last) || "-c".startsWith(last)) {
                    if ("countdown".startsWith(last)) out.add("countdown");
                    if ("-c".startsWith(last)) out.add("-c");
                }
                for (String dur : List.of("15m", "30m", "1h", "2h", "6h", "1d")) {
                    if (dur.startsWith(last)) out.add(dur);
                }
                return out;
            }
        }

        return base;
    }
}
