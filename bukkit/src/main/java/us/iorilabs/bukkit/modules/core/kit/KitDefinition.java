package us.iorilabs.bukkit.modules.core.kit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.inventory.ItemStack;

@Getter
@Accessors(fluent = true)
public final class KitDefinition {

    private final String name;
    private final String permission;
    private final long cooldownSeconds;
    private final List<ItemStack> items;

    public KitDefinition(String name, String permission, long cooldownSeconds, List<ItemStack> items) {
        this.name = name;
        this.permission = permission;
        this.cooldownSeconds = Math.max(0L, cooldownSeconds);
        this.items = items == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(items));
    }
}
