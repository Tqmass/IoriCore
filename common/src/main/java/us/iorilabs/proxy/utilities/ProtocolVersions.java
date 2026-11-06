package us.iorilabs.proxy.utilities;

import java.util.Locale;
import java.util.Map;

public final class ProtocolVersions {

    /** Corte UI legacy fijo: 1.12.2. No configurable. */
    public static final String LEGACY_MAX_VERSION = "1.12.2";
    public static final int LEGACY_MAX_PROTOCOL = 340;

    private static final Map<String, Integer> MAX_BY_VERSION = Map.ofEntries(
        Map.entry("1.8", 47),
        Map.entry("1.8.8", 47),
        Map.entry("1.8.9", 47),
        Map.entry("1.9", 107),
        Map.entry("1.9.4", 110),
        Map.entry("1.10", 210),
        Map.entry("1.10.2", 210),
        Map.entry("1.11", 315),
        Map.entry("1.11.2", 316),
        Map.entry("1.12", 335),
        Map.entry("1.12.1", 338),
        Map.entry("1.12.2", 340),
        Map.entry("1.13", 393),
        Map.entry("1.13.2", 404),
        Map.entry("1.14", 477),
        Map.entry("1.14.4", 498),
        Map.entry("1.15", 573),
        Map.entry("1.15.2", 578),
        Map.entry("1.16", 735),
        Map.entry("1.16.1", 736),
        Map.entry("1.16.2", 751),
        Map.entry("1.16.3", 753),
        Map.entry("1.16.4", 754),
        Map.entry("1.16.5", 754),
        Map.entry("1.17", 755),
        Map.entry("1.17.1", 756),
        Map.entry("1.18", 757),
        Map.entry("1.18.2", 758),
        Map.entry("1.19", 759),
        Map.entry("1.19.2", 760),
        Map.entry("1.19.3", 761),
        Map.entry("1.19.4", 762),
        Map.entry("1.20", 763),
        Map.entry("1.20.1", 763),
        Map.entry("1.20.2", 764),
        Map.entry("1.20.4", 765),
        Map.entry("1.20.5", 766),
        Map.entry("1.20.6", 766),
        Map.entry("1.21", 767),
        Map.entry("1.21.1", 767),
        Map.entry("1.21.3", 768),
        Map.entry("1.21.4", 769),
        Map.entry("1.21.5", 770)
    );

    private ProtocolVersions() {}

    public static boolean isLegacy(int protocol) {
        return protocol > 0 && protocol <= LEGACY_MAX_PROTOCOL;
    }

    public static int maxProtocol(String versionOrProtocol) {
        if (versionOrProtocol == null || versionOrProtocol.isBlank()) return LEGACY_MAX_PROTOCOL;

        String raw = versionOrProtocol.trim().toLowerCase(Locale.ROOT);
        if (raw.startsWith("v")) {
            raw = raw.substring(1);
        }

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {}

        Integer exact = MAX_BY_VERSION.get(raw);
        if (exact != null) return exact;

        int lastDot = raw.lastIndexOf('.');
        if (lastDot > 0) {
            Integer majorMinor = MAX_BY_VERSION.get(raw.substring(0, lastDot));
            if (majorMinor != null) return majorMinor;
        }

        if (raw.matches("1\\.\\d+\\.x")) {
            Integer major = MAX_BY_VERSION.get(raw.substring(0, raw.length() - 2));
            if (major != null) return major;
        }

        return LEGACY_MAX_PROTOCOL;
    }
}
