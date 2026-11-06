package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.warp.WarpService;
import us.iorilabs.proxy.commands.CommandSource;

public final class SetWarpCommand extends EssentialsCommand {

    private final WarpService warps;

    public SetWarpCommand(IoriCorePlugin plugin, WarpService warps) {
        super(plugin, "setwarp", Collections.emptyList(), "ioricore.essentials.setwarp");
        this.warps = warps;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.setwarp")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        warps.set(name, player.getLocation());
        send(source, "ESSENTIALS.SETWARP.SUCCESS", Map.of("warp", name));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.SETWARP.USAGE");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
