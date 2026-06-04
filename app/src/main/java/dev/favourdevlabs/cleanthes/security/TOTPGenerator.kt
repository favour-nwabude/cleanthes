package dev.favourdevlabs.cleanthes.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * RFC 6238 TOTP — supports SHA1, SHA256, SHA512.
 * No Android imports. Fully unit-testable on bare JVM.
 */
object TOTPGenerator {

    private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    // --- Public API ----------------------------------------------------------

    /** Defaults: 6 digits, 30s window, SHA1. Covers ~99% of real-world services. */
    @Throws(Exception::class)
    fun generate(base32Secret: String): String = generate(base32Secret, 6, 30, "SHA1")

    /** Custom digits and period, SHA1 algorithm. */
    @Throws(Exception::class)
    fun generate(base32Secret: String, digits: Int, period: Int): String =
        generate(base32Secret, digits, period, "SHA1")

    /**
     * Full control. Algorithm is the value from the otpauth:// URI — "SHA1",
     * "SHA256", or "SHA512". Any unrecognised value falls back to SHA1.
     */
    @Throws(Exception::class)
    fun generate(base32Secret: String, digits: Int, period: Int, algorithm: String): String {
        val key      = decodeBase32(base32Secret)
        val timeStep = System.currentTimeMillis() / 1000L / period
        return hotp(key, timeStep, digits, toJavaMacAlgorithm(algorithm))
    }

    /** Seconds remaining in the current time window. Drives the countdown bar. */
    fun getSecondsRemaining(period: Int): Int {
        val epochSeconds = System.currentTimeMillis() / 1000L
        return (period - (epochSeconds % period)).toInt()
    }

    // --- HOTP core — RFC 4226 §5 --------------------------------------------

    @Throws(Exception::class)
    private fun hotp(key: ByteArray, counter: Long, digits: Int, macAlgorithm: String): String {
        // Counter as 8-byte big-endian
        val msg = ByteArray(8)
        var tmp = counter
        for (i in 7 downTo 0) {
            msg[i] = (tmp and 0xFF).toByte()
            tmp = tmp shr 8
        }

        val mac = Mac.getInstance(macAlgorithm)
        mac.init(SecretKeySpec(key, macAlgorithm))
        val hash = mac.doFinal(msg)

        // Dynamic truncation
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val p = ((hash[offset].toInt()     and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8)  or
                 (hash[offset + 3].toInt() and 0xFF)

        val otp = p % 10.0.pow(digits).toInt()
        return "%0${digits}d".format(otp)
    }

    // --- Algorithm mapping --------------------------------------------------

    /**
     * Maps the algorithm string from the otpauth:// URI to the Java Mac name.
     * The URI uses "SHA1", "SHA256", "SHA512".
     * Java's Mac.getInstance() expects "HmacSHA1", "HmacSHA256", "HmacSHA512".
     */
    private fun toJavaMacAlgorithm(otpAlgorithm: String?): String =
        when (otpAlgorithm?.uppercase()) {
            "SHA256" -> "HmacSHA256"
            "SHA512" -> "HmacSHA512"
            else     -> "HmacSHA1"
        }

    // --- Base32 decoder — RFC 4648 §6 ---------------------------------------

    @JvmStatic // kept for test compatibility — called from OtpAuthParser
    fun decodeBase32(input: String): ByteArray {
        val s   = input.uppercase().replace(Regex("[=\\s]"), "")
        val out = ByteArray(s.length * 5 / 8)
        var buf      = 0
        var bitsLeft = 0
        var idx      = 0

        for (ch in s) {
            val v = BASE32_CHARS.indexOf(ch)
            if (v < 0) throw IllegalArgumentException(
                "Invalid Base32 character '$ch'"
            )
            buf = (buf shl 5) or v
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out[idx++] = (buf shr bitsLeft).toByte()
            }
        }
        return out
    }
}
