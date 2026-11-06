package us.iorilabs.bukkit.menu.api;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;

public final class ButtonListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder owner = event.getInventory().getHolder();
        if (!(owner instanceof MenuHolder)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        MenuHolder holder = (MenuHolder) owner;
        Player player = (Player) event.getWhoClicked();
        ClickType click = event.getClick();
        int slot = event.getRawSlot();
        if (slot < 0) return;

        if (slot >= event.getInventory().getSize()) {
            if (click.isShiftClick() || click == ClickType.DOUBLE_CLICK) event.setCancelled(true);
            return;
        }

        Button button = holder.buttons().get(slot);
        if (button == null) {
            event.setCancelled(true);
            return;
        }

        boolean cancel = button.shouldCancel(player, slot, click);
        event.setCancelled(cancel);

        Menu menu = holder.menu();
        button.clicked(player, slot, click, event.getHotbarButton());
        refreshAfterClick(player, holder, menu, button, slot, click);

        if (cancel) MenuApi.sync(player::updateInventory);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder owner = event.getInventory().getHolder();
        if (!(owner instanceof MenuHolder)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        MenuHolder holder = (MenuHolder) owner;
        Player player = (Player) event.getWhoClicked();
        int topSize = event.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot >= topSize) continue;
            Button button = holder.buttons().get(slot);
            if (button != null && !button.shouldCancel(player, slot, ClickType.LEFT)) continue;
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder owner = event.getInventory().getHolder();
        if (!(owner instanceof MenuHolder)) return;
        if (!(event.getPlayer() instanceof Player)) return;

        MenuHolder holder = (MenuHolder) owner;
        Player player = (Player) event.getPlayer();
        MenuApi.untrack(player, holder);
        holder.menu().onClose(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MenuApi.untrack(event.getPlayer().getUniqueId());
    }

    private void refreshAfterClick(
        Player player,
        MenuHolder holder,
        Menu menu,
        Button button,
        int slot,
        ClickType click
    ) {
        if (MenuApi.session(player) != holder || holder.menu() != menu) return;
        if (button.shouldUpdate(player, slot, click) || menu.updateAfterClick()) menu.openMenu(player);
    }
}
