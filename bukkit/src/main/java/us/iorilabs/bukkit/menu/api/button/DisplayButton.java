package us.iorilabs.bukkit.menu.api.button;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.menu.api.Button;

public class DisplayButton extends Button {

    private final ItemStack item;
    private final boolean cancel;

    public DisplayButton(ItemStack item) {
        this(item, true);
    }

    public DisplayButton(ItemStack item, boolean cancel) {
        this.item = item;
        this.cancel = cancel;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return item == null ? new ItemStack(Material.AIR) : item;
    }

    @Override
    public boolean shouldCancel(Player player, int slot, ClickType click) {
        return cancel;
    }
}
