package us.iorilabs.bukkit.menu;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class Menu implements InventoryHolder {

    private final Map<Integer, MenuItem> items = new HashMap<>();
    private Inventory inventory;
    private String clickSound;
    private Player viewer;

    public abstract String title(Player player);

    public abstract int rows();

    public abstract void populate(Player player);

    public final void open(Player player) {
        viewer = player;
        items.clear();
        inventory = Bukkit.createInventory(this, rows() * 9, MenuColors.title(title(player)));
        populate(player);
        for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().stack());
        }
        player.openInventory(inventory);
    }

    protected Player viewer() {
        return viewer;
    }

    protected void set(int slot, MenuItem item) {
        if (slot < 0 || (inventory != null && slot >= inventory.getSize())) return;
        if (item == null) {
            items.remove(slot);
            return;
        }
        items.put(slot, item);
    }

    protected void fill(MenuFiller filler) {
        if (filler == null || inventory == null) return;
        for (int i = 0; i < inventory.getSize(); i++) {
            if (items.containsKey(i)) continue;
            set(i, new MenuItem(filler.build()));
        }
    }

    protected void paintBorder(MenuFiller filler) {
        if (filler == null || inventory == null) return;
        int size = inventory.getSize();
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            boolean border = row == 0 || row == rows - 1 || col == 0 || col == 8;
            if (!border || items.containsKey(i)) continue;
            set(i, new MenuItem(filler.build()));
        }
    }

    protected void paintSlots(MenuFiller filler) {
        if (filler == null || filler.slots().isEmpty()) return;
        for (int slot : filler.slots()) {
            set(slot, new MenuItem(filler.build()));
        }
    }

    protected void clickSound(String key) {
        this.clickSound = key;
    }

    public String clickSound() {
        return clickSound;
    }

    public MenuItem item(int slot) {
        return items.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
