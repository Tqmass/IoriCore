package us.iorilabs.bukkit.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Menu)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Menu menu = (Menu) holder;
        Player player = (Player) event.getWhoClicked();

        event.setCancelled(true);
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getHolder() != menu) return;

        MenuItem item = menu.item(event.getSlot());
        if (item == null) return;
        if (item.click() == null) return;

        String sound = menu.clickSound();
        if (sound != null) {
            IoriCorePlugin.getPlugin(IoriCorePlugin.class).sounds().play(player, sound);
        }
        item.handle(player, event.getClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
