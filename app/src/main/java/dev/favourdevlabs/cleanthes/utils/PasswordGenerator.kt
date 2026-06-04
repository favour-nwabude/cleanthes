package dev.favourdevlabs.cleanthes.utils

import java.security.SecureRandom

object PasswordGenerator {

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS    = "0123456789"
    private const val SPECIAL   = "!@#\$%^&*()-_=+[]{}|;:,.<>?"

    const val DEFAULT_LENGTH    = 16
    const val DEFAULT_UPPERCASE = true
    const val DEFAULT_LOWERCASE = true
    const val DEFAULT_DIGITS    = true
    const val DEFAULT_SPECIAL   = true

    private const val MIN_LENGTH = 8
    private const val MAX_LENGTH = 64

    private val SECURE_RANDOM = SecureRandom()

    fun generate(): String =
        generate(DEFAULT_LENGTH, DEFAULT_UPPERCASE, DEFAULT_LOWERCASE, DEFAULT_DIGITS, DEFAULT_SPECIAL)

    fun generate(
        length: Int,
        useUppercase: Boolean,
        useLowercase: Boolean,
        useDigits: Boolean,
        useSpecial: Boolean
    ): String {
        val len = length.coerceIn(MIN_LENGTH, MAX_LENGTH)

        val pool      = StringBuilder()
        val guaranteed = StringBuilder()

        if (useUppercase) { pool.append(UPPERCASE); guaranteed.append(randomCharFrom(UPPERCASE)) }
        if (useLowercase) { pool.append(LOWERCASE); guaranteed.append(randomCharFrom(LOWERCASE)) }
        if (useDigits)    { pool.append(DIGITS);    guaranteed.append(randomCharFrom(DIGITS)) }
        if (useSpecial)   { pool.append(SPECIAL);   guaranteed.append(randomCharFrom(SPECIAL)) }

        require(pool.isNotEmpty()) { "At least one character category must be enabled." }

        val password = CharArray(len)
        val guaranteedCount = minOf(guaranteed.length, len)
        for (i in 0 until guaranteedCount) password[i] = guaranteed[i]

        val poolStr = pool.toString()
        for (i in guaranteedCount until len) password[i] = randomCharFrom(poolStr)

        fisherYatesShuffle(password)
        return String(password)
    }

    fun evaluateStrength(password: String?): Int {
        if (password.isNullOrEmpty()) return 0
        var score = 0
        if (password.length > 8)                            score++
        if (password.contains(Regex("[A-Z]")))              score++
        if (password.contains(Regex("\\d")))                score++
        if (password.contains(Regex("[!@#\$%^&*()\\-_=+\\[\\]{}|;:,.<>?]"))) score++
        if (password.length >= 16)                          score++
        return score
    }

    private fun randomCharFrom(pool: String): Char = pool[SECURE_RANDOM.nextInt(pool.length)]

    private fun fisherYatesShuffle(array: CharArray) {
        for (i in array.size - 1 downTo 1) {
            val j = SECURE_RANDOM.nextInt(i + 1)
            val temp = array[i]; array[i] = array[j]; array[j] = temp
        }
    }
}
