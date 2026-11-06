package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.bukkit.modules.core.vanish.VanishService;
import us.iorilabs.proxy.commands.CommandSource;

public final class TpAllCommand extends EssentialsCommand {

    private final EssentialsState state;
    private final VanishService vanish;

    public TpAllCommand(IoriCorePlugin plugin, EssentialsState state, VanishService vanish) {
        super(plugin, "tpall", Collections.singletonList("teleportall"), "ioricore.essentials.tpall");
        this.state = state;
        this.vanish = vanish;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.tpall")) return;

        Player sender = requirePlayer(source);
        if (sender == null) return;

        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender)) continue;
            if (vanish != null && vanish.vanished(online.getUniqueId())) continue;
            state.rememberBack(online);
            online.teleport(sender.getLocation());
            EssentialsSounds.play(plugin, online, "SUCCESS");
            count++;
        }
        EssentialsSounds.play(plugin, sender, "SUCCESS");
        send(source, "ESSENTIALS.TPALL.SUCCESS", Map.of("count", String.valueOf(count)));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.TPALL.USAGE");
    }
}
