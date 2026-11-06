package us.iorilabs.bukkit.modules.staffmode.command.argument;

import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.staffmode.command.StaffModeCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class StaffModeChatArgument extends CommandArgument {

    private final StaffModeCommand parent;

    public StaffModeChatArgument(StaffModeCommand parent) {
        super("chat", "ioricore.staffchat.send");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(parent.language().raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        parent.staffChat().toggle(player);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
