package us.iorilabs.bukkit.menu;

import java.util.function.Consumer;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@Getter
@Accessors(fluent = true)
public final class MenuItem {

    private final ItemStack stack;
    private final Consumer<ClickContext> click;

    public MenuItem(ItemStack stack) {
        this(stack, null);
    }

    public MenuItem(ItemStack stack, Consumer<ClickContext> click) {
        this.stack = stack;
        this.click = click;
    }

    public void handle(Player player, ClickType type) {
        if (click == null) return;
        click.accept(new ClickContext(player, type));
    }

    @Getter
    @Accessors(fluent = true)
    public static final class ClickContext {

        private final Player player;
        private final ClickType type;

        public ClickContext(Player player, ClickType type) {
            this.player = player;
            this.type = type;
        }
    }
}
