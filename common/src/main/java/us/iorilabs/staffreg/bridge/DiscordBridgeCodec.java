package us.iorilabs.staffreg.bridge;

import java.util.Locale;
import us.iorilabs.staffreg.crypto.BridgeEventSigner;

public final class DiscordBridgeCodec {

    private DiscordBridgeCodec() {}

    public static void sign(DiscordBridgeEvent event, String secret) {
        if (event == null) return;
        event.signature(BridgeEventSigner.sign(secret, event.canonicalPayload()));
    }

    public static boolean verify(DiscordBridgeEvent event, String secret, boolean required) {
        if (event == null) return false;
        if (!required) return true;
        return BridgeEventSigner.verify(secret, event.canonicalPayload(), event.signature());
    }

    public static String toWire(DiscordBridgeEvent event) {
        if (event == null || event.type() == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("id=")
            .append(event.eventId())
            .append(";type=")
            .append(event.type().name())
            .append(";ts=")
            .append(event.timestamp())
            .append(";sig=")
            .append(nullToEmpty(event.signature()));
        for (var entry : event.fields().entrySet()) {
            sb.append(';').append(escape(entry.getKey())).append('=').append(escape(entry.getValue()));
        }
        return sb.toString();
    }

    public static DiscordBridgeEvent fromWire(String wire) {
        if (wire == null || wire.isBlank()) return null;
        DiscordBridgeEvent event = new DiscordBridgeEvent();
        String[] parts = wire.split(";");
        for (String part : parts) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String key = unescape(part.substring(0, eq));
            String value = unescape(part.substring(eq + 1));
            switch (key) {
                case "id" -> event.eventId(value);
                case "type" -> {
                    try {
                        event.type(BridgeEventType.valueOf(value.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                }
                case "ts" -> {
                    try {
                        event.timestamp(Long.parseLong(value));
                    } catch (NumberFormatException ignored) {}
                }
                case "sig" -> event.signature(value);
                default -> event.put(key, value);
            }
        }
        return event.type() == null ? null : event;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace(";", "\\;").replace("=", "\\=");
    }

    private static String unescape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                out.append(value.charAt(++i));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
