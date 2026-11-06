package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.argument.LambdaCommandArgument;
import us.iorilabs.proxy.report.Report;
import us.iorilabs.proxy.report.ReportService;

public final class ReportsCommand extends CommandParent {

    private final IoriProxy proxy;
    private final ReportService reportService;

    public ReportsCommand(IoriProxy proxy, ReportService reportService) {
        super("reports", List.of(), proxy.configs().commands().permission("report_receive"));
        this.proxy = proxy;
        this.reportService = reportService;
        register(
            new LambdaCommandArgument(
                "resolve",
                proxy.configs().commands().permission("report_resolve"),
                (source, args) -> {
                    if (args.length < 1) {
                        source.sendMessage(proxy.language().raw("report.resolve_usage"));
                        return;
                    }

                    String id = args[0];
                    if (!reportService.resolve(id, source.name())) {
                        source.sendMessage(proxy.language().raw("helpop.not_found", Map.of("id", id)));
                        return;
                    }

                    source.sendMessage(
                        proxy
                            .language()
                            .raw(
                                "report.resolved",
                                Map.of(
                                    "id",
                                    id,
                                    "target",
                                    reportService.find(id).map(Report::targetName).orElse("?"),
                                    "staff",
                                    source.name()
                                )
                            )
                    );
                }
            )
        );
        setDefaultArgument(
            new LambdaCommandArgument(
                "list",
                proxy.configs().commands().permission("report_receive"),
                (source, args) -> {
                    List<Report> pending = reportService.pendingReports();
                    for (String line : proxy
                        .language()
                        .rawList("report.list_header", Map.of("count", String.valueOf(pending.size())))) {
                        source.sendMessage(line);
                    }

                    for (Report report : pending) {
                        source.sendMessage(
                            proxy
                                .language()
                                .raw(
                                    "report.list_entry",
                                    Map.of(
                                        "id",
                                        report.id(),
                                        "target",
                                        report.targetName(),
                                        "reason",
                                        report.reason(),
                                        "reporter",
                                        report.reporterName(),
                                        "server",
                                        report.server()
                                    )
                                )
                        );
                    }

                    for (String line : proxy.language().rawList("report.list_footer")) {
                        source.sendMessage(line);
                    }
                }
            )
        );
    }

    @Override
    protected void sendUsage(CommandSource source) {
        source.sendMessage(proxy.language().raw("report.reports_usage"));
    }

    @Override
    protected String noPermissionMessage() {
        return proxy.language().raw("general.no_permission");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("resolve".startsWith(prefix)) return List.of("resolve");
            if ("list".startsWith(prefix)) return List.of("list");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("resolve")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return reportService
                .pendingReports()
                .stream()
                .map(Report::id)
                .filter(id -> id.startsWith(prefix))
                .toList();
        }

        return super.suggest(source, label, args);
    }
}
