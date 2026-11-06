package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class TransferCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public TransferCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "transfer";
    }

    @Override
    public List<String> aliases() {
        return List.of("migrate", "accounttransfer");
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("transfer");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (proxy.accountTransfer() == null) {
            source.sendMessage(proxy.language().raw("general.feature_disabled"));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(proxy.language().raw("account_transfer.usage"));
            return;
        }

        Optional<UUID> from = proxy.accountTransfer().resolveUuid(args[0]);
        Optional<UUID> to = proxy.accountTransfer().resolveUuid(args[1]);
        if (from.isEmpty()) {
            source.sendMessage(proxy.language().raw("general.player_not_found", Map.of("player", args[0])));
            return;
        }

        if (to.isEmpty()) {
            source.sendMessage(proxy.language().raw("general.player_not_found", Map.of("player", args[1])));
            return;
        }

        String fromName = proxy.accountTransfer().resolveName(from.get(), args[0]);
        String toName = proxy.accountTransfer().resolveName(to.get(), args[1]);
        Optional<String> error = proxy.accountTransfer().transfer(from.get(), fromName, to.get(), toName);
        if (error.isPresent()) {
            source.sendMessage(proxy.language().raw(error.get()));
            return;
        }

        source.sendMessage(proxy.language().raw("account_transfer.success", Map.of("from", fromName, "to", toName)));
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 1 && args.length != 2) return List.of();

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return proxy
            .platform()
            .onlinePlayers()
            .stream()
            .map(ProxyPlayer::name)
            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
            .toList();
    }
}
