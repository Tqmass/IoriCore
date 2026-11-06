package us.iorilabs.staffreg.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;
import us.iorilabs.staffreg.StaffRegEnums.ButtonAction;

public final class ButtonIdSigner {

    private static final String SEP = ":";

    private ButtonIdSigner() {}

    public static String customId(ButtonAction action, UUID challengeId, String token, String secret) {
        if (action == null || challengeId == null || token == null || token.isBlank()) {
            throw new IllegalArgumentException("button id parts");
        }
        String body = action.name() + SEP + challengeId + SEP + token;
        String sig = BridgeEventSigner.sign(secret == null ? "" : secret, body);
        return "sr" + SEP + body + SEP + sig;
    }

    public static ParsedButton parse(String customId, String secret) {
        if (customId == null || !customId.startsWith("sr" + SEP)) return null;
        String[] parts = customId.split(SEP);
        if (parts.length != 5) return null;
        ButtonAction action;
        try {
            action = ButtonAction.valueOf(parts[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
        UUID challengeId;
        try {
            challengeId = UUID.fromString(parts[2]);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        String token = parts[3];
        String signature = parts[4];
        String body = action.name() + SEP + challengeId + SEP + token;
        if (!BridgeEventSigner.verify(secret == null ? "" : secret, body, signature)) return null;
        return new ParsedButton(action, challengeId, token);
    }

    public static String hashToken(String token) {
        if (token == null || token.isBlank()) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            return "";
        }
    }

    public record ParsedButton(ButtonAction action, UUID challengeId, String token) {}
}
