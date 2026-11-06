package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.platform.BackendServer;
import us.iorilabs.proxy.utilities.TextNames;

public final class ServersCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public ServersCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "servers";
    }

    @Override
    public List<String> aliases() {
        return List.of();
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("servers");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        List<BackendServer> servers = proxy.router().listServers();

        for (String line : proxy
            .language()
            .rawList("transfer.servers_header", Map.of("count", Integer.toString(servers.size())))) {
            source.sendMessage(line);
        }

        for (BackendServer server : servers) {
            String statusKey = server.online() ? "transfer.servers_status_online" : "transfer.servers_status_offline";
            source.sendMessage(
                proxy
                    .language()
                    .raw(
                        "transfer.servers_entry",
                        Map.of(
                            "server",
                            TextNames.displayServer(server.name()),
                            "status",
                            proxy.language().raw(statusKey),
                            "players",
                            Integer.toString(server.players())
                        )
                    )
            );
        }

        source.sendMessage(proxy.language().raw("transfer.servers_footer"));
    }
}
