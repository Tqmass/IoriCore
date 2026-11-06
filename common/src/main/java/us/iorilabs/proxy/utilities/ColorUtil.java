package us.iorilabs.proxy.utilities;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final ANSIComponentSerializer ANSI = ANSIComponentSerializer.ansi();

    private static final Pattern HEX_AMP = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final Pattern HEX_ANGLE = Pattern.compile("(?i)<#([0-9a-f]{6})>");
    private static final Pattern LEGACY_CODE = Pattern.compile("(?i)&([0-9a-fk-or])");

    private ColorUtil() {}

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) return Component.empty();

        String normalized = toMiniMessage(input);

        try {
            return MINI.deserialize(normalized);
        } catch (Exception ignored) {
            return LEGACY_AMP.deserialize(input.replace('§', '&'));
        }
    }

    public static String toLegacySection(String input) {
        return LEGACY_SECTION.serialize(parse(input));
    }

    /** Console / terminal colors (ANSI), same approach LuckPerms uses — not § codes. */
    public static String toAnsi(String input) {
        if (input == null || input.isEmpty()) return "";

        return ANSI.serialize(parse(input));
    }

    public static String toMiniMessage(String input) {
        if (input == null || input.isEmpty()) return "";

        String value = input.replace('§', '&');
        Matcher hexAmp = HEX_AMP.matcher(value);
        StringBuffer hexAmpOut = new StringBuffer();

        while (hexAmp.find()) {
            hexAmp.appendReplacement(hexAmpOut, "<#" + hexAmp.group(1).toLowerCase(Locale.ROOT) + ">");
        }

        hexAmp.appendTail(hexAmpOut);
        value = hexAmpOut.toString();

        Matcher hexAngle = HEX_ANGLE.matcher(value);
        StringBuffer hexAngleOut = new StringBuffer();

        while (hexAngle.find()) {
            hexAngle.appendReplacement(hexAngleOut, "<#" + hexAngle.group(1).toLowerCase(Locale.ROOT) + ">");
        }

        hexAngle.appendTail(hexAngleOut);
        value = hexAngleOut.toString();

        Matcher legacy = LEGACY_CODE.matcher(value);
        StringBuffer legacyOut = new StringBuffer();

        while (legacy.find()) {
            legacy.appendReplacement(legacyOut, Matcher.quoteReplacement(legacyToMini(legacy.group(1))));
        }

        legacy.appendTail(legacyOut);
        return legacyOut.toString();
    }

    private static String legacyToMini(String code) {
        // Vanilla: color codes reset decorations. MiniMessage colors do not — emulate vanilla
        // so &7&m---\n&7text does not leave strikethrough on the next line.
        return switch (code.toLowerCase(Locale.ROOT)) {
            case "0" -> "<reset><black>";
            case "1" -> "<reset><dark_blue>";
            case "2" -> "<reset><dark_green>";
            case "3" -> "<reset><dark_aqua>";
            case "4" -> "<reset><dark_red>";
            case "5" -> "<reset><dark_purple>";
            case "6" -> "<reset><gold>";
            case "7" -> "<reset><gray>";
            case "8" -> "<reset><dark_gray>";
            case "9" -> "<reset><blue>";
            case "a" -> "<reset><green>";
            case "b" -> "<reset><aqua>";
            case "c" -> "<reset><red>";
            case "d" -> "<reset><light_purple>";
            case "e" -> "<reset><yellow>";
            case "f" -> "<reset><white>";
            case "k" -> "<obfuscated>";
            case "l" -> "<bold>";
            case "m" -> "<strikethrough>";
            case "n" -> "<underlined>";
            case "o" -> "<italic>";
            case "r" -> "<reset>";
            default -> "&" + code;
        };
    }
}
