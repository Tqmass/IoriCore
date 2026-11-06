package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.proxy.commands.CommandSource;

public final class DisposalCommand extends EssentialsCommand {

    public DisposalCommand(IoriCorePlugin plugin) {
        super(plugin, "disposal", Arrays.asList("trash", "disposalchest"), "ioricore.essentials.disposal");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.disposal")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        String title = language.raw(player, "ESSENTIALS.DISPOSAL.TITLE");
        Inventory inventory = Bukkit.createInventory(player, 54, title);
        player.openInventory(inventory);

        Listener closer = new Listener() {
            @EventHandler
            public void onClose(InventoryCloseEvent event) {
                if (!(event.getPlayer() instanceof Player)) return;
                if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                if (event.getInventory() != inventory) return;
                event.getInventory().clear();
                HandlerList.unregisterAll(this);
            }
        };
        Bukkit.getPluginManager().registerEvents(closer, plugin);
        send(source, "ESSENTIALS.DISPOSAL.OPEN");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.DISPOSAL.USAGE");
    }
}
