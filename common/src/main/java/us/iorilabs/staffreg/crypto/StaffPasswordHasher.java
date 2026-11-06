package us.iorilabs.staffreg.crypto;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.mindrot.jbcrypt.BCrypt;

public final class StaffPasswordHasher {

    private static final Argon2 ARGON2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String LEGACY_PREFIX = "pbkdf2$";

    private StaffPasswordHasher() {}

    public static String hash(String password) {
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("password");
        return ARGON2.hash(3, 65536, 1, password.toCharArray());
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null || stored.isBlank()) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            try {
                return BCrypt.checkpw(password, stored);
            } catch (RuntimeException ex) {
                return false;
            }
        }
        if (stored.startsWith("$argon2")) {
            try {
                return ARGON2.verify(stored, password.toCharArray());
            } catch (RuntimeException ex) {
                return false;
            }
        }
        if (stored.startsWith(LEGACY_PREFIX)) {
            return verifyLegacyPbkdf2(password, stored);
        }
        return false;
    }

    public static String hmacIp(String ip, String pepper) {
        if (ip == null || ip.isBlank()) return "";
        String material = (pepper == null ? "" : pepper) + "|" + ip.trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            return "";
        }
    }

    private static boolean verifyLegacyPbkdf2(String password, String stored) {
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) return false;
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("PBKDF2 unavailable", ex);
        }
    }
}
