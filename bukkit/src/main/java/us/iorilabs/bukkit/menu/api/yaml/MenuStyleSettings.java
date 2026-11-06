package us.iorilabs.bukkit.menu.api.yaml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class MenuStyleSettings {

    private final Map<String, YamlMenuTheme> themes = new HashMap<>();
    private int separatorWidth = 26;

    public MenuStyleSettings(IoriCorePlugin plugin) {
        if (plugin == null || plugin.settings() == null) return;
        FileConfiguration root = plugin.settings();
        separatorWidth = Math.max(8, root.getInt("MENUS.STYLE.SEPARATOR-WIDTH", 26));
        ConfigurationSection themesSection = root.getConfigurationSection("MENUS.THEMES");
        if (themesSection == null) return;
        for (String key : themesSection.getKeys(false)) {
            ConfigurationSection t = themesSection.getConfigurationSection(key);
            if (t == null) continue;
            themes.put(key.trim().toLowerCase(Locale.ROOT), YamlMenuTheme.from(t));
        }
    }

    public int separatorWidth() {
        return separatorWidth;
    }

    public YamlMenuTheme theme(String name) {
        if (name == null || name.isEmpty()) return null;
        return themes.get(name.trim().toLowerCase(Locale.ROOT));
    }

    public Map<String, YamlMenuTheme> themes() {
        return Collections.unmodifiableMap(themes);
    }
}
