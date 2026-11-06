package us.iorilabs.bukkit.menu.api;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class Button {

    public abstract ItemStack getButtonItem(Player player);

    public void clicked(Player player, int slot, ClickType click, int hotbarButton) {}

    public boolean shouldCancel(Player player, int slot, ClickType click) {
        return true;
    }

    public boolean shouldUpdate(Player player, int slot, ClickType click) {
        return false;
    }

    protected static void play(Player player, ButtonSound sound) {
        sound.play(player);
    }
}
