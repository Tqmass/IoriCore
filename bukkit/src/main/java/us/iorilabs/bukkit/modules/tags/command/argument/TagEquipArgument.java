package us.iorilabs.bukkit.modules.tags.command.argument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.tags.Tag;
import us.iorilabs.bukkit.modules.tags.command.TagCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class TagEquipArgument extends CommandArgument {

    private final TagCommand parent;

    public TagEquipArgument(TagCommand parent) {
        super("equip", "ioricore.tags.equip");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        parent.equip(source, args.length == 0 ? null : args[0]);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length > 1) return Collections.emptyList();

        Player player = EssentialsPlayers.player(source);
        if (player == null) return Collections.emptyList();

        String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Tag tag : parent.tags().visibleFor(player)) {
            if (tag.id().startsWith(prefix)) out.add(tag.id());
        }
        return out;
    }
}
