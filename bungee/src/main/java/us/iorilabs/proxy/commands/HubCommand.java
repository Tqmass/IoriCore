package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import us.iorilabs.proxy.IoriProxy;

public final class HubCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public HubCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "hub";
    }

    @Override
    public List<String> aliases() {
        return List.of("lobby");
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("hub");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (source.isConsole()) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return;
        }

        if (!proxy.configs().lobby().hubEnabled()) {
            source.sendMessage(proxy.language().raw("general.feature_disabled"));
            return;
        }

        Optional<String> lobby = proxy.router().pickLobby(source.uuidOrNull());
        if (lobby.isEmpty()) {
            source.sendMessage(proxy.language().raw("lobby.no_lobby"));
            return;
        }

        source.sendMessage(proxy.language().raw("lobby.sending"));

        if (!proxy.router().send(source.uuidOrNull(), lobby.get())) {
            source.sendMessage(proxy.language().raw("transfer.send_failed", Map.of("server", lobby.get())));
        }
    }
}
