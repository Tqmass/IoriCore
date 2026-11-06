package us.iorilabs.bukkit.menu.api;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.menu.MenuColors;

@Getter
@Setter
@Accessors(fluent = true)
public abstract class Menu {

    private static final int ROW = 9;
    private static final int MAX_SIZE = 54;

    private boolean autoUpdate;
    private boolean updateAfterClick;

    public abstract String getTitle(Player player);

    public abstract Map<Integer, Button> getButtons(Player player);

    public int getSize() {
        return -1;
    }

    protected Button getPlaceholder() {
        return null;
    }

    public void onOpen(Player player) {}

    public void onClose(Player player) {}

    public final void openMenu(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!Bukkit.isPrimaryThread()) {
            MenuApi.sync(() -> openMenu(player));
            return;
        }

        Map<Integer, Button> layout = new HashMap<>(getButtons(player));
        int size = resolveSize(layout);
        Button placeholder = getPlaceholder();
        if (placeholder != null) {
            for (int slot = 0; slot < size; slot++) layout.putIfAbsent(slot, placeholder);
        }
        layout.keySet().removeIf(slot -> slot < 0 || slot >= size);

        String title = MenuColors.title(player, getTitle(player));
        MenuHolder holder = reusableHolder(player, size, title);
        boolean reuse = holder != null;

        if (reuse) {
            Menu previous = holder.menu();
            holder.menu(this);
            if (previous != this) previous.onClose(player);
        } else {
            holder = new MenuHolder(this);
            holder.title(title);
            holder.inventory(Bukkit.createInventory(holder, size, title));
        }

        Inventory inventory = holder.inventory();
        inventory.clear();
        holder.buttons(layout);
        for (Map.Entry<Integer, Button> entry : layout.entrySet()) {
            inventory.setItem(entry.getKey(), render(player, entry.getValue()));
        }

        if (reuse) {
            player.updateInventory();
        } else {
            player.openInventory(inventory);
        }
        MenuApi.track(player, holder);
        onOpen(player);
    }

    protected final int slot(int x, int y) {
        return y * ROW + x;
    }

    protected final void fill(Map<Integer, Button> buttons, Button filler) {
        int size = getSize();
        for (int slot = 0; slot < size; slot++) buttons.putIfAbsent(slot, filler);
    }

    protected final void border(Map<Integer, Button> buttons, Button filler) {
        int size = getSize();
        if (size < ROW) return;
        int rows = size / ROW;
        for (int slot = 0; slot < size; slot++) {
            int row = slot / ROW;
            int column = slot % ROW;
            boolean edge = row == 0 || row == rows - 1 || column == 0 || column == ROW - 1;
            if (edge) buttons.putIfAbsent(slot, filler);
        }
    }

    protected final void corners(Map<Integer, Button> buttons, Button filler) {
        int size = getSize();
        if (size < ROW) return;
        for (int slot : new int[] { 0, ROW - 1, size - ROW, size - 1 }) {
            buttons.putIfAbsent(slot, filler);
        }
    }

    private MenuHolder reusableHolder(Player player, int size, String title) {
        MenuHolder holder = MenuApi.session(player);
        if (holder == null || holder.inventory() == null) return null;
        Inventory inventory = holder.inventory();
        if (inventory.getSize() != size || !title.equals(holder.title())) return null;
        return inventory.getViewers().contains(player) ? holder : null;
    }

    private int resolveSize(Map<Integer, Button> layout) {
        int requested = getSize();
        if (requested <= 0) {
            int highest = -1;
            for (int slot : layout.keySet()) highest = Math.max(highest, slot);
            requested = highest + 1;
        }
        int padded = Math.min(MAX_SIZE, Math.max(ROW, requested)) + ROW - 1;
        return Math.min(MAX_SIZE, (padded / ROW) * ROW);
    }

    private ItemStack render(Player player, Button button) {
        try {
            return button.getButtonItem(player);
        } catch (RuntimeException ex) {
            MenuApi.warn("Menu button failed: " + button.getClass().getSimpleName(), ex);
            return null;
        }
    }
}
