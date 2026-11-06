package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.blacklist.BlacklistService;

public final class BlacklistCommand implements ProxyCommand {

    private final IoriProxy proxy;
    private final boolean unblacklist;

    public BlacklistCommand(IoriProxy proxy, boolean unblacklist) {
        this.proxy = proxy;
        this.unblacklist = unblacklist;
    }

    @Override
    public String name() {
        return unblacklist ? "unblacklist" : "blacklist";
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("blacklist");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        BlacklistService service = proxy.blacklist();
        if (!service.enabled()) {
            source.sendMessage(proxy.language().raw("general.feature_disabled"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw(unblacklist ? "blacklist.un_usage" : "blacklist.usage"));
            return;
        }

        List<String> tokens = new ArrayList<>();
        boolean silent = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase(service.silentFlag())) {
                silent = true;
            } else {
                tokens.add(arg);
            }
        }

        if (tokens.isEmpty()) {
            source.sendMessage(proxy.language().raw(unblacklist ? "blacklist.un_usage" : "blacklist.usage"));
            return;
        }

        String target = tokens.get(0);
        String reason = tokens.size() > 1 ? String.join(" ", tokens.subList(1, tokens.size())) : "No reason";

        if (unblacklist) {
            service.remove(target, reason, silent, source.name());
            source.sendMessage(proxy.language().raw("blacklist.removed", Map.of("player", target)));
        } else {
            service.add(target, reason, silent, source.name());
            source.sendMessage(proxy.language().raw("blacklist.added", Map.of("player", target, "reason", reason)));
        }
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length > 1) return List.of();

        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

        List<String> out = new ArrayList<>();
        proxy
            .platform()
            .onlinePlayers()
            .forEach(player -> {
                if (player.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(player.name());
                }
            });
        return out;
    }
}
