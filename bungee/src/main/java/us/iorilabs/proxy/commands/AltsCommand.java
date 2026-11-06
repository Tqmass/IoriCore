package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.alts.AltService;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class AltsCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public AltsCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "alts";
    }

    @Override
    public List<String> aliases() {
        return List.of("alt", "multicuenta", "accounts");
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("alts");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (proxy.alts() == null) {
            source.sendMessage(proxy.language().raw("general.feature_disabled"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("alts.usage"));
            return;
        }

        Optional<UUID> uuid = resolveTarget(args[0]);
        if (uuid.isEmpty()) {
            source.sendMessage(proxy.language().raw("general.player_not_found", Map.of("player", args[0])));
            return;
        }

        String name = proxy.platform().findPlayerByUuid(uuid.get()).map(ProxyPlayer::name).orElse(args[0]);
        List<AltService.AltAccount> alts = proxy.alts().altsOf(uuid.get());

        for (String line : proxy
            .language()
            .rawList("alts.header", Map.of("player", name, "count", String.valueOf(alts.size())))) {
            source.sendMessage(line);
        }

        if (alts.isEmpty()) {
            source.sendMessage(proxy.language().raw("alts.empty"));
        } else {
            for (AltService.AltAccount alt : alts) {
                source.sendMessage(
                    proxy
                        .language()
                        .raw(
                            "alts.entry",
                            Map.of("status", proxy.alts().statusLabel(alt.status()), "player", alt.name())
                        )
                );
            }
        }

        for (String line : proxy.language().rawList("alts.footer")) {
            source.sendMessage(line);
        }
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 1) return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);
        return proxy
            .platform()
            .onlinePlayers()
            .stream()
            .map(ProxyPlayer::name)
            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
            .toList();
    }

    private Optional<UUID> resolveTarget(String input) {
        if (proxy.accountTransfer() != null) {
            return proxy.accountTransfer().resolveUuid(input);
        }

        try {
            return Optional.of(UUID.fromString(input.trim()));
        } catch (IllegalArgumentException ignored) {}

        Optional<ProxyPlayer> online = proxy.platform().findPlayerByName(input);
        if (online.isPresent()) return Optional.of(online.get().uuid());

        if (proxy.friendService() != null) {
            return proxy.friendService().resolveUuidPublic(input);
        }

        return Optional.empty();
    }
}
