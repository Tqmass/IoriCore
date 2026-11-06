package us.iorilabs.proxy.commands;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.PlayerPresence;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.DurationParser;
import us.iorilabs.proxy.utilities.TextNames;

public final class SeenCommand implements ProxyCommand {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(
        ZoneId.systemDefault()
    );

    private final IoriProxy proxy;

    public SeenCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "seen";
    }

    @Override
    public List<String> aliases() {
        return List.of("inspect", "info");
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("seen");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("seen.usage"));
            return;
        }

        String targetName = args[0];
        boolean admin = source.hasPermission(proxy.configs().commands().permission("seen-admin"));
        boolean showIp = admin || source.hasPermission(proxy.configs().commands().permission("seen-ipaddress"));
        boolean showCountry = admin || source.hasPermission(proxy.configs().commands().permission("seen-country"));

        Optional<ProxyPlayer> online = proxy.platform().findPlayerByName(targetName);
        UUID uuid;
        String name;
        String server = "";
        boolean isOnline;
        String ip = "—";

        if (online.isPresent()) {
            ProxyPlayer player = online.get();
            uuid = player.uuid();
            name = player.name();
            server = player.currentServer();
            isOnline = true;
            ip = player.ipAddress().orElse("—");
        } else {
            Optional<PlayerPresence> presence = proxy.presence().findByName(targetName);
            if (presence.isPresent()) {
                uuid = presence.get().uuid();
                name = presence.get().name();
                server = presence.get().server();
                isOnline = true;
            } else {
                source.sendMessage(proxy.language().raw("general.player_not_found", Map.of("player", targetName)));
                return;
            }

            if (proxy.playerData() != null) {
                ip = proxy.playerData().lastIp(uuid).orElse("—");
            }
        }

        String status = resolveStatus(uuid, name, isOnline);
        long firstJoin = proxy.playerData() == null ? 0L : proxy.playerData().firstJoinMillis(uuid);
        long lastJoin = proxy.playerData() == null ? 0L : proxy.playerData().lastJoinMillis(uuid);
        long playtime = proxy.playtime() == null ? 0L : proxy.playtime().totalMillis(uuid);
        String country = resolveCountry(ip);

        for (String line : proxy.language().rawList("seen.header", Map.of("player", name))) {
            source.sendMessage(line);
        }

        source.sendMessage(proxy.language().raw("seen.line_uuid", Map.of("uuid", uuid.toString())));

        if (showIp || showCountry) {
            String ipPart = showIp ? ip : "—";
            String countryPart = showCountry ? country : "—";
            source.sendMessage(
                proxy.language().raw("seen.line_ip_country", Map.of("ip", ipPart, "country", countryPart))
            );
        }

        source.sendMessage(proxy.language().raw("seen.line_name", Map.of("player", name)));
        source.sendMessage(proxy.language().raw("seen.line_status", Map.of("status", status)));

        String serverDisplay = TextNames.displayServer(server == null || server.isBlank() ? "?" : server);
        if (isOnline) {
            source.sendMessage(proxy.language().raw("seen.line_server_online", Map.of("server", serverDisplay)));
        } else {
            source.sendMessage(proxy.language().raw("seen.line_server_offline", Map.of("server", serverDisplay)));
        }

        source.sendMessage(proxy.language().raw("seen.line_first", Map.of("when", formatWhen(firstJoin))));
        source.sendMessage(proxy.language().raw("seen.line_last", Map.of("when", formatWhen(lastJoin))));
        source.sendMessage(proxy.language().raw("seen.line_playtime_label"));
        source.sendMessage(
            proxy.language().raw("seen.playtime_network", Map.of("time", DurationParser.format(playtime)))
        );

        if (proxy.playtime() != null) {
            for (Map.Entry<String, Long> entry : proxy.playtime().serverMillis(uuid).entrySet()) {
                source.sendMessage(
                    proxy
                        .language()
                        .raw(
                            "seen.server_entry",
                            Map.of(
                                "server",
                                TextNames.displayServer(entry.getKey()),
                                "time",
                                DurationParser.format(entry.getValue())
                            )
                        )
                );
            }
        }

        for (String line : proxy.language().rawList("seen.footer")) {
            source.sendMessage(line);
        }
    }

    private String resolveStatus(UUID uuid, String name, boolean isOnline) {
        if (proxy.blacklist() != null && proxy.blacklist().enabled() && proxy.blacklist().isBlacklisted(uuid, name)) {
            return proxy.language().raw("seen.status_blacklisted");
        }

        // LiteBans soft-hook could go here later.
        return isOnline ? proxy.language().raw("seen.status_online") : proxy.language().raw("seen.status_offline");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 1) return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);
        return proxy
            .platform()
            .onlinePlayers()
            .stream()
            .map(ProxyPlayer::name)
            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
            .toList();
    }

    private static String formatWhen(long epochMs) {
        if (epochMs <= 0L) return "—";

        return WHEN.format(Instant.ofEpochMilli(epochMs));
    }

    private static String resolveCountry(String ip) {
        if (ip == null || ip.isBlank() || "—".equals(ip) || ip.startsWith("127.") || "0.0.0.0".equals(ip)) {
            return "—";
        }

        return "—";
    }
}
