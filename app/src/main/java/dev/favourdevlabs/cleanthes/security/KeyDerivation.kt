package dev.favourdevlabs.cleanthes.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object KeyDerivation {

    private const val ALGORITHM        = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS       = 310_000
    private const val KEY_LENGTH_BITS  = 256
    private const val SALT_LENGTH_BYTES = 16

    data class StoredHash(val saltBase64: String, val hashBase64: String)

    @Throws(Exception::class)
    fun deriveKey(masterPassword: CharArray, salt: ByteArray): SecretKey {
        val spec    = PBEKeySpec(masterPassword, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }

    @Throws(Exception::class)
    fun hashPassword(masterPassword: CharArray): StoredHash {
        val salt    = generateSalt()
        val spec    = PBEKeySpec(masterPassword, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash    = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return StoredHash(
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        )
    }

    @Throws(Exception::class)
    fun verifyMasterPassword(
        masterPassword: CharArray,
        storedSaltB64: String,
        storedHashB64: String
    ): Boolean {
        val salt        = Base64.decode(storedSaltB64, Base64.NO_WRAP)
        val storedHash  = Base64.decode(storedHashB64, Base64.NO_WRAP)
        val spec        = PBEKeySpec(masterPassword, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory     = SecretKeyFactory.getInstance(ALGORITHM)
        val attemptHash = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return MessageDigest.isEqual(storedHash, attemptHash)
    }

    fun generateSalt(): ByteArray =
        ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
}
