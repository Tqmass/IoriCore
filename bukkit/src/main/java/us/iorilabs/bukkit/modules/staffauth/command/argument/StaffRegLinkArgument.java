package us.iorilabs.bukkit.modules.staffauth.command.argument;

import org.bukkit.entity.Player;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.staffauth.StaffRegService;
import us.iorilabs.bukkit.modules.staffauth.command.StaffRegCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class StaffRegLinkArgument extends CommandArgument {

    private final StaffRegCommand parent;
    private final StaffRegService service;

    public StaffRegLinkArgument(StaffRegCommand parent, StaffRegService service) {
        super("link", "ioricore.staffreg");
        this.parent = parent;
        this.service = service;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!(source instanceof BukkitCommandSource bcs) || bcs.playerOrNull() == null) {
            parent.send(source, "GENERAL.PLAYER-ONLY");
            return;
        }
        Player player = bcs.playerOrNull();
        service.beginDiscordLink(player);
    }
}
