package us.iorilabs.bukkit.ranks.argument;

import java.util.Collections;
import java.util.List;
import us.iorilabs.bukkit.ranks.Rank;
import us.iorilabs.bukkit.ranks.RanksCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class RankInfoArgument extends CommandArgument {

    private final RanksCommand parent;

    public RankInfoArgument(RanksCommand parent) {
        super("info", "ioricore.ranks.info");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 1) {
            source.sendMessage(parent.language().raw(source, "RANKS.INFO-USAGE"));
            return;
        }
        Rank rank = RankArgs.requireRank(parent, source, args[0]);
        if (rank == null) return;

        List<String> lines = parent
            .language()
            .list(
                source,
                "RANKS.INFO",
                RankArgs.map(
                    "rank",
                    rank.key(),
                    "display",
                    rank.display(),
                    "prefix",
                    rank.prefix() == null ? "" : rank.prefix(),
                    "suffix",
                    rank.suffix() == null ? "" : rank.suffix(),
                    "weight",
                    String.valueOf(rank.weight()),
                    "color",
                    rank.colorKey() == null ? "" : rank.colorKey(),
                    "type",
                    rank.type().name(),
                    "scope",
                    rank.scope().name(),
                    "flags",
                    rank.flags().toString(),
                    "parents",
                    String.join(", ", rank.parents()),
                    "permissions",
                    String.valueOf(rank.permissionCount()),
                    "default",
                    String.valueOf(rank.defaultRank())
                )
            );
        for (String line : lines) source.sendMessage(line);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        return args.length <= 1
            ? RankArgs.suggestRanks(parent, args.length == 0 ? "" : args[0])
            : Collections.emptyList();
    }
}
