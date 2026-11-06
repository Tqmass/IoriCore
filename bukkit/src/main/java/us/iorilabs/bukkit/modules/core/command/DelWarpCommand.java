package us.iorilabs.bukkit.modules.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.warp.WarpService;
import us.iorilabs.proxy.commands.CommandSource;

public final class DelWarpCommand extends EssentialsCommand {

    private final WarpService warps;

    public DelWarpCommand(IoriCorePlugin plugin, WarpService warps) {
        super(plugin, "delwarp", Collections.singletonList("deletewarp"), "ioricore.essentials.delwarp");
        this.warps = warps;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.delwarp")) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        if (!warps.delete(name)) {
            send(source, "ESSENTIALS.WARP.NOT-FOUND", Map.of("warp", name));
            return;
        }
        send(source, "ESSENTIALS.DELWARP.SUCCESS", Map.of("warp", name));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.DELWARP.USAGE");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String name : warps.names()) {
            if (name.startsWith(prefix)) out.add(name);
        }
        return out;
    }
}
