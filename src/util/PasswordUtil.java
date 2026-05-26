package util;

/**
 * Utility class for hashing and checking passwords using BCrypt.
 */
public class PasswordUtil {

    /**
     * Hashes a plaintext password using BCrypt.
     *
     * @param plainPassword the plaintext password to hash
     * @return the hashed password
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * Verifies a plaintext password against a BCrypt hash.
     *
     * @param plainPassword the plaintext password to verify
     * @param hashedPassword the hashed password
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
