package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.discord.link.DiscordLinkService;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class SyncCommand implements ProxyCommand {

    private final IoriProxy proxy;
    private final DiscordLinkService links;

    public SyncCommand(IoriProxy proxy, DiscordLinkService links) {
        this.proxy = proxy;
        this.links = links;
    }

    @Override
    public String name() {
        return "sync";
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("sync");
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

        long wait = links.cooldownRemaining(player.uuid());
        if (wait > 0L) {
            player.sendMessage(
                proxy.language().rawFor(player, "sync.cooldown", Map.of("seconds", String.valueOf(wait)))
            );
            return;
        }

        proxy
            .platform()
            .asyncExecutor()
            .execute(() -> {
                if (links.discordId(player.uuid()).isPresent()) {
                    player.sendMessage(proxy.language().rawFor(player, "sync.already_linked"));
                    return;
                }

                links.requestCode(player, code -> {
                    if (code.isEmpty()) {
                        player.sendMessage(proxy.language().rawFor(player, "sync.unavailable"));
                        return;
                    }

                    Map<String, String> ph = Map.of(
                        "code",
                        code.get(),
                        "minutes",
                        String.valueOf(Math.max(1, links.codeTtlSeconds() / 60))
                    );
                    for (String line : proxy.language().rawList("sync.code", ph, player.protocolVersion())) {
                        player.sendMessage(line);
                    }
                });
            });
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        return List.of();
    }
}
