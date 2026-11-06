package us.iorilabs.bukkit.menu.api.button;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.menu.api.Button;
import us.iorilabs.bukkit.menu.api.ButtonSound;
import us.iorilabs.bukkit.menu.api.MenuApi;
import us.iorilabs.bukkit.util.ItemBuilder;

public class CloseButton extends Button {

    private final ItemStack item;

    public CloseButton() {
        this(null);
    }

    public CloseButton(ItemStack item) {
        this.item = item;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        if (item != null) return item;
        return new ItemBuilder(Material.BARRIER).name(MenuApi.text(player, "MENU-API.CLOSE")).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType click, int hotbarButton) {
        play(player, ButtonSound.CLICK);
        player.closeInventory();
    }
}
