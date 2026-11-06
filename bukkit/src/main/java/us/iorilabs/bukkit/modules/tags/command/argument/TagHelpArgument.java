package us.iorilabs.bukkit.modules.tags.command.argument;

import us.iorilabs.bukkit.modules.tags.command.TagCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class TagHelpArgument extends CommandArgument {

    private final TagCommand parent;

    public TagHelpArgument(TagCommand parent) {
        super("help", null);
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        parent.sendUsage(source);
    }
}
