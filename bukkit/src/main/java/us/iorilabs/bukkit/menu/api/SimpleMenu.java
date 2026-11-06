package us.iorilabs.bukkit.menu.api;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.menu.api.button.CloseButton;
import us.iorilabs.bukkit.menu.api.button.FillerButton;

public abstract class SimpleMenu extends Menu {

    private static final int DEFAULT_SIZE = 45;
    private static final int DEFAULT_GLASS_DATA = 7;

    public abstract Map<Integer, Button> getMenuButtons(Player player);

    @Override
    public final Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>(getMenuButtons(player));
        if (closeCorners()) corners(buttons, new CloseButton());
        if (glassBorders()) border(buttons, new FillerButton(glassData()));
        return buttons;
    }

    @Override
    public int getSize() {
        return DEFAULT_SIZE;
    }

    @Override
    protected Button getPlaceholder() {
        return usePlaceholder() ? new FillerButton(glassData()) : null;
    }

    protected boolean closeCorners() {
        return false;
    }

    protected boolean glassBorders() {
        return false;
    }

    protected boolean usePlaceholder() {
        return false;
    }

    protected int glassData() {
        return DEFAULT_GLASS_DATA;
    }
}
