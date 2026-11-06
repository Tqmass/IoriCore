package us.iorilabs.bukkit.ranks.argument;

import java.util.Collections;
import java.util.List;
import us.iorilabs.bukkit.ranks.RanksCommand;
import us.iorilabs.bukkit.ranks.enums.RankEditResult;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class RankDeleteArgument extends CommandArgument {

    private final RanksCommand parent;

    public RankDeleteArgument(RanksCommand parent) {
        super("delete", "ioricore.ranks.delete");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 1) {
            source.sendMessage(parent.language().raw(source, "RANKS.DELETE-USAGE"));
            return;
        }
        RankEditResult result = parent.ranks().delete(args[0]);
        if (result == RankEditResult.OK) {
            source.sendMessage(parent.language().raw(source, "RANKS.DELETED", "rank", args[0]));
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
