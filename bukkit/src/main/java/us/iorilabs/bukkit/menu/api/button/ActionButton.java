package us.iorilabs.bukkit.menu.api.button;

import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.menu.api.Button;
import us.iorilabs.bukkit.menu.api.ButtonSound;

public class ActionButton extends Button {

    private final ItemStack item;
    private final Consumer<Player> action;
    private final ButtonSound sound;
    private final boolean refresh;

    public ActionButton(ItemStack item, Consumer<Player> action) {
        this(item, action, ButtonSound.CLICK, false);
    }

    public ActionButton(ItemStack item, Consumer<Player> action, ButtonSound sound) {
        this(item, action, sound, false);
    }

    public ActionButton(ItemStack item, Consumer<Player> action, ButtonSound sound, boolean refresh) {
        this.item = item;
        this.action = action;
        this.sound = sound;
        this.refresh = refresh;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return item;
    }

    @Override
    public void clicked(Player player, int slot, ClickType click, int hotbarButton) {
        if (action == null) return;
        if (sound != null) play(player, sound);
        action.accept(player);
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType click) {
        return refresh;
    }
}
