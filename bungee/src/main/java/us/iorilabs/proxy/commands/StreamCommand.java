package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.stream.StreamService;

public final class StreamCommand implements ProxyCommand {

    private final IoriProxy proxy;
    private final StreamService streamService;
    private final String commandKey;

    public StreamCommand(IoriProxy proxy, StreamService streamService, String commandKey) {
        this.proxy = proxy;
        this.streamService = streamService;
        this.commandKey = commandKey;
    }

    @Override
    public String name() {
        return commandKey;
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission(commandKey);
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.isPlayer()) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("stream.usage"));
            return;
        }

        String url = args[0];
        if (!streamService.isValidStreamUrl(url)) {
            source.sendMessage(proxy.language().raw("stream.invalid_url"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        Optional<Long> remaining = streamService.cooldownRemainingSeconds(uuid);
        if (remaining.isPresent()) {
            source.sendMessage(
                proxy.language().raw("stream.cooldown", Map.of("seconds", String.valueOf(remaining.get())))
            );
            return;
        }

        ProxyPlayer player = proxy.platform().findPlayerByUuid(uuid).orElse(null);
        if (player == null) return;

        boolean video = "youtube".equalsIgnoreCase(commandKey);
        streamService.broadcast(player, url.trim(), video);
    }
}
