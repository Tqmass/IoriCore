package us.iorilabs.bukkit.ranks.argument;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import us.iorilabs.bukkit.ranks.RanksCommand;
import us.iorilabs.bukkit.ranks.enums.RankEditResult;
import us.iorilabs.bukkit.ranks.enums.RankField;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class RankSetFieldArgument extends CommandArgument {

    private final RanksCommand parent;
    private final RankField field;

    public RankSetFieldArgument(RanksCommand parent, String name, RankField field) {
        super(name, "ioricore.ranks.edit");
        this.parent = parent;
        this.field = field;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 2) {
            source.sendMessage(parent.language().raw(source, "RANKS.SET-USAGE", "field", name()));
            return;
        }
        String value = RankArgs.join(args, 1);
        RankEditResult result = parent.ranks().setField(args[0], field, value);
        if (result == RankEditResult.OK) {
            source.sendMessage(
                parent
                    .language()
                    .raw(
                        source,
                        "RANKS.SET-OK",
                        "rank",
                        args[0],
                        "field",
                        field.name().toLowerCase(Locale.ROOT),
                        "value",
                        value
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
