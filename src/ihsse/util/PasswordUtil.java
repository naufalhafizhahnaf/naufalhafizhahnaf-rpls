package ihsse.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitas hash password SHA-256.
 * Password asli tidak pernah disimpan di database.
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String sha256(String teks) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(teks.getBytes(StandardCharsets.UTF_8));
            StringBuilder hasil = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hasil.append(String.format("%02x", b));
            }
            return hasil.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritma SHA-256 tidak tersedia.", e);
        }
    }

    public static boolean cocok(String passwordAsli, String hashTersimpan) {
        if (passwordAsli == null || hashTersimpan == null) {
            return false;
        }
        String hashInput = sha256(passwordAsli);
        return MessageDigest.isEqual(
                hashInput.getBytes(StandardCharsets.UTF_8),
                hashTersimpan.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
