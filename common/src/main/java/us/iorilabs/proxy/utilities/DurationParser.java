package us.iorilabs.proxy.utilities;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern TOKEN = Pattern.compile("(\\d+)([smhd])", Pattern.CASE_INSENSITIVE);

    private DurationParser() {}

    public static long parseMillis(String input) {
        Validate.notBlank(input, "duration");
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        Matcher matcher = TOKEN.matcher(normalized);
        long total = 0L;
        int consumed = 0;

        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1));
            char unit = matcher.group(2).charAt(0);
            switch (unit) {
                case 's':
                    total += TimeUnit.SECONDS.toMillis(amount);
                    break;
                case 'm':
                    total += TimeUnit.MINUTES.toMillis(amount);
                    break;
                case 'h':
                    total += TimeUnit.HOURS.toMillis(amount);
                    break;
                case 'd':
                    total += TimeUnit.DAYS.toMillis(amount);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown duration unit: " + unit);
            }

            consumed += matcher.group().length();
        }

        if (consumed == 0 || consumed != normalized.length()) {
            throw new IllegalArgumentException("Invalid duration: " + input);
        }

        return total;
    }

    public static String format(long millis) {
        if (millis < 0) {
            millis = 0;
        }

        long seconds = millis / 1000L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;
        StringBuilder builder = new StringBuilder();

        if (days > 0) {
            appendUnit(builder, days, "d");
        }

        if (hours > 0) {
            appendUnit(builder, hours, "h");
        }

        if (minutes > 0) {
            appendUnit(builder, minutes, "m");
        }

        if (seconds > 0 || builder.length() == 0) {
            appendUnit(builder, seconds, "s");
        }

        return builder.toString();
    }

    private static void appendUnit(StringBuilder builder, long amount, String unit) {
        if (builder.length() > 0) builder.append(' ');

        builder.append(amount).append(unit);
    }
}
