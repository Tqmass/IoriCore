package us.iorilabs.bukkit.menu.api;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.menu.api.button.ActionButton;
import us.iorilabs.bukkit.menu.api.button.BackButton;
import us.iorilabs.bukkit.menu.api.button.DisplayButton;
import us.iorilabs.bukkit.menu.api.button.FillerButton;
import us.iorilabs.bukkit.util.ItemBuilder;
import us.iorilabs.bukkit.util.LegacyMaterials;

public final class ConfirmationMenu extends Menu {

    private static final int SIZE = 27;
    private static final int CONFIRM_SLOT = 11;
    private static final int PREVIEW_SLOT = 13;
    private static final int CANCEL_SLOT = 15;
    private static final int BACK_SLOT = 22;
    private static final int[] CONFIRM_SIDE = { 1, 2, 3, 10, 12, 19, 20, 21 };
    private static final int[] CANCEL_SIDE = { 5, 6, 7, 14, 16, 23, 24, 25 };
    private static final int GREEN = 5;
    private static final int RED = 14;

    private final String title;
    private final ItemStack preview;
    private final Runnable onConfirm;
    private final Menu back;

    public ConfirmationMenu(String title, ItemStack preview, Runnable onConfirm, Menu back) {
        this.title = title;
        this.preview = preview;
        this.onConfirm = onConfirm;
        this.back = back;
    }

    @Override
    public String getTitle(Player player) {
        return title;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        Button green = new FillerButton(GREEN);
        for (int slot : CONFIRM_SIDE) buttons.put(slot, green);
        Button red = new FillerButton(RED);
        for (int slot : CANCEL_SIDE) buttons.put(slot, red);

        if (preview != null) buttons.put(PREVIEW_SLOT, new DisplayButton(preview));
        buttons.put(CONFIRM_SLOT, confirmButton(player));
        buttons.put(CANCEL_SLOT, cancelButton(player));
        if (back != null) buttons.put(BACK_SLOT, new BackButton(back));
        return buttons;
    }

    private Button confirmButton(Player viewer) {
        ItemStack item = new ItemBuilder(LegacyMaterials.wool(GREEN))
            .name(MenuApi.text(viewer, "MENU-API.CONFIRM"))
            .build();
        return new ActionButton(
            item,
            player -> {
                player.closeInventory();
                onConfirm.run();
            },
            ButtonSound.CONFIRM
        );
    }

    private Button cancelButton(Player viewer) {
        ItemStack item = new ItemBuilder(LegacyMaterials.wool(RED))
            .name(MenuApi.text(viewer, "MENU-API.CANCEL"))
            .build();
        return new ActionButton(
            item,
            player -> {
                if (back == null) {
                    player.closeInventory();
                    return;
                }
                back.openMenu(player);
            },
            ButtonSound.DENY
        );
    }
}
