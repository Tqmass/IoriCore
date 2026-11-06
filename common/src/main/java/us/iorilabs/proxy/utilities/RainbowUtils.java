package us.iorilabs.proxy.utilities;

import java.util.concurrent.ThreadLocalRandom;

public final class RainbowUtils {

    private static final char[] CODES = { 'a', 'b', 'c', 'd', 'e', '6', '9', 'd', '5', '3' };

    private RainbowUtils() {}

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder out = new StringBuilder(text.length() * 3);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                out.append(ch);
                continue;
            }

            out.append('&').append(CODES[random.nextInt(CODES.length)]).append(ch);
        }

        return out.toString();
    }
}
