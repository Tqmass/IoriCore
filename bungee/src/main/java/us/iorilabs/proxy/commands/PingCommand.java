package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.PlayerPresence;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class PingCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public PingCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "ping";
    }

    @Override
    public List<String> aliases() {
        return List.of("latency", "ms");
    }

    @Override
    public String permission() {
        String configured = proxy.configs().commands().permission("ping");
        if (configured == null || configured.isBlank() || configured.equalsIgnoreCase("none")) {
            return "";
        }

        return configured;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        String targetName;

        if (args.length >= 1) {
            if (!source.hasPermission(proxy.configs().commands().permission("ping_other"))) {
                source.sendMessage(proxy.language().raw("general.no_permission"));
                return;
            }

            targetName = args[0];
        } else if (source.isPlayer()) {
            targetName = source.name();
        } else {
            source.sendMessage(proxy.language().raw("ping.usage"));
            return;
        }

        Optional<ProxyPlayer> local = proxy.platform().findPlayerByName(targetName);
        if (local.isPresent()) {
            int ping = local.get().ping();
            source.sendMessage(
                proxy
                    .language()
                    .raw(
                        "ping.result",
                        Map.of(
                            "player",
                            local.get().name(),
                            "ping",
                            String.valueOf(Math.max(0, ping)),
                            "proxy",
                            proxy.configs().settings().proxyId()
                        )
                    )
            );
            return;
        }

        Optional<PlayerPresence> remote = proxy.presence().findByName(targetName);
        if (remote.isEmpty()) {
            source.sendMessage(proxy.language().raw("general.player_not_found", Map.of("player", targetName)));
            return;
        }

        source.sendMessage(
            proxy
                .language()
                .raw(
                    "ping.remote",
                    Map.of(
                        "player",
                        remote.get().name(),
                        "proxy",
                        remote.get().proxyId(),
                        "server",
                        remote.get().server() == null ? "?" : remote.get().server()
                    )
                )
        );
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 1 && source.hasPermission(proxy.configs().commands().permission("ping_other"))) {
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
