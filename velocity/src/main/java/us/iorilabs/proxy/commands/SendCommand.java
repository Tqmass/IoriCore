package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.platform.BackendServer;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.server.ServerRouter;
import us.iorilabs.proxy.utilities.TextNames;

public final class SendCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public SendCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "send";
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("send");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length == 0) {
            for (String line : proxy.language().rawList("transfer.send_usage")) {
                source.sendMessage(line);
            }
            return;
        }

        ServerRouter router = proxy.router();

        if (args.length == 1) {
            if (source.isConsole()) {
                source.sendMessage(proxy.language().raw("general.player_only"));
                return;
            }

            String server = args[0];
            Optional<BackendServer> target = router.resolveServer(server);
            if (target.isEmpty() || !target.get().online()) {
                source.sendMessage(proxy.language().raw("general.server_not_found", Map.of("server", server)));
                return;
            }

            UUID uuid = source.uuidOrNull();
            ProxyPlayer self = uuid == null ? null : proxy.platform().findPlayerByUuid(uuid).orElse(null);
            if (self != null && target.get().name().equalsIgnoreCase(self.currentServer())) {
                source.sendMessage(proxy.language().raw("transfer.already_connected"));
                return;
            }

            if (uuid == null || !router.send(uuid, target.get().name())) {
                source.sendMessage(
                    proxy
                        .language()
                        .raw("transfer.send_failed", Map.of("server", TextNames.displayServer(target.get().name())))
                );
                return;
            }

            source.sendMessage(
                proxy
                    .language()
                    .raw("transfer.sent_self", Map.of("server", TextNames.displayServer(target.get().name())))
            );
            return;
        }

        if (!source.hasPermission(proxy.configs().commands().permission("send_other")) && !source.isConsole()) {
            source.sendMessage(proxy.language().raw("general.no_permission"));
            return;
        }

        String playerName = args[0];
        String server = args[1];
        Optional<ProxyPlayer> local = proxy.platform().findPlayerByName(playerName);
        if (local.isEmpty()) {
            source.sendMessage(proxy.language().raw("general.player_not_found", Map.of("player", playerName)));
            return;
        }

        Optional<BackendServer> target = router.resolveServer(server);
        if (target.isEmpty() || !target.get().online()) {
            source.sendMessage(proxy.language().raw("general.server_not_found", Map.of("server", server)));
            return;
        }

        if (target.get().name().equalsIgnoreCase(local.get().currentServer())) {
            source.sendMessage(proxy.language().raw("transfer.already_connected"));
            return;
        }

        String displayServer = TextNames.displayServer(target.get().name());
        if (!router.send(local.get().uuid(), target.get().name())) {
            source.sendMessage(proxy.language().raw("transfer.send_failed", Map.of("server", displayServer)));
            return;
        }

        source.sendMessage(
            proxy.language().raw("transfer.sent_other", Map.of("player", local.get().name(), "server", displayServer))
        );
        for (String line : proxy
            .language()
            .rawList("transfer.sent_by", Map.of("by", source.name(), "server", displayServer))) {
            local.get().sendMessage(line);
        }

        if (proxy.staffAlerts() != null) {
            proxy.staffAlerts().send(local.get().name(), target.get().name(), source.name());
        }
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        List<String> out = new ArrayList<>();

        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

            if (source.hasPermission(proxy.configs().commands().permission("send_other")) || source.isConsole()) {
                for (ProxyPlayer player : proxy.platform().onlinePlayers()) {
                    if (player.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        out.add(player.name());
                    }
                }
            }

            for (BackendServer server : proxy.router().listServers()) {
                if (server.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(server.name());
                }
            }

            return out;
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);

            for (BackendServer server : proxy.router().listServers()) {
                if (server.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(server.name());
                }
            }
        }

        return out;
    }
}
