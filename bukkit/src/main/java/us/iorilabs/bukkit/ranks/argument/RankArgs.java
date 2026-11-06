package us.iorilabs.bukkit.ranks.argument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.ranks.Rank;
import us.iorilabs.bukkit.ranks.RanksCommand;
import us.iorilabs.bukkit.ranks.enums.RankEditResult;
import us.iorilabs.proxy.commands.CommandSource;

final class RankArgs {

    private RankArgs() {}

    static Rank requireRank(RanksCommand parent, CommandSource source, String token) {
        Rank rank = parent.ranks().resolve(token).orElse(null);
        if (rank == null) {
            source.sendMessage(parent.language().raw(source, "RANKS.UNKNOWN", "rank", token == null ? "" : token));
            return null;
        }
        return rank;
    }

    static void feedback(RanksCommand parent, CommandSource source, RankEditResult result, String... pairs) {
        String key;
        switch (result) {
            case OK:
                key = "RANKS.OK";
                break;
            case UNKNOWN_RANK:
                key = "RANKS.UNKNOWN";
                break;
            case UNKNOWN_PARENT:
                key = "RANKS.UNKNOWN-PARENT";
                break;
            case INVALID_NAME:
                key = "RANKS.INVALID-NAME";
                break;
            case INVALID_VALUE:
                key = "RANKS.INVALID-VALUE";
                break;
            case ALREADY_EXISTS:
                key = "RANKS.ALREADY-EXISTS";
                break;
            case NOT_PRESENT:
                key = "RANKS.NOT-PRESENT";
                break;
            case CYCLE:
                key = "RANKS.CYCLE";
                break;
            case DEFAULT_RANK:
                key = "RANKS.DEFAULT-PROTECTED";
                break;
            default:
                key = "RANKS.FAILED";
                break;
        }
        source.sendMessage(parent.language().raw(source, key, pairs));
    }

    static Map<String, String> map(String... pairs) {
        if (pairs == null || pairs.length < 2) return Collections.emptyMap();
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) out.put(pairs[i], pairs[i + 1]);
        return out;
    }

    static List<String> suggestRanks(RanksCommand parent, String prefix) {
        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Rank rank : parent.ranks().all()) {
            if (
                needle.isEmpty() ||
                rank.key().startsWith(needle) ||
                rank.name().toLowerCase(Locale.ROOT).startsWith(needle)
            ) {
                out.add(rank.key());
            }
        }
        return out;
    }

    static List<String> suggestPlayers(us.iorilabs.proxy.commands.CommandSource source, String prefix) {
        return us.iorilabs.bukkit.modules.core.EssentialsPlayers.suggestOnline(source, prefix);
    }

    static String join(String[] args, int from) {
        if (args == null || from >= args.length) return "";
        StringBuilder out = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) out.append(' ');
            out.append(args[i]);
        }
        return out.toString().trim();
    }
}
