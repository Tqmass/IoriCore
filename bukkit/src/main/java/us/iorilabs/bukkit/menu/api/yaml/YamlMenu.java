package us.iorilabs.bukkit.menu.api.yaml;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.menu.api.Button;
import us.iorilabs.bukkit.menu.api.Menu;
import us.iorilabs.bukkit.menu.api.button.FillerButton;
import us.iorilabs.bukkit.menu.enums.FillMode;
import us.iorilabs.bukkit.menu.enums.MenuClickType;

public final class YamlMenu extends Menu {

    private final YamlMenuService menus;
    private final YamlMenuDefinition definition;
    private final Map<String, String> ctx;
    private final YamlItemFactory items;
    private final YamlActionRunner runner;

    public YamlMenu(
        YamlMenuService menus,
        YamlMenuDefinition definition,
        Map<String, String> ctx,
        YamlItemFactory items,
        YamlActionRunner runner
    ) {
        this.menus = menus;
        this.definition = definition;
        this.ctx = ctx;
        this.items = items;
        this.runner = runner;
        autoUpdate(definition.updateInterval() > 0);
        updateAfterClick(false);
    }

    @Override
    public String getTitle(Player player) {
        Map<String, String> placeholders = YamlPlaceholders.base(player, ctx);
        if (definition.titleKey() != null && !definition.titleKey().isEmpty() && menus.plugin().language() != null) {
            return menus.plugin().language().rawFor(player, definition.titleKey(), placeholders);
        }
        return YamlPlaceholders.apply(definition.title(), player, placeholders);
    }

    @Override
    public int getSize() {
        return definition.size();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        Map<String, String> placeholders = YamlPlaceholders.base(player, ctx);
        menus.internals().enrich(player, definition.id(), placeholders);

        if (definition.filler() != null) {
            Button filler = new FillerButton(items.build(player, definition.filler(), placeholders, menus, definition));
            if (definition.fillMode() == FillMode.FILL) fill(buttons, filler);
            else if (definition.fillMode() == FillMode.BORDER || definition.border()) border(buttons, filler);
        }

        if (definition.accentFiller() != null) {
            Button accent = new FillerButton(
                items.build(player, definition.accentFiller(), placeholders, menus, definition)
            );
            for (Integer slot : definition.accentSlots()) {
                if (slot != null) buttons.put(slot, accent);
            }
        }

        if (definition.items() != null) {
            for (YamlMenuItemDef item : definition.items().values()) {
                if (item == null || item.slots() == null) continue;
                if (!YamlRequirement.all(item.viewRequirements(), player, placeholders)) continue;
                YamlButton button = new YamlButton(menus, item, placeholders, items, runner);
                for (Integer slot : item.slots()) {
                    if (slot != null) buttons.put(slot, button);
                }
            }
        }

        placeDynamic(player, buttons, placeholders);
        placePagination(player, buttons, placeholders);
        return buttons;
    }

    @Override
    public void onOpen(Player player) {
        if (definition.soundOpen() != null && !definition.soundOpen().isEmpty() && menus.plugin().sounds() != null) {
            menus.plugin().sounds().play(player, definition.soundOpen());
        }
    }

    private void placeDynamic(Player player, Map<Integer, Button> buttons, Map<String, String> placeholders) {
        if (definition.dynamicSource() == null || definition.dynamicSource().isEmpty()) return;
        if (definition.dynamicTemplate() == null || definition.dynamicSlots().isEmpty()) return;

        List<Map<String, String>> entries = menus.dynamics().provide(definition.dynamicSource(), player, placeholders);
        int page = menus.sessions().page(player.getUniqueId(), definition.id());
        int pageSize = definition.dynamicSlots().size();
        int start = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int index = start + i;
            if (index >= entries.size()) break;
            Integer slot = definition.dynamicSlots().get(i);
            if (slot == null || buttons.containsKey(slot)) continue;
            Map<String, String> entryCtx = YamlPlaceholders.merge(placeholders, entries.get(index));
            YamlMenuItemDef templated = definition.dynamicTemplate().copyWithPlaceholders(entryCtx);
            YamlMenuItemDef stateful = YamlMenuItemState.resolve(player, templated, entryCtx);
            if (!YamlRequirement.all(stateful.viewRequirements(), player, entryCtx)) continue;
            buttons.put(slot, new YamlButton(menus, stateful, entryCtx, items, runner));
        }
    }

    private void placePagination(Player player, Map<Integer, Button> buttons, Map<String, String> placeholders) {
        int page = menus.sessions().page(player.getUniqueId(), definition.id());
        int pageSize = definition.dynamicSlots() == null ? 0 : definition.dynamicSlots().size();
        int total = 0;
        if (definition.dynamicSource() != null && !definition.dynamicSource().isEmpty()) {
            total = menus.dynamics().provide(definition.dynamicSource(), player, placeholders).size();
        }
        int pages = pageSize <= 0 ? 1 : Math.max(1, (total + pageSize - 1) / pageSize);

        if (definition.pagePrev() >= 0 && page > 0) {
            buttons.put(definition.pagePrev(), pageButton("PREV", "[page] prev", placeholders, page, pages, total));
        }
        if (definition.pageNext() >= 0 && page + 1 < pages) {
            buttons.put(definition.pageNext(), pageButton("NEXT", "[page] next", placeholders, page, pages, total));
        }
        if (definition.pageInfo() >= 0) {
            buttons.put(definition.pageInfo(), pageButton("INFO", "", placeholders, page, pages, total));
        }
    }

    private Button pageButton(
        String key,
        String action,
        Map<String, String> placeholders,
        int page,
        int pages,
        int total
    ) {
        YamlMenuItemDef item = definition.item(key);
        if (item == null) {
            item = new YamlMenuItemDef();
            item.material("ARROW");
            item.name("&e" + key);
        }
        Map<String, String> local = new HashMap<>(placeholders);
        local.put("page", String.valueOf(page + 1));
        local.put("pages", String.valueOf(pages));
        local.put("total", String.valueOf(total));
        YamlMenuItemDef copy = item.copyWithPlaceholders(local);
        if (action != null && !action.isEmpty()) {
            copy.actions(
                Collections.singletonMap(MenuClickType.ANY, Collections.singletonList(YamlAction.parse(action)))
            );
        }
        return new YamlButton(menus, copy, local, items, runner);
    }
}
