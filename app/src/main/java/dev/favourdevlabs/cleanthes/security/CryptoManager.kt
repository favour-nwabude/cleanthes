package dev.favourdevlabs.cleanthes.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {

    private const val TRANSFORMATION      = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES     = 12

    @Throws(Exception::class)
    fun encrypt(plaintext: String, secretKey: SecretKey): String {
        val iv = generateIV()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val ivPlusCiphertext = ByteArray(IV_LENGTH_BYTES + ciphertext.size)
        System.arraycopy(iv, 0, ivPlusCiphertext, 0, IV_LENGTH_BYTES)
        System.arraycopy(ciphertext, 0, ivPlusCiphertext, IV_LENGTH_BYTES, ciphertext.size)

        return Base64.encodeToString(ivPlusCiphertext, Base64.NO_WRAP)
    }

    @Throws(Exception::class)
    fun decrypt(encryptedData: String, secretKey: SecretKey): String {
        val ivPlusCiphertext = Base64.decode(encryptedData, Base64.NO_WRAP)

        val iv         = ivPlusCiphertext.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = ivPlusCiphertext.copyOfRange(IV_LENGTH_BYTES, ivPlusCiphertext.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun generateIV(): ByteArray =
        ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
}
