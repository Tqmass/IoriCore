package us.iorilabs.proxy.commands;

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

public final class PlaytimeCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public PlaytimeCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "playtime";
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("playtime");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (proxy.playtime() == null) {
            source.sendMessage(proxy.language().raw("general.feature_disabled"));
            return;
        }

        String targetName;

        if (args.length >= 1) {
            String otherPerm = proxy.configs().commands().permission("playtime-other");
            if (!source.hasPermission(otherPerm) && !source.hasPermission("ioriproxy.playtime.others")) {
                source.sendMessage(proxy.language().raw("general.no_permission"));
                return;
            }

            targetName = args[0];
        } else if (source.isPlayer()) {
            targetName = source.name();
        } else {
            source.sendMessage(proxy.language().raw("playtime.usage"));
            return;
        }

        UUID uuid;
        String displayName;
        Optional<ProxyPlayer> local = proxy.platform().findPlayerByName(targetName);
        if (local.isPresent()) {
            uuid = local.get().uuid();
            displayName = local.get().name();
        } else {
            Optional<PlayerPresence> remote = proxy.presence().findByName(targetName);
            if (remote.isEmpty()) {
                source.sendMessage(proxy.language().raw("general.player_not_found", Map.of("player", targetName)));
                return;
            }

            uuid = remote.get().uuid();
            displayName = remote.get().name();
        }

        long total = proxy.playtime().totalMillis(uuid);
        Map<String, Long> servers = proxy.playtime().serverMillis(uuid);

        for (String line : proxy
            .language()
            .rawList("playtime.result_header", Map.of("player", displayName, "total", DurationParser.format(total)))) {
            source.sendMessage(line);
        }

        if (servers.isEmpty()) {
            source.sendMessage(proxy.language().raw("playtime.servers_empty"));
        } else {
            source.sendMessage(proxy.language().raw("playtime.servers_label"));
            for (Map.Entry<String, Long> entry : servers.entrySet()) {
                source.sendMessage(
                    proxy
                        .language()
                        .raw(
                            "playtime.server_entry",
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

        for (String line : proxy.language().rawList("playtime.result_footer")) {
            source.sendMessage(line);
        }
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        String otherPerm = proxy.configs().commands().permission("playtime-other");
        if (
            args.length == 1 && (source.hasPermission(otherPerm) || source.hasPermission("ioriproxy.playtime.others"))
        ) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return proxy
                .platform()
                .onlinePlayers()
                .stream()
                .map(ProxyPlayer::name)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
        }

        return List.of();
    }
}
