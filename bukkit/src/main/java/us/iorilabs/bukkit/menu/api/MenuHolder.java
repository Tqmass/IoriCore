package us.iorilabs.bukkit.menu.api;

import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

@Getter
@Setter
@Accessors(fluent = true)
public final class MenuHolder implements InventoryHolder {

    private Menu menu;
    private Inventory inventory;
    private String title = "";
    private Map<Integer, Button> buttons = Collections.emptyMap();

    public MenuHolder(Menu menu) {
        this.menu = menu;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
