package us.iorilabs.bukkit.menu.api.button;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.menu.MenuFiller;
import us.iorilabs.bukkit.menu.api.Button;
import us.iorilabs.bukkit.util.ItemBuilder;
import us.iorilabs.bukkit.util.LegacyMaterials;

public class FillerButton extends Button {

    private static final int DEFAULT_LEGACY_DATA = 7;

    private final ItemStack item;

    public FillerButton() {
        this(DEFAULT_LEGACY_DATA);
    }

    public FillerButton(int legacyData) {
        this(new ItemBuilder(LegacyMaterials.stainedPane(legacyData)).name(" ").build());
    }

    public FillerButton(MenuFiller filler) {
        this(filler.build());
    }

    public FillerButton(ItemStack item) {
        this.item = item;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return item;
    }
}
