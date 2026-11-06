package us.iorilabs.staffreg.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class BridgeEventSigner {

    private BridgeEventSigner() {}

    public static String sign(String secret, String payload) {
        if (secret == null || secret.isBlank() || payload == null) return "";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return "";
        }
    }

    public static boolean verify(String secret, String payload, String signature) {
        if (signature == null || signature.isBlank()) return false;
        String expected = sign(secret, payload == null ? "" : payload);
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
