package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.proxy.commands.CommandSource;

public final class HatCommand extends EssentialsCommand {

    public HatCommand(IoriCorePlugin plugin) {
        super(plugin, "hat", Collections.emptyList(), "ioricore.essentials.hat");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.hat")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        PlayerInventory inventory = player.getInventory();
        ItemStack hand = inventory.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) {
            send(source, "ESSENTIALS.HAT.EMPTY");
            return;
        }

        ItemStack helmet = inventory.getHelmet();
        inventory.setHelmet(hand.clone());
        inventory.setItemInHand(helmet == null ? new ItemStack(Material.AIR) : helmet.clone());
        send(source, "ESSENTIALS.HAT.SUCCESS");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.HAT.USAGE");
    }
}
