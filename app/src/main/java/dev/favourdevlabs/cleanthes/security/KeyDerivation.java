package dev.favourdevlabs.cleanthes.security;

import android.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

public class KeyDerivation {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int ITERATIONS = 310_000;

    private static final int KEY_LENGTH_BITS = 256;

    private static final int SALT_LENGTH_BYTES = 16;

    public static SecretKey deriveKey(char[] masterPassword, byte[] salt) throws Exception {

        PBEKeySpec spec = new PBEKeySpec(masterPassword, salt, ITERATIONS, KEY_LENGTH_BITS);

        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);

        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        spec.clearPassword();

        return new SecretKeySpec(keyBytes, "AES");
    }

    public static StoredHash hashPassword(char[] masterPassword) throws Exception {

        byte[] salt = generateSalt();

        PBEKeySpec spec = new PBEKeySpec(masterPassword, salt, ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] hash = factory.generateSecret(spec).getEncoded();
        spec.clearPassword();

        return new StoredHash(
                Base64.encodeToString(salt, Base64.NO_WRAP),
                Base64.encodeToString(hash, Base64.NO_WRAP));
    }

    public static boolean verifyMasterPassword(char[] masterPassword, String storedSaltB64, String storedHashB64)
            throws Exception {

        byte[] salt = Base64.decode(storedSaltB64, Base64.NO_WRAP);
        byte[] storedHash = Base64.decode(storedHashB64, Base64.NO_WRAP);

        PBEKeySpec spec = new PBEKeySpec(masterPassword, salt, ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] attemptHash = factory.generateSecret(spec).getEncoded();
        spec.clearPassword();

        return java.security.MessageDigest.isEqual(storedHash, attemptHash);
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static class StoredHash {

        public final String saltBase64;
        public final String hashBase64;

        public StoredHash(String saltBase64, String hashBase64) {
            this.saltBase64 = saltBase64;
            this.hashBase64 = hashBase64;
        }
    }
}
