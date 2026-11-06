package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.report.ReportService;

public final class ReportCommand implements ProxyCommand {

    private final IoriProxy proxy;
    private final ReportService reportService;

    public ReportCommand(IoriProxy proxy, ReportService reportService) {
        this.proxy = proxy;
        this.reportService = reportService;
    }

    @Override
    public String name() {
        return "report";
    }

    @Override
    public String permission() {
        // Open command: send uses report_send; toggle title uses report_receive inside.
        return "";
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.isPlayer()) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return;
        }

        if (args.length >= 1 && "toggle".equalsIgnoreCase(args[0])) {
            handleToggle(source, args);
            return;
        }

        if (!source.hasPermission(proxy.configs().commands().permission("report_send"))) {
            source.sendMessage(proxy.language().raw("general.no_permission"));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(proxy.language().raw("report.usage"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        ProxyPlayer reporter = proxy.platform().findPlayerByUuid(uuid).orElse(null);
        if (reporter == null) return;

        String target = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (reason.isBlank()) {
            source.sendMessage(proxy.language().raw("report.usage"));
            return;
        }

        Optional<Long> cooldown = reportService.cooldownRemainingSeconds(uuid);
        if (cooldown.isPresent()) {
            source.sendMessage(
                proxy.language().raw("report.cooldown", Map.of("seconds", String.valueOf(cooldown.get())))
            );
            return;
        }

        Optional<String> error = reportService.create(reporter, target, reason, "");
        if (error.isPresent()) {
            if ("self".equals(error.get())) {
                source.sendMessage(proxy.language().raw("report.self"));
            }

            return;
        }

        source.sendMessage(proxy.language().raw("report.sent", Map.of("target", target)));
    }

    private void handleToggle(CommandSource source, String[] args) {
        if (!source.hasPermission(proxy.configs().commands().permission("report_receive"))) {
            source.sendMessage(proxy.language().raw("general.no_permission"));
            return;
        }

        if (args.length < 2 || !"title".equalsIgnoreCase(args[1])) {
            source.sendMessage(proxy.language().raw("report.toggle_usage"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        boolean muted = reportService.toggleTitleMute(uuid);
        source.sendMessage(proxy.language().raw(muted ? "report.title_muted" : "report.title_unmuted"));
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            if (
                source.hasPermission(proxy.configs().commands().permission("report_receive")) &&
                "toggle".startsWith(prefix)
            ) {
                out.add("toggle");
            }

            UUID self = source.uuidOrNull();
            proxy
                .platform()
                .onlinePlayers()
                .forEach(player -> {
                    if (self != null && self.equals(player.uuid())) return;
                    if (player.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        out.add(player.name());
                    }
                });
            return out;
        }

        if (
            args.length == 2 &&
            "toggle".equalsIgnoreCase(args[0]) &&
            source.hasPermission(proxy.configs().commands().permission("report_receive"))
        ) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if ("title".startsWith(prefix)) return List.of("title");
        }

        return List.of();
    }
}
