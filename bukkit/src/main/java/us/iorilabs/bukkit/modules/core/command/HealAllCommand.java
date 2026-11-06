package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.proxy.commands.CommandSource;

public final class HealAllCommand extends EssentialsCommand {

    public HealAllCommand(IoriCorePlugin plugin) {
        super(plugin, "healall", Collections.emptyList(), "ioricore.essentials.healall");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.healall")) return;

        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.setHealth(online.getMaxHealth());
            online.setFoodLevel(20);
            online.setSaturation(20f);
            online.setFireTicks(0);
            EssentialsSounds.play(plugin, online, "SUCCESS");
            count++;
        }
        send(source, "ESSENTIALS.HEALALL.SUCCESS", Map.of("count", String.valueOf(count)));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.HEALALL.USAGE");
    }
}
