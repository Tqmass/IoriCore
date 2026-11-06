package us.iorilabs.bukkit.menu.api.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.menu.enums.MenuClickType;
import us.iorilabs.bukkit.menu.enums.RequirementKind;

public final class YamlMenuItemState {

    private YamlMenuItemState() {}

    public static YamlMenuItemDef resolve(Player player, YamlMenuItemDef base, Map<String, String> placeholders) {
        if (base == null) return null;
        if (base.states() == null || base.states().isEmpty()) return base;
        String key = evaluate(base, player, placeholders);
        YamlMenuItemDef overlay = base.states().get(key);
        if (overlay == null) overlay = base.states().get("AVAILABLE");
        if (overlay == null) return base;
        return merge(base, overlay);
    }

    private static String evaluate(YamlMenuItemDef base, Player player, Map<String, String> placeholders) {
        List<YamlRequirement> hard = new ArrayList<>();
        List<YamlRequirement> soft = new ArrayList<>();
        if (base.stateRequirements() != null) {
            for (YamlRequirement req : base.stateRequirements()) {
                if (req == null) continue;
                if (req.kind() == RequirementKind.PERMISSION) hard.add(req);
                else soft.add(req);
            }
        }
        for (YamlRequirement req : hard) {
            if (!req.test(player, placeholders)) return "NO-PERMISSION";
        }
        for (YamlRequirement req : soft) {
            if (!req.test(player, placeholders)) return "MISSING-REQUIREMENTS";
        }
        return "AVAILABLE";
    }

    private static YamlMenuItemDef merge(YamlMenuItemDef base, YamlMenuItemDef overlay) {
        YamlMenuItemDef out = new YamlMenuItemDef();
        out.key(base.key());
        out.slots(base.slots());
        boolean overlayMat = overlay.material() != null && !overlay.material().trim().isEmpty();
        out.material(overlayMat ? overlay.material() : base.material());
        out.materialFallback(
            overlay.materialFallback() != null && !overlay.materialFallback().trim().isEmpty()
                ? overlay.materialFallback()
                : base.materialFallback()
        );
        out.data(overlayMat ? overlay.data() : base.data());
        out.customModelData(overlay.customModelData() >= 0 ? overlay.customModelData() : base.customModelData());
        out.amount(overlay.amount() > 1 ? overlay.amount() : base.amount());
        out.name(pickString(base.name(), overlay.name()));
        out.nameKey(pickString(base.nameKey(), overlay.nameKey()));
        List<String> lore = overlay.lore() != null && !overlay.lore().isEmpty() ? overlay.lore() : base.lore();
        out.lore(lore == null ? new ArrayList<>() : new ArrayList<>(lore));
        out.loreKey(pickString(base.loreKey(), overlay.loreKey()));
        out.glow(overlay.glow() || base.glow());
        out.enchants(
            overlay.enchants() != null && !overlay.enchants().isEmpty() ? overlay.enchants() : base.enchants()
        );
        out.flags(overlay.flags() != null && !overlay.flags().isEmpty() ? overlay.flags() : base.flags());
        out.skullOwner(pickString(base.skullOwner(), overlay.skullOwner()));
        out.skullTexture(pickString(base.skullTexture(), overlay.skullTexture()));
        out.skullSelf(base.skullSelf() || overlay.skullSelf());
        out.viewRequirements(base.viewRequirements());
        out.clickRequirements(base.clickRequirements());
        out.stateRequirements(base.stateRequirements());
        out.states(base.states());
        out.itemSound(pickString(base.itemSound(), overlay.itemSound()));
        Map<MenuClickType, List<YamlAction>> mergedActions = !overlay.actions().isEmpty()
            ? overlay.actions()
            : base.actions();
        out.actions(mergedActions);
        List<YamlAction> deny =
            overlay.denyActions() != null && !overlay.denyActions().isEmpty()
                ? overlay.denyActions()
                : base.denyActions();
        out.denyActions(deny == null ? List.of() : deny);
        return out;
    }

    private static String pickString(String baseValue, String overlayValue) {
        if (overlayValue != null && !overlayValue.trim().isEmpty()) return overlayValue;
        return baseValue == null ? "" : baseValue;
    }

    public static String stateKeyFor(YamlMenuItemDef base, Player player, Map<String, String> placeholders) {
        if (base == null || base.states() == null || base.states().isEmpty()) return "";
        return evaluate(base, player, placeholders).toUpperCase(Locale.ROOT);
    }
}
