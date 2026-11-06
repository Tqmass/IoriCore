package us.iorilabs.proxy.discord;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import us.iorilabs.staffreg.crypto.BridgeEventSigner;

public final class StaffAuthButtonCodec {

    public static final String PREFIX = "iori:staffauth:";

    private StaffAuthButtonCodec() {}

    public static String encodeApprove(String token, String secret, long expiresAtMillis, String targetDiscordId) {
        return encode("a", token, secret, expiresAtMillis, targetDiscordId);
    }

    public static String encodeDeny(String token, String secret, long expiresAtMillis, String targetDiscordId) {
        return encode("d", token, secret, expiresAtMillis, targetDiscordId);
    }

    private static String encode(
        String action,
        String token,
        String secret,
        long expiresAtMillis,
        String targetDiscordId
    ) {
        if (token == null || token.isBlank()) return "";
        String tid = targetDiscordId == null ? "" : targetDiscordId.trim();
        String body = token.trim().toLowerCase(Locale.ROOT) + "|" + action + "|" + expiresAtMillis + "|" + tid;
        String sig = BridgeEventSigner.sign(secret == null ? "" : secret, body);
        if (sig.isBlank()) return "";
        return PREFIX + action + ":" + token.trim().toLowerCase(Locale.ROOT) + ":" + sig;
    }

    public static Parsed parse(String customId) {
        if (customId == null || !customId.startsWith(PREFIX)) return null;
        String rest = customId.substring(PREFIX.length());
        int a = rest.indexOf(':');
        if (a < 1) return null;
        String action = rest.substring(0, a);
        String tail = rest.substring(a + 1);
        int b = tail.indexOf(':');
        if (b < 1) return null;
        String token = tail.substring(0, b).trim().toLowerCase(Locale.ROOT);
        String signature = tail.substring(b + 1);
        if (token.isBlank() || signature.isBlank()) return null;
        if (!"a".equals(action) && !"d".equals(action)) return null;
        return new Parsed(action, token, signature);
    }

    public static boolean verify(Parsed parsed, String secret, long expiresAtMillis, String targetDiscordId) {
        if (parsed == null) return false;
        String tid = targetDiscordId == null ? "" : targetDiscordId.trim();
        String body = parsed.token + "|" + parsed.action + "|" + expiresAtMillis + "|" + tid;
        return BridgeEventSigner.verify(secret == null ? "" : secret, body, parsed.signature);
    }

    public record Parsed(String action, String token, String signature) {}
}
