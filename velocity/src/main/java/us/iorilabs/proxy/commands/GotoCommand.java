package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.PlayerPresence;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class GotoCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public GotoCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "goto";
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("goto");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (source.isConsole()) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("transfer.goto_usage"));
            return;
        }

        Optional<PlayerPresence> target = proxy.router().locate(args[0]);
        if (target.isEmpty() || target.get().server() == null || target.get().server().isBlank()) {
            source.sendMessage(proxy.language().raw("general.player_not_found", Map.of("player", args[0])));
            return;
        }

        PlayerPresence presence = target.get();
        if (proxy.router().resolveServer(presence.server()).isEmpty()) {
            source.sendMessage(
                proxy
                    .language()
                    .raw(
                        "transfer.target_server_unavailable",
                        Map.of("player", presence.name(), "server", presence.server())
                    )
            );
            return;
        }

        boolean sameServer = proxy
            .platform()
            .findPlayerByUuid(source.uuidOrNull())
            .map(self -> presence.server().equalsIgnoreCase(self.currentServer()))
            .orElse(false);
        if (sameServer) {
            source.sendMessage(proxy.language().raw("transfer.already_connected"));
            return;
        }

        if (!proxy.router().send(source.uuidOrNull(), presence.server())) {
            source.sendMessage(proxy.language().raw("transfer.send_failed", Map.of("server", presence.server())));
            return;
        }

        source.sendMessage(
            proxy
                .language()
                .raw(
                    "transfer.goto_success",
                    Map.of(
                        "player",
                        presence.name(),
                        "server",
                        us.iorilabs.proxy.utilities.TextNames.displayServer(presence.server())
                    )
                )
        );
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length > 1) return List.of();

        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();

        for (ProxyPlayer player : proxy.platform().onlinePlayers()) {
            if (player.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(player.name());
            }
        }

        return out;
    }
}
