package us.iorilabs.bukkit.modules.lobby;

import java.awt.Color;
import java.util.Locale;

public enum LobbyGlowColor {
    BLACK("black", "&0Black", 15),
    DARK_BLUE("dark_blue", "&1Dark Blue", 11),
    DARK_GREEN("dark_green", "&2Dark Green", 13),
    DARK_AQUA("dark_aqua", "&3Dark Aqua", 9),
    DARK_RED("dark_red", "&4Dark Red", 14),
    DARK_PURPLE("dark_purple", "&5Dark Purple", 10),
    GOLD("gold", "&6Gold", 1),
    GRAY("gray", "&7Gray", 7),
    DARK_GRAY("dark_gray", "&8Dark Gray", 8),
    BLUE("blue", "&9Blue", 11),
    GREEN("green", "&aGreen", 5),
    AQUA("aqua", "&bAqua", 9),
    RED("red", "&cRed", 14),
    LIGHT_PURPLE("light_purple", "&dLight Purple", 2),
    YELLOW("yellow", "&eYellow", 4),
    WHITE("white", "&fWhite", 0);

    private final String key;
    private final String display;
    private final byte woolData;

    LobbyGlowColor(String key, String display, int woolData) {
        this.key = key;
        this.display = display;
        this.woolData = (byte) woolData;
    }

    public String key() {
        return key;
    }

    public String display() {
        return display;
    }

    public byte woolData() {
        return woolData;
    }

    public Color toColor() {
        return switch (this) {
            case BLACK -> new Color(0, 0, 0);
            case DARK_BLUE -> new Color(0, 0, 170);
            case DARK_GREEN -> new Color(0, 170, 0);
            case DARK_AQUA -> new Color(0, 170, 170);
            case DARK_RED -> new Color(170, 0, 0);
            case DARK_PURPLE -> new Color(170, 0, 170);
            case GOLD -> new Color(255, 170, 0);
            case GRAY -> new Color(170, 170, 170);
            case DARK_GRAY -> new Color(85, 85, 85);
            case BLUE -> new Color(85, 85, 255);
            case GREEN -> new Color(85, 255, 85);
            case AQUA -> new Color(85, 255, 255);
            case RED -> new Color(255, 85, 85);
            case LIGHT_PURPLE -> new Color(255, 85, 255);
            case YELLOW -> new Color(255, 255, 85);
            case WHITE -> new Color(255, 255, 255);
        };
    }

    public static LobbyGlowColor fromKey(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (LobbyGlowColor color : values()) {
            if (color.key.equals(key) || color.name().equalsIgnoreCase(key)) return color;
        }
        return null;
    }

    public static Color parseNamed(String raw) {
        LobbyGlowColor glow = fromKey(raw);
        return glow == null ? null : glow.toColor();
    }
}
