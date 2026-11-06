package us.iorilabs.bukkit.modules.tags.command.argument;

import us.iorilabs.bukkit.modules.tags.command.TagCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class TagUnequipArgument extends CommandArgument {

    private final TagCommand parent;

    public TagUnequipArgument(TagCommand parent) {
        super("unequip", "ioricore.tags.equip");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        parent.unequip(source);
    }
}
