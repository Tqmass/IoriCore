package us.iorilabs.bukkit.menu.enums;

import java.util.Locale;

public enum MenuVariant {
    MODERN,
    LEGACY;

    public String folder() {
        return name().toLowerCase(Locale.ROOT);
    }

    public MenuVariant other() {
        return this == MODERN ? LEGACY : MODERN;
    }
}
