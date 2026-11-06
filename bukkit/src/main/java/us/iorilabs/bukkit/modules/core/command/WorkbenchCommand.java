package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Collections;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.proxy.commands.CommandSource;

public final class WorkbenchCommand extends EssentialsCommand {

    public WorkbenchCommand(IoriCorePlugin plugin) {
        super(plugin, "workbench", Arrays.asList("craft", "wb", "craftingtable"), "ioricore.essentials.workbench");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.workbench")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        player.openWorkbench(player.getLocation(), true);
        send(source, "ESSENTIALS.WORKBENCH.OPEN");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.WORKBENCH.USAGE");
    }
}
