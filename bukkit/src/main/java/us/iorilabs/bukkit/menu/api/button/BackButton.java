package us.iorilabs.bukkit.menu.api.button;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.menu.api.Button;
import us.iorilabs.bukkit.menu.api.ButtonSound;
import us.iorilabs.bukkit.menu.api.Menu;
import us.iorilabs.bukkit.menu.api.MenuApi;
import us.iorilabs.bukkit.util.ItemBuilder;

public class BackButton extends Button {

    private final Menu target;
    private final ItemStack item;

    public BackButton(Menu target) {
        this(target, null);
    }

    public BackButton(Menu target, ItemStack item) {
        this.target = target;
        this.item = item;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        if (item != null) return item;
        return new ItemBuilder(Material.ARROW).name(MenuApi.text(player, "MENU-API.BACK")).build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType click, int hotbarButton) {
        play(player, ButtonSound.CLICK);
        target.openMenu(player);
    }
}
