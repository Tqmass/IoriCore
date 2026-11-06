package us.iorilabs.proxy.placeholders;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderTokens {

    private static final Pattern TOKEN = Pattern.compile("%ioriproxy_([a-z0-9_]+)%", Pattern.CASE_INSENSITIVE);

    private PlaceholderTokens() {}

    public static String replace(String input, Function<String, String> resolver) {
        if (input == null || input.isBlank()) return input;

        Function<String, String> safe = resolver == null ? key -> "" : resolver;
        Matcher matcher = TOKEN.matcher(input);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(safe.apply(key)));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
