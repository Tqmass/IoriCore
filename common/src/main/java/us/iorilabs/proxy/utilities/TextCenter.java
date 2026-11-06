package us.iorilabs.proxy.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextCenter {

    /** Approximate visible width of the server-list MOTD slot. */
    public static final int MOTD_WIDTH = 45;

    /** Default Minecraft chat line width (~53–55 visible chars). */
    private static final int DEFAULT_WIDTH = 54;
    private static final Pattern CENTER = Pattern.compile("(?is)<center>(.*?)</center>");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern LEGACY = Pattern.compile(
        "(?i)&#[0-9a-f]{6}|§#[0-9a-f]{6}|&#[0-9a-f]{6}|[&§]x([&§][0-9a-f]){6}|[&§][0-9a-fk-or]"
    );

    private TextCenter() {}

    public static String apply(String input) {
        return apply(input, DEFAULT_WIDTH);
    }

    public static String apply(String input, int width) {
        if (input == null || input.isEmpty()) return "";
        if (!input.toLowerCase().contains("<center>")) return input;

        Matcher matcher = CENTER.matcher(input);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String inner = matcher.group(1).strip();
            matcher.appendReplacement(out, Matcher.quoteReplacement(centerLine(inner, width)));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    /** Centers every line of a multi-line MOTD (server list). */
    public static String centerMotd(String block) {
        return centerMotd(block, MOTD_WIDTH);
    }

    public static String centerMotd(String block, int width) {
        if (block == null || block.isEmpty()) return "";

        String[] lines = block.split("\\R", -1);
        List<String> out = new ArrayList<>(lines.length);

        for (String line : lines) {
            out.add(centerLine(line.stripLeading(), width));
        }

        return String.join("\n", out);
    }

    public static String centerLine(String input, int width) {
        if (input == null) return "";
        if (input.isBlank()) return input;

        String plain = stripFormatting(input);
        int pad = Math.max(0, (width - plain.length()) / 2);
        if (pad == 0) return input;

        return " ".repeat(pad) + input;
    }

    public static String stripFormatting(String input) {
        if (input == null || input.isEmpty()) return "";

        String value = TAG.matcher(input).replaceAll("");
        return LEGACY.matcher(value).replaceAll("");
    }
}
