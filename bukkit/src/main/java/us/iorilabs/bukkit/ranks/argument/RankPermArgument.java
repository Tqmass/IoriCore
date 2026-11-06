package us.iorilabs.bukkit.ranks.argument;

import java.util.Collections;
import java.util.List;
import us.iorilabs.bukkit.ranks.RanksCommand;
import us.iorilabs.bukkit.ranks.enums.RankEditResult;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class RankPermArgument extends CommandArgument {

    private final RanksCommand parent;
    private final boolean add;

    public RankPermArgument(RanksCommand parent, boolean add) {
        super(add ? "addperm" : "delperm", "ioricore.ranks.permissions");
        this.parent = parent;
        this.add = add;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 2) {
            source.sendMessage(parent.language().raw(source, add ? "RANKS.ADDPERM-USAGE" : "RANKS.DELPERM-USAGE"));
            return;
        }
        RankEditResult result = add
            ? parent.ranks().addPermission(args[0], args[1])
            : parent.ranks().removePermission(args[0], args[1]);
        if (result == RankEditResult.OK) {
            source.sendMessage(
                parent
                    .language()
                    .raw(
                        source,
                        add ? "RANKS.PERM-ADDED" : "RANKS.PERM-REMOVED",
                        "rank",
                        args[0],
                        "permission",
                        args[1]
                    )
            );
            return;
        }
        RankArgs.feedback(parent, source, result, "rank", args[0]);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        return args.length <= 1
            ? RankArgs.suggestRanks(parent, args.length == 0 ? "" : args[0])
            : Collections.emptyList();
    }
}
