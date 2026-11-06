package us.iorilabs.bukkit.menu.enums;

import java.util.Locale;

public enum MenuActionType {
    PLAYER,
    CONSOLE,
    MESSAGE,
    OPEN,
    BACK,
    CLOSE,
    SOUND,
    INTERNAL,
    REFRESH,
    PERMISSION,
    PAGE;

    public static MenuActionType parse(String raw) {
        if (raw == null) return null;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
