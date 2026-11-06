package us.iorilabs.bukkit.ranks.argument;

import java.util.Locale;
import us.iorilabs.bukkit.ranks.RanksCommand;
import us.iorilabs.bukkit.ranks.enums.RankEditResult;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class RankCreateArgument extends CommandArgument {

    private final RanksCommand parent;

    public RankCreateArgument(RanksCommand parent) {
        super("create", "ioricore.ranks.create");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 1) {
            source.sendMessage(parent.language().raw(source, "RANKS.CREATE-USAGE"));
            return;
        }
        int weight = 0;
        if (args.length >= 2) {
            try {
                weight = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                RankArgs.feedback(parent, source, RankEditResult.INVALID_VALUE, "rank", args[0]);
                return;
            }
        }
        RankEditResult result = parent.ranks().create(args[0], weight);
        if (result == RankEditResult.OK) {
            source.sendMessage(
                parent
                    .language()
                    .raw(
                        source,
                        "RANKS.CREATED",
                        "rank",
                        args[0].toLowerCase(Locale.ROOT),
                        "weight",
                        String.valueOf(weight)
                    )
            );
            return;
        }
        RankArgs.feedback(parent, source, result, "rank", args[0], "weight", String.valueOf(weight));
    }
}
