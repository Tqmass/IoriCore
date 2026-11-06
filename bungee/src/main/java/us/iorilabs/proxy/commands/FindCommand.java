package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.PlayerPresence;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class FindCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public FindCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "find";
    }

    @Override
    public List<String> aliases() {
        return List.of("whereis");
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("find");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("transfer.find_usage"));
            return;
        }

        Optional<PlayerPresence> target = proxy.router().locate(args[0]);
        if (target.isEmpty()) {
            source.sendMessage(proxy.language().raw("general.player_not_found", Map.of("player", args[0])));
            return;
        }

        PlayerPresence presence = target.get();
        String server = presence.server() == null || presence.server().isBlank() ? "?" : presence.server();
        source.sendMessage(
            proxy
                .language()
                .raw(
                    "transfer.find_result",
                    Map.of("player", presence.name(), "server", server, "proxy", presence.proxyId())
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
