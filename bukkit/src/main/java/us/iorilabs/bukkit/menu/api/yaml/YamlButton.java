package us.iorilabs.bukkit.menu.api.yaml;

import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.menu.api.Button;
import us.iorilabs.bukkit.menu.enums.MenuClickType;

public final class YamlButton extends Button {

    private final YamlMenuService menus;
    private final YamlMenuItemDef def;
    private final Map<String, String> ctx;
    private final YamlItemFactory items;
    private final YamlActionRunner runner;

    public YamlButton(
        YamlMenuService menus,
        YamlMenuItemDef def,
        Map<String, String> ctx,
        YamlItemFactory items,
        YamlActionRunner runner
    ) {
        this.menus = menus;
        this.def = def;
        this.ctx = ctx;
        this.items = items;
        this.runner = runner;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        Map<String, String> ph = YamlPlaceholders.base(player, ctx);
        YamlMenuItemDef effective = YamlMenuItemState.resolve(player, def, ph);
        return items.build(player, effective, ctx, menus, currentDefinition(player));
    }

    @Override
    public void clicked(Player player, int slot, ClickType click, int hotbarButton) {
        Map<String, String> placeholders = YamlPlaceholders.base(player, ctx);
        YamlMenuItemDef effective = YamlMenuItemState.resolve(player, def, placeholders);
        if (!YamlRequirement.all(effective.clickRequirements(), player, placeholders)) {
            YamlMenuDefinition current = currentDefinition(player);
            String denySound = effective.itemSound();
            if (denySound == null || denySound.isEmpty()) {
                denySound = current != null ? current.soundDeny() : "";
            }
            if ((denySound == null || denySound.isEmpty()) && current != null && current.themeName() != null) {
                denySound = menus.themeSoundFor(current.themeName(), "MENU-LOCKED");
            }
            if (denySound != null && !denySound.isEmpty() && menus.plugin().sounds() != null) {
                menus.plugin().sounds().play(player, denySound);
            }
            runner.run(player, effective.denyActions(), placeholders);
            return;
        }

        YamlMenuDefinition current = currentDefinition(player);
        String clickSound = effective.itemSound();
        if (clickSound == null || clickSound.isEmpty()) {
            clickSound = current != null ? current.soundClick() : "";
        }
        if (clickSound != null && !clickSound.isEmpty() && menus.plugin().sounds() != null) {
            menus.plugin().sounds().play(player, clickSound);
        }

        List<YamlAction> actions = effective.actionsFor(MenuClickType.of(click));
        runner.run(player, actions, placeholders);
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType click) {
        return false;
    }

    private YamlMenuDefinition currentDefinition(Player player) {
        String id = menus.sessions().of(player.getUniqueId()).currentId();
        return id == null ? null : menus.definition(player, id);
    }
}
