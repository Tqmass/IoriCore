package us.iorilabs.bukkit.ranks;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.ranks.argument.RankCreateArgument;
import us.iorilabs.bukkit.ranks.argument.RankDeleteArgument;
import us.iorilabs.bukkit.ranks.argument.RankFlagArgument;
import us.iorilabs.bukkit.ranks.argument.RankInfoArgument;
import us.iorilabs.bukkit.ranks.argument.RankParentArgument;
import us.iorilabs.bukkit.ranks.argument.RankPermArgument;
import us.iorilabs.bukkit.ranks.argument.RankSetFieldArgument;
import us.iorilabs.bukkit.ranks.argument.RanksHelpArgument;
import us.iorilabs.bukkit.ranks.argument.RanksListArgument;
import us.iorilabs.bukkit.ranks.enums.RankField;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class RanksCommand extends CommandParent {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final RankService ranks;

    public RanksCommand(IoriCorePlugin plugin, CoreLanguage language, RankService ranks) {
        super("ranks", Collections.singletonList("rank"), "ioricore.ranks");
        this.plugin = plugin;
        this.language = language;
        this.ranks = ranks;

        RanksListArgument list = new RanksListArgument(this);
        register(list);
        register(new RanksHelpArgument(this));
        register(new RankCreateArgument(this));
        register(new RankDeleteArgument(this));
        register(new RankInfoArgument(this));
        register(new RankSetFieldArgument(this, "setprefix", RankField.PREFIX));
        register(new RankSetFieldArgument(this, "setsuffix", RankField.SUFFIX));
        register(new RankSetFieldArgument(this, "setcolor", RankField.COLOR));
        register(new RankSetFieldArgument(this, "setweight", RankField.WEIGHT));
        register(new RankSetFieldArgument(this, "setdisplay", RankField.DISPLAY));
        register(new RankPermArgument(this, true));
        register(new RankPermArgument(this, false));
        register(new RankParentArgument(this, true));
        register(new RankParentArgument(this, false));
        register(new RankFlagArgument(this));
        setDefaultArgument(list);
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.ranks") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw(source, "GENERAL.NO-PERMISSION"));
            return;
        }
        super.execute(source, label, args);
    }

    public void openMenu(CommandSource source) {
        if (!(source instanceof BukkitCommandSource)) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }
        Player player = ((BukkitCommandSource) source).playerOrNull();
        if (player == null) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }
        plugin.yamlMenus().open(player, "rank-list");
    }

    @Override
    protected void sendUsage(CommandSource source) {
        List<String> lines = language.list(source, "RANKS.USAGE");
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
