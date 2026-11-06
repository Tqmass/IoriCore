package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.platform.BackendServer;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.PageUtil;
import us.iorilabs.proxy.utilities.TextNames;

public final class AnalyticsCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public AnalyticsCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "analytics";
    }

    @Override
    public List<String> aliases() {
        return List.of();
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("analytics");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length >= 1 && "versions".equalsIgnoreCase(args[0])) {
            sendVersions(source, PageUtil.parsePage(args, 1));
            return;
        }

        int online = proxy.platform().onlinePlayers().size();
        long joins = proxy.analytics() == null ? 0L : proxy.analytics().joins();
        int peak = proxy.analytics() == null ? online : proxy.analytics().peakOnline();
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
        long maxMb = rt.maxMemory() / (1024L * 1024L);
        double ramPct = maxMb <= 0 ? 0 : (usedMb * 100.0) / maxMb;
        String ramColor = ramPct >= 90 ? "&c" : ramPct >= 75 ? "&e" : "&a";

        Map<String, String> ph = Map.of(
            "proxy",
            proxy.configs().settings().proxyId(),
            "joins",
            String.valueOf(joins),
            "peak",
            String.valueOf(peak),
            "online",
            String.valueOf(online),
            "ram_used",
            String.valueOf(usedMb),
            "ram_max",
            String.valueOf(maxMb),
            "ram_pct",
            String.format(Locale.ROOT, "%.0f", ramPct),
            "ram_color",
            ramColor
        );

        for (String line : proxy.language().rawList("analytics.summary", ph)) {
            source.sendMessage(line.replace("<ram_color>", ramColor));
        }

        source.sendMessage(proxy.language().raw("analytics.servers_label"));
        List<BackendServer> servers = new ArrayList<>(proxy.platform().registeredServers());
        servers.sort(Comparator.comparing(s -> s.name().toLowerCase(Locale.ROOT)));

        int serverPage = PageUtil.parsePage(args, 0);
        int serverPages = PageUtil.totalPages(Math.max(servers.size(), 1), PageUtil.DEFAULT_PAGE_SIZE);
        int safeServerPage = PageUtil.clampPage(serverPage, serverPages);

        for (BackendServer server : PageUtil.slice(servers, safeServerPage, PageUtil.DEFAULT_PAGE_SIZE)) {
            long count = proxy
                .platform()
                .onlinePlayers()
                .stream()
                .map(ProxyPlayer::currentServer)
                .filter(n -> n != null && n.equalsIgnoreCase(server.name()))
                .count();
            source.sendMessage(
                proxy
                    .language()
                    .raw(
                        "analytics.server_entry",
                        Map.of(
                            "server",
                            TextNames.displayServer(server.name()),
                            "online",
                            String.valueOf(count),
                            "status",
                            server.online()
                                ? proxy.language().raw("transfer.servers_status_online")
                                : proxy.language().raw("transfer.servers_status_offline")
                        )
                    )
            );
        }

        for (String line : proxy.language().rawList("analytics.footer")) {
            source.sendMessage(line);
        }

        if (servers.size() > PageUtil.DEFAULT_PAGE_SIZE) {
            source.sendMessage(
                PageUtil.navBar(
                    "/analytics",
                    safeServerPage,
                    serverPages,
                    proxy
                        .language()
                        .raw(
                            "general.page_label",
                            Map.of("page", String.valueOf(safeServerPage), "pages", String.valueOf(serverPages))
                        ),
                    proxy.language().raw("general.help_prev_hover"),
                    proxy.language().raw("general.help_next_hover")
                )
            );
        }

        source.sendMessage(proxy.language().raw("analytics.versions_hint"));
    }

    private void sendVersions(CommandSource source, int page) {
        Map<String, Integer> byBrand = new HashMap<>();
        List<ProxyPlayer> players = new ArrayList<>(proxy.platform().onlinePlayers());
        players.sort(Comparator.comparing(p -> p.name().toLowerCase(Locale.ROOT)));

        int totalPages = PageUtil.totalPages(Math.max(players.size(), 1), PageUtil.DEFAULT_PAGE_SIZE);
        int safePage = PageUtil.clampPage(page, totalPages);

        for (String line : proxy
            .language()
            .rawList(
                "analytics.versions_header",
                Map.of(
                    "count",
                    String.valueOf(players.size()),
                    "page",
                    String.valueOf(safePage),
                    "pages",
                    String.valueOf(totalPages)
                )
            )) {
            source.sendMessage(line);
        }

        for (ProxyPlayer player : players) {
            byBrand.merge(displayBrand(player.clientBrand().orElse(null)), 1, Integer::sum);
        }

        for (ProxyPlayer player : PageUtil.slice(players, safePage, PageUtil.DEFAULT_PAGE_SIZE)) {
            source.sendMessage(
                proxy
                    .language()
                    .raw(
                        "analytics.versions_entry",
                        Map.of(
                            "player",
                            player.name(),
                            "brand",
                            displayBrand(player.clientBrand().orElse(null)),
                            "server",
                            TextNames.displayServer(player.currentServer())
                        )
                    )
            );
        }

        source.sendMessage(proxy.language().raw("analytics.versions_breakdown"));
        byBrand
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
            .forEach(entry ->
                source.sendMessage(
                    proxy
                        .language()
                        .raw(
                            "analytics.versions_bucket",
                            Map.of("brand", entry.getKey(), "count", String.valueOf(entry.getValue()))
                        )
                )
            );

        for (String line : proxy.language().rawList("analytics.footer")) {
            source.sendMessage(line);
        }

        if (players.size() > PageUtil.DEFAULT_PAGE_SIZE) {
            source.sendMessage(
                PageUtil.navBar(
                    "/analytics versions",
                    safePage,
                    totalPages,
                    proxy
                        .language()
                        .raw(
                            "general.page_label",
                            Map.of("page", String.valueOf(safePage), "pages", String.valueOf(totalPages))
                        ),
                    proxy.language().raw("general.help_prev_hover"),
                    proxy.language().raw("general.help_next_hover")
                )
            );
        }
    }

    static String displayBrand(String brand) {
        if (brand == null || brand.isBlank()) return "vanilla";

        String raw = brand.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("lunar")) return "lunar-client";
        if (lower.contains("badlion")) return "badlion";
        if (lower.contains("feather")) return "feather";
        if (lower.contains("labymod")) return "labymod";
        if (lower.contains("forge")) return "forge";
        if (lower.contains("fabric")) return "fabric";
        if (lower.contains("quilt")) return "quilt";
        if (lower.contains("neoforge")) return "neoforge";
        if (lower.equals("vanilla") || lower.contains("vanilla")) return "vanilla";
        return raw.replace(' ', '-').toLowerCase(Locale.ROOT);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            if ("versions".startsWith(prefix)) out.add("versions");
            return out;
        }
        return List.of();
    }
}
