package us.iorilabs.bukkit.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;

public final class ColorUtil {

    public static final int HEX_MIN_PROTOCOL = 735;

    private static final char SECTION = ChatColor.COLOR_CHAR;
    private static final Pattern HEX = Pattern.compile("&#([0-9A-Fa-f]{6})|<#([0-9A-Fa-f]{6})>");
    private static final boolean SERVER_HEX = detectServerHex();

    private static final int[][] LEGACY_RGB = {
        { 0x00, 0x00, 0x00 },
        { 0x00, 0x00, 0xAA },
        { 0x00, 0xAA, 0x00 },
        { 0x00, 0xAA, 0xAA },
        { 0xAA, 0x00, 0x00 },
        { 0xAA, 0x00, 0xAA },
        { 0xFF, 0xAA, 0x00 },
        { 0xAA, 0xAA, 0xAA },
        { 0x55, 0x55, 0x55 },
        { 0x55, 0x55, 0xFF },
        { 0x55, 0xFF, 0x55 },
        { 0x55, 0xFF, 0xFF },
        { 0xFF, 0x55, 0x55 },
        { 0xFF, 0x55, 0xFF },
        { 0xFF, 0xFF, 0x55 },
        { 0xFF, 0xFF, 0xFF },
    };
    private static final char[] LEGACY_CODES = "0123456789abcdef".toCharArray();

    private ColorUtil() {}

    public static boolean serverHex() {
        return SERVER_HEX;
    }

    public static boolean hexFor(int protocol) {
        return SERVER_HEX && protocol >= HEX_MIN_PROTOCOL;
    }

    public static String translate(String input) {
        return translate(input, SERVER_HEX);
    }

    public static String translate(String input, int protocol) {
        return translate(input, hexFor(protocol));
    }

    public static String translate(String input, boolean hex) {
        if (input == null || input.isEmpty()) return "";
        return ChatColor.translateAlternateColorCodes('&', hex(input, hex));
    }

    public static String hex(String input, boolean hex) {
        if (input == null || input.indexOf('#') < 0) return input == null ? "" : input;
        Matcher matcher = HEX.matcher(input);
        StringBuffer out = new StringBuffer(input.length() + 16);
        while (matcher.find()) {
            String rgb = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(out, Matcher.quoteReplacement(hex ? sectionHex(rgb) : nearestLegacy(rgb)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public static String nearestLegacy(String rgb) {
        int value = Integer.parseInt(rgb, 16);
        int r = (value >> 16) & 0xFF;
        int g = (value >> 8) & 0xFF;
        int b = value & 0xFF;
        int best = 15;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < LEGACY_RGB.length; i++) {
            long dr = r - LEGACY_RGB[i][0];
            long dg = g - LEGACY_RGB[i][1];
            long db = b - LEGACY_RGB[i][2];
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return String.valueOf(SECTION) + LEGACY_CODES[best];
    }

    private static String sectionHex(String rgb) {
        StringBuilder builder = new StringBuilder(14).append(SECTION).append('x');
        for (char c : rgb.toLowerCase().toCharArray()) {
            builder.append(SECTION).append(c);
        }
        return builder.toString();
    }

    private static boolean detectServerHex() {
        try {
            Class.forName("net.md_5.bungee.api.ChatColor").getMethod("of", String.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}
