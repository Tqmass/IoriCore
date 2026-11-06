package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.spawn.SpawnService;
import us.iorilabs.proxy.commands.CommandSource;

public final class SetSpawnCommand extends EssentialsCommand {

    private final SpawnService spawn;

    public SetSpawnCommand(IoriCorePlugin plugin, SpawnService spawn) {
        super(plugin, "setspawn", Collections.emptyList(), "ioricore.essentials.setspawn");
        this.spawn = spawn;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.setspawn")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        spawn.setSpawn(player.getLocation());
        player
            .getWorld()
            .setSpawnLocation(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
            );
        send(source, "ESSENTIALS.SETSPAWN.SUCCESS");
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.SETSPAWN.USAGE");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
