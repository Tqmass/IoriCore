package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.platform.BackendServer;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.TextNames;

public final class GlistCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public GlistCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "glist";
    }

    @Override
    public List<String> aliases() {
        return List.of("gcount");
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("glist");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        List<BackendServer> servers = new ArrayList<>(proxy.platform().registeredServers());
        servers.sort(Comparator.comparing(s -> s.name().toLowerCase(Locale.ROOT)));

        var online = proxy.platform().onlinePlayers();
        int total =
            proxy.vanishPresence() == null ? online.size() : proxy.vanishPresence().visibleCount(source, online);
        for (String line : proxy.language().rawList("glist.header", Map.of("total", String.valueOf(total)))) {
            source.sendMessage(line);
        }

        for (BackendServer server : servers) {
            long count = online
                .stream()
                .filter(player -> proxy.vanishPresence() == null || proxy.vanishPresence().visible(source, player))
                .map(ProxyPlayer::currentServer)
                .filter(name -> name != null && name.equalsIgnoreCase(server.name()))
                .count();
            source.sendMessage(
                proxy
                    .language()
                    .raw(
                        "glist.entry",
                        Map.of("server", TextNames.displayServer(server.name()), "count", String.valueOf(count))
                    )
            );
        }

        for (String line : proxy.language().rawList("glist.footer", Map.of("total", String.valueOf(total)))) {
            source.sendMessage(line);
        }
    }
}
