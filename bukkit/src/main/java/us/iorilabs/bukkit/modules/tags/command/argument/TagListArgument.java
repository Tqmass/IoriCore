package us.iorilabs.bukkit.modules.tags.command.argument;

import org.bukkit.entity.Player;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.tags.command.TagCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class TagListArgument extends CommandArgument {

    private final TagCommand parent;

    public TagListArgument(TagCommand parent) {
        super("list", null);
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        Player player = EssentialsPlayers.player(source);
        if (player != null) {
            parent.plugin().yamlMenus().open(player, "tags");
            return;
        }
        parent.list(source);
    }
}
