package us.iorilabs.bukkit.menu.api.yaml;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.ConfigurationSection;

@Getter
@Setter
@Accessors(fluent = true)
public final class YamlMenuTheme {

    private YamlMenuItemDef filler;
    private YamlMenuItemDef accentFiller;
    private List<Integer> accentCornerSlots = Collections.emptyList();
    private String soundOpen = "";
    private String soundClick = "";
    private String soundBack = "";
    private String soundPage = "";
    private String soundLocked = "";
    private String soundSuccess = "";
    private String soundError = "";

    public static YamlMenuTheme from(ConfigurationSection section) {
        YamlMenuTheme theme = new YamlMenuTheme();
        if (section == null) return theme;
        ConfigurationSection fillerSection = section.getConfigurationSection("FILLER");
        if (fillerSection != null) theme.filler(YamlMenuItemDef.from("FILLER", fillerSection));
        ConfigurationSection accent = section.getConfigurationSection("ACCENT-FILLER");
        if (accent != null) {
            theme.accentFiller(YamlMenuItemDef.from("ACCENT", accent));
            theme.accentCornerSlots(YamlSlotParser.parse(accent.get("SLOTS")));
        }
        ConfigurationSection sound = section.getConfigurationSection("SOUND");
        if (sound != null) {
            theme.soundOpen(sound.getString("OPEN", ""));
            theme.soundClick(sound.getString("CLICK", ""));
            theme.soundBack(sound.getString("BACK", ""));
            theme.soundPage(sound.getString("PAGE", ""));
            theme.soundLocked(sound.getString("LOCKED", ""));
            theme.soundSuccess(sound.getString("SUCCESS", ""));
            theme.soundError(sound.getString("ERROR", ""));
        }
        return theme;
    }

    public void apply(YamlMenuDefinition def) {
        if (def == null) return;
        if (filler != null && def.filler() == null) def.filler(filler);
        if (accentFiller != null && def.accentFiller() == null) def.accentFiller(accentFiller);
        if (
            accentCornerSlots != null &&
            !accentCornerSlots.isEmpty() &&
            (def.accentSlots() == null || def.accentSlots().isEmpty())
        ) {
            def.accentSlots(accentCornerSlots);
        }
        if (soundOpen != null && !soundOpen.isEmpty() && (def.soundOpen() == null || def.soundOpen().isEmpty())) {
            def.soundOpen(soundOpen);
        }
        if (soundClick != null && !soundClick.isEmpty() && (def.soundClick() == null || def.soundClick().isEmpty())) {
            def.soundClick(soundClick);
        }
        if ((def.soundDeny() == null || def.soundDeny().isEmpty()) && soundLocked != null && !soundLocked.isEmpty()) {
            def.soundDeny(soundLocked);
        }
    }

    public String soundForItem(String key) {
        if (key == null || key.isEmpty()) return "";
        String k = key.trim().toUpperCase(Locale.ROOT);
        return switch (k) {
            case "MENU-BACK", "BACK" -> soundBack;
            case "MENU-PAGE", "PAGE" -> soundPage;
            case "MENU-LOCKED", "LOCKED" -> soundLocked;
            case "MENU-SUCCESS", "SUCCESS" -> soundSuccess;
            case "MENU-ERROR", "ERROR" -> soundError;
            default -> "";
        };
    }
}
