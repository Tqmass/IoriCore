package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.proxy.commands.CommandSource;

public final class RepairCommand extends EssentialsCommand {

    public RepairCommand(IoriCorePlugin plugin) {
        super(plugin, "repair", Collections.singletonList("fix"), "ioricore.essentials.repair");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.repair")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        boolean all = args.length > 0 && "all".equalsIgnoreCase(args[0]);
        if (all) {
            if (deny(source, "ioricore.essentials.repair.all")) return;
            int fixed = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (repair(item)) fixed++;
            }
            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (repair(item)) fixed++;
            }
            send(source, "ESSENTIALS.REPAIR.ALL", java.util.Map.of("count", String.valueOf(fixed)));
            return;
        }

        ItemStack hand = player.getItemInHand();
        if (!repair(hand)) {
            send(source, "ESSENTIALS.REPAIR.NONE");
            return;
        }
        send(source, "ESSENTIALS.REPAIR.SUCCESS");
    }

    private static boolean repair(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getType().getMaxDurability() <= 0) return false;
        if (item.getDurability() == 0) return false;
        item.setDurability((short) 0);
        return true;
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.REPAIR.USAGE");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 1 && "all".startsWith(args[0].toLowerCase())) {
            return Collections.singletonList("all");
        }
        return Collections.emptyList();
    }
}
