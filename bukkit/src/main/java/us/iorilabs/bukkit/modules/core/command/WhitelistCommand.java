package us.iorilabs.bukkit.modules.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.proxy.commands.CommandSource;

public final class WhitelistCommand extends EssentialsCommand {

    private static final List<String> SUBCOMMANDS = Arrays.asList("list", "add", "remove", "on", "off");

    public WhitelistCommand(IoriCorePlugin plugin) {
        super(plugin, "whitelist", Collections.singletonList("wl"), "ioricore.whitelist");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.whitelist")) return;

        String action = args.length == 0 ? "list" : args[0].trim().toLowerCase(Locale.ROOT);
        switch (action) {
            case "list":
            case "ls":
                list(source);
                return;
            case "on":
            case "enable":
                Bukkit.setWhitelist(true);
                send(source, "WHITELIST.ENABLED");
                return;
            case "off":
            case "disable":
                Bukkit.setWhitelist(false);
                send(source, "WHITELIST.DISABLED");
                return;
            case "add":
                if (args.length < 2) {
                    sendUsage(source);
                    return;
                }
                add(source, args[1]);
                return;
            case "remove":
            case "rem":
            case "del":
                if (args.length < 2) {
                    sendUsage(source);
                    return;
                }
                remove(source, args[1]);
                return;
            default:
                sendUsage(source);
        }
    }

    private void list(CommandSource source) {
        Set<OfflinePlayer> players = Bukkit.getWhitelistedPlayers();
        boolean on = Bukkit.hasWhitelist();
        send(source, "WHITELIST.STATUS", Map.of("status", on ? "ON" : "OFF", "count", String.valueOf(players.size())));
        if (players.isEmpty()) {
            send(source, "WHITELIST.EMPTY");
            return;
        }
        String names = players
            .stream()
            .map(player -> player.getName() == null ? player.getUniqueId().toString() : player.getName())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.joining(", "));
        send(source, "WHITELIST.LIST", Map.of("players", names));
    }

    private void add(CommandSource source, String name) {
        if (!validName(name)) {
            send(source, "WHITELIST.INVALID-NAME", Map.of("player", name == null ? "" : name));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(name.trim());
        String display = target.getName() == null ? name.trim() : target.getName();
        if (target.isWhitelisted()) {
            send(source, "WHITELIST.ALREADY", Map.of("player", display));
            return;
        }
        target.setWhitelisted(true);
        send(source, "WHITELIST.ADDED", Map.of("player", display));
    }

    private void remove(CommandSource source, String name) {
        if (!validName(name)) {
            send(source, "WHITELIST.INVALID-NAME", Map.of("player", name == null ? "" : name));
            return;
        }
        OfflinePlayer target = resolveWhitelisted(name.trim());
        if (target == null) {
            send(source, "WHITELIST.NOT-LISTED", Map.of("player", name.trim()));
            return;
        }
        String display = target.getName() == null ? name.trim() : target.getName();
        target.setWhitelisted(false);
        send(source, "WHITELIST.REMOVED", Map.of("player", display));
    }

    private static OfflinePlayer resolveWhitelisted(String name) {
        OfflinePlayer exact = Bukkit.getOfflinePlayer(name);
        if (exact.isWhitelisted()) return exact;
        String needle = name.toLowerCase(Locale.ROOT);
        for (OfflinePlayer entry : Bukkit.getWhitelistedPlayers()) {
            String entryName = entry.getName();
            if (entryName != null && entryName.toLowerCase(Locale.ROOT).equals(needle)) return entry;
        }
        return null;
    }

    private static boolean validName(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') return false;
        }
        return true;
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.whitelist") && !source.hasPermission("ioricore.admin")) {
            return Collections.emptyList();
        }
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String option : SUBCOMMANDS) {
                if (option.startsWith(prefix)) out.add(option);
            }
            return out;
        }
        if (args.length == 2 && "add".equalsIgnoreCase(args[0])) {
            return EssentialsPlayers.suggestOnline(source, args[1]);
        }
        if (args.length == 2 && ("remove".equalsIgnoreCase(args[0]) || "rem".equalsIgnoreCase(args[0]))) {
            return suggestWhitelisted(args[1]);
        }
        return Collections.emptyList();
    }

    private static List<String> suggestWhitelisted(String prefix) {
        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (OfflinePlayer entry : Bukkit.getWhitelistedPlayers()) {
            String name = entry.getName();
            if (name == null || name.isEmpty()) continue;
            if (needle.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(needle)) out.add(name);
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "WHITELIST.USAGE");
    }
}
