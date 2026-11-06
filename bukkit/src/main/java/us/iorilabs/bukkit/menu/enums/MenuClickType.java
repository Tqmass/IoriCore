package us.iorilabs.bukkit.menu.enums;

import java.util.Locale;
import org.bukkit.event.inventory.ClickType;

public enum MenuClickType {
    LEFT,
    RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    MIDDLE,
    DROP,
    NUMBER,
    ANY;

    public static MenuClickType of(ClickType click) {
        if (click == null) return ANY;
        switch (click) {
            case LEFT:
                return LEFT;
            case RIGHT:
                return RIGHT;
            case SHIFT_LEFT:
                return SHIFT_LEFT;
            case SHIFT_RIGHT:
                return SHIFT_RIGHT;
            case MIDDLE:
                return MIDDLE;
            case DROP:
            case CONTROL_DROP:
                return DROP;
            case NUMBER_KEY:
                return NUMBER;
            default:
                return ANY;
        }
    }

    public static MenuClickType parse(String raw) {
        if (raw == null) return null;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
