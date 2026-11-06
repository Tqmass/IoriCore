package us.iorilabs.bukkit.security;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {

    private static final int ROUNDS = 12;

    private PasswordHasher() {}

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(ROUNDS));
    }

    public static boolean verify(String password, String hash) {
        if (hash == null || password == null) return false;
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
