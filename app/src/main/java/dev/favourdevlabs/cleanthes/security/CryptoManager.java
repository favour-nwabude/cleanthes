package dev.favourdevlabs.cleanthes.security;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;

public class CryptoManager {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final int IV_LENGTH_BYTES = 12;

    public static String encrypt(String plaintext, SecretKey secretKey) throws Exception {

        byte[] iv = generateIV();

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        byte[] ivPlusCiphertext = new byte[IV_LENGTH_BYTES + ciphertext.length];
        System.arraycopy(iv, 0, ivPlusCiphertext, 0, IV_LENGTH_BYTES);
        System.arraycopy(ciphertext, 0, ivPlusCiphertext, IV_LENGTH_BYTES, ciphertext.length);

        return Base64.encodeToString(ivPlusCiphertext, Base64.NO_WRAP);
    }

    public static String decrypt(String encryptedData, SecretKey secretKey) throws Exception {

        byte[] ivPlusCiphertext = Base64.decode(encryptedData, Base64.NO_WRAP);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        byte[] ciphertext = new byte[ivPlusCiphertext.length - IV_LENGTH_BYTES];
        System.arraycopy(ivPlusCiphertext, 0, iv, 0, IV_LENGTH_BYTES);
        System.arraycopy(ivPlusCiphertext, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext, "UTF-8");
    }

    private static byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
