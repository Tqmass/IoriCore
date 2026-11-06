package us.iorilabs.staffreg.crypto;

import java.security.SecureRandom;
import java.util.Locale;

public final class OtcGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private OtcGenerator() {}

    public static String generate(int length) {
        int size = Math.max(10, length);
        char[] out = new char[size];
        for (int i = 0; i < size; i++) {
            out[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(out);
    }

    public static String hash(String code) {
        return ButtonIdSigner.hashToken(code == null ? "" : code.trim().toUpperCase(Locale.ROOT));
    }
}
