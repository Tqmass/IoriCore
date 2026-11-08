package us.iorilabs.proxy.commands;

import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.discord.link.DiscordLinkService;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class UnsyncCommand implements ProxyCommand {

    private final IoriProxy proxy;
    private final DiscordLinkService links;

    public UnsyncCommand(IoriProxy proxy, DiscordLinkService links) {
        this.proxy = proxy;
        this.links = links;
    }

    @Override
    public String name() {
        return "unsync";
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("unsync");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        ProxyPlayer player =
            source.uuidOrNull() == null ? null : proxy.platform().findPlayerByUuid(source.uuidOrNull()).orElse(null);
        if (player == null) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return;
        }
        if (!links.available()) {
            player.sendMessage(proxy.language().rawFor(player, "sync.unavailable"));
            return;
        }

        links.unlink(player.uuid(), removed ->
            player.sendMessage(proxy.language().rawFor(player, removed ? "sync.unlinked" : "sync.not_linked"))
        );
    }
}
