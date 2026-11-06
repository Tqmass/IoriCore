package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.staff.StaffGroups;

public final class StaffCommand implements ProxyCommand {

    private final IoriProxy proxy;
    private final StaffGroups staffGroups;

    public StaffCommand(IoriProxy proxy, StaffGroups staffGroups) {
        this.proxy = proxy;
        this.staffGroups = staffGroups;
    }

    @Override
    public String name() {
        return "staff";
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("staff_list");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length > 0 && !args[0].equalsIgnoreCase("list")) {
            for (String line : proxy.language().rawList("staff.usage")) {
                source.sendMessage(line);
            }
            return;
        }

        Map<String, List<ProxyPlayer>> grouped = new LinkedHashMap<>();

        for (Map.Entry<String, String> group : staffGroups.groups().entrySet()) {
            grouped.put(group.getKey(), new ArrayList<>());
        }

        grouped.put("staff", new ArrayList<>());

        for (ProxyPlayer player : proxy.platform().onlinePlayers()) {
            if (!staffGroups.isStaff(player)) continue;

            String rank = staffGroups.rankId(player);
            if (rank.isBlank()) {
                grouped.get("staff").add(player);
            } else if (grouped.containsKey(rank)) {
                grouped.get(rank).add(player);
            } else {
                grouped.get("staff").add(player);
            }
        }

        int total = grouped.values().stream().mapToInt(List::size).sum();
        if (total == 0) {
            source.sendMessage(proxy.language().raw("staff.list_empty"));
            return;
        }

        for (String line : proxy.language().rawList("staff.list_header", Map.of("count", String.valueOf(total)))) {
            source.sendMessage(line);
        }

        for (Map.Entry<String, List<ProxyPlayer>> entry : grouped.entrySet()) {
            for (ProxyPlayer player : entry.getValue()) {
                String rank = staffGroups.displayRank(player);
                String key = rank.isBlank() ? "staff.list_entry" : "staff.list_entry_rank";
                source.sendMessage(
                    proxy
                        .language()
                        .raw(
                            key,
                            Map.of(
                                "rank",
                                rank,
                                "player",
                                player.name(),
                                "server",
                                us.iorilabs.proxy.utilities.TextNames.displayServer(
                                    blankToQuestion(player.currentServer())
                                )
                            )
                        )
                );
            }
        }

        for (String line : proxy.language().rawList("staff.list_footer")) {
            source.sendMessage(line);
        }
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("list".startsWith(prefix)) return List.of("list");
        }

        return List.of();
    }

    private static String blankToQuestion(String server) {
        return server == null || server.isBlank() ? "?" : server;
    }
}
