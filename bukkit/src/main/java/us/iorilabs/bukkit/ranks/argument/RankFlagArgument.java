package us.iorilabs.bukkit.ranks.argument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import us.iorilabs.bukkit.ranks.Rank;
import us.iorilabs.bukkit.ranks.RanksCommand;
import us.iorilabs.bukkit.ranks.enums.RankEditResult;
import us.iorilabs.bukkit.ranks.enums.RankFlag;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class RankFlagArgument extends CommandArgument {

    private final RanksCommand parent;

    public RankFlagArgument(RanksCommand parent) {
        super("flag", "ioricore.ranks.edit");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 2) {
            source.sendMessage(parent.language().raw(source, "RANKS.FLAG-USAGE"));
            return;
        }
        RankFlag flag = RankFlag.from(args[1]);
        if (flag == null) {
            source.sendMessage(parent.language().raw(source, "RANKS.UNKNOWN-FLAG", "flag", args[1]));
            return;
        }
        Rank rank = RankArgs.requireRank(parent, source, args[0]);
        if (rank == null) return;

        boolean value = args.length < 3 ? !rank.has(flag) : parseBool(args[2]);
        RankEditResult result = parent.ranks().setFlag(args[0], flag, value);
        if (result == RankEditResult.OK) {
            source.sendMessage(
                parent
                    .language()
                    .raw(source, "RANKS.FLAG-SET", "rank", args[0], "flag", flag.name(), "value", String.valueOf(value))
            );
            return;
        }
        RankArgs.feedback(parent, source, result, "rank", args[0]);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) return RankArgs.suggestRanks(parent, args.length == 0 ? "" : args[0]);
        if (args.length == 2) {
            String needle = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (RankFlag flag : RankFlag.values()) {
                if (flag.name().toLowerCase(Locale.ROOT).startsWith(needle)) out.add(flag.name());
            }
            return out;
        }
        if (args.length == 3) {
            List<String> out = new ArrayList<>();
            out.add("true");
            out.add("false");
            return out;
        }
        return Collections.emptyList();
    }

    private static boolean parseBool(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return value.equals("true") || value.equals("yes") || value.equals("on") || value.equals("1");
    }
}
