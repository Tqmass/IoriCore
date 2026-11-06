package us.iorilabs.bukkit.ranks.argument;

import java.util.Collections;
import java.util.List;
import us.iorilabs.bukkit.ranks.RanksCommand;
import us.iorilabs.bukkit.ranks.enums.RankEditResult;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class RankParentArgument extends CommandArgument {

    private final RanksCommand parent;
    private final boolean add;

    public RankParentArgument(RanksCommand parent, boolean add) {
        super(add ? "addparent" : "delparent", "ioricore.ranks.edit");
        this.parent = parent;
        this.add = add;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 2) {
            source.sendMessage(parent.language().raw(source, add ? "RANKS.ADDPARENT-USAGE" : "RANKS.DELPARENT-USAGE"));
            return;
        }
        RankEditResult result = add
            ? parent.ranks().addParent(args[0], args[1])
            : parent.ranks().removeParent(args[0], args[1]);
        if (result == RankEditResult.OK) {
            source.sendMessage(
                parent
                    .language()
                    .raw(
                        source,
                        add ? "RANKS.PARENT-ADDED" : "RANKS.PARENT-REMOVED",
                        "rank",
                        args[0],
                        "parent",
                        args[1]
                    )
            );
            return;
        }
        RankArgs.feedback(parent, source, result, "rank", args[0], "parent", args[1]);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) return RankArgs.suggestRanks(parent, args.length == 0 ? "" : args[0]);
        if (args.length == 2) return RankArgs.suggestRanks(parent, args[1]);
        return Collections.emptyList();
    }
}
