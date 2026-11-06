package us.iorilabs.proxy.commands.argument;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.CommandSource;
import us.iorilabs.proxy.maintenance.MaintenanceService;
import us.iorilabs.proxy.maintenance.MaintenanceState;
import us.iorilabs.proxy.maintenance.MaintenanceType;
import us.iorilabs.proxy.utilities.DurationParser;

public final class MaintenanceOps {

    private final IoriProxy proxy;

    public MaintenanceOps(IoriProxy proxy) {
        this.proxy = proxy;
    }

    public MaintenanceService service() {
        return proxy.maintenance();
    }

    public void showStatus(CommandSource source) {
        List<MaintenanceState> active = service().activeStates();
        if (active.isEmpty()) {
            source.sendMessage(proxy.language().raw("maintenance.none"));
            return;
        }

        for (MaintenanceState state : active) {
            source.sendMessage(
                proxy
                    .language()
                    .raw(
                        "maintenance.status_entry",
                        Map.of(
                            "servidor",
                            state.scopeType() + ":" + state.scopeId(),
                            "scope",
                            state.scopeType() + ":" + state.scopeId(),
                            "reason",
                            state.reason()
                        )
                    )
            );
        }
    }

    public void sendHelp(CommandSource source) {
        for (String line : proxy.language().rawList("maintenance.usage")) {
            source.sendMessage(line);
        }
    }

    public void enable(CommandSource source, MaintenanceType scope, String id, String[] args) {
        ParsedTimes times = parseTimes(source, args);
        if (times == null) return;

        service().enable(scope, id, null, source.name(), times.durationMillis, times.countdownMillis);

        boolean global = scope == MaintenanceType.GLOBAL;
        String where = global ? "" : " en &f" + id;
        Map<String, String> ph = Map.of("scope", id, "servidor", id, "where", where);

        if (times.countdownMillis > 0) {
            source.sendMessage(
                proxy
                    .language()
                    .raw(
                        "maintenance.countdown_scheduled",
                        Map.of(
                            "scope",
                            id,
                            "servidor",
                            id,
                            "where",
                            where,
                            "time",
                            DurationParser.format(times.countdownMillis)
                        )
                    )
            );
            return;
        }

        source.sendMessage(proxy.language().raw("maintenance.enabled", ph));
    }

    public void handleDefault(CommandSource source, String[] args) {
        if (args.length == 0) {
            sendHelp(source);
            return;
        }

        String token = args[0].toLowerCase(Locale.ROOT);
        if (token.equals("help") || token.equals("?")) {
            sendHelp(source);
            return;
        }

        if (service().modes().containsKey(token)) {
            enable(source, MaintenanceType.MODE, token, copyFrom(args, 1));
            return;
        }

        if (proxy.platform().server(args[0]).isPresent()) {
            enable(source, MaintenanceType.SERVER, args[0], copyFrom(args, 1));
            return;
        }

        sendHelp(source);
    }

    public void handleWhitelist(CommandSource source, String[] args) {
        if (args.length < 1) {
            listWhitelist(source);
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add" -> {
                if (args.length < 2) {
                    source.sendMessage(proxy.language().raw("maintenance.whitelist_usage"));
                    return;
                }

                String entry = args[1].trim();
                service().addWhitelist(entry);
                for (String line : proxy.language().rawList("maintenance.whitelist_added", Map.of("entry", entry))) {
                    source.sendMessage(line);
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    source.sendMessage(proxy.language().raw("maintenance.whitelist_usage"));
                    return;
                }

                String entry = args[1].trim();
                if (!service().removeWhitelist(entry)) {
                    source.sendMessage(proxy.language().raw("maintenance.whitelist_not_found", Map.of("entry", entry)));
                    return;
                }

                for (String line : proxy.language().rawList("maintenance.whitelist_removed", Map.of("entry", entry))) {
                    source.sendMessage(line);
                }
            }
            case "list" -> listWhitelist(source);
            default -> source.sendMessage(proxy.language().raw("maintenance.whitelist_usage"));
        }
    }

    private void listWhitelist(CommandSource source) {
        for (String line : proxy.language().rawList("maintenance.whitelist_header")) {
            source.sendMessage(line);
        }

        for (String entry : service().whitelistEntries()) {
            source.sendMessage(proxy.language().raw("maintenance.whitelist_entry", Map.of("entry", entry)));
        }

        for (String line : proxy.language().rawList("maintenance.whitelist_footer")) {
            source.sendMessage(line);
        }
    }

    private ParsedTimes parseTimes(CommandSource source, String[] args) {
        long duration = 0L;
        long countdown = 0L;
        boolean durationSet = false;

        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (token.equalsIgnoreCase("countdown") || token.equalsIgnoreCase("-c")) {
                if (i + 1 >= args.length) {
                    source.sendMessage(proxy.language().raw("maintenance.invalid_duration"));
                    return null;
                }

                try {
                    countdown = DurationParser.parseMillis(args[++i]);
                } catch (IllegalArgumentException ex) {
                    source.sendMessage(proxy.language().raw("maintenance.invalid_duration"));
                    return null;
                }
                continue;
            }

            try {
                long parsed = DurationParser.parseMillis(token);
                if (durationSet) {
                    source.sendMessage(proxy.language().raw("maintenance.invalid_duration"));
                    return null;
                }
                duration = parsed;
                durationSet = true;
            } catch (IllegalArgumentException ex) {
                source.sendMessage(proxy.language().raw("maintenance.invalid_duration"));
                return null;
            }
        }

        return new ParsedTimes(duration, countdown);
    }

    private static String[] copyFrom(String[] args, int from) {
        if (from >= args.length) return new String[0];

        String[] out = new String[args.length - from];
        System.arraycopy(args, from, out, 0, out.length);
        return out;
    }

    private record ParsedTimes(long durationMillis, long countdownMillis) {}
}
