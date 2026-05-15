package dev.favourdevlabs.cleanthes.utils;

import java.security.SecureRandom;

public class PasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    public static final int DEFAULT_LENGTH = 16;
    public static final boolean DEFAULT_UPPERCASE = true;
    public static final boolean DEFAULT_LOWERCASE = true;
    public static final boolean DEFAULT_DIGITS = true;
    public static final boolean DEFAULT_SPECIAL = true;

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generate() {
        return generate(
                DEFAULT_LENGTH,
                DEFAULT_UPPERCASE,
                DEFAULT_LOWERCASE,
                DEFAULT_DIGITS,
                DEFAULT_SPECIAL);
    }

    public static final String generate(int length,
            boolean useUppercase,
            boolean useLowercase,
            boolean useDigits,
            boolean useSpecial) {

        length = Math.max(MIN_LENGTH, Math.min(MAX_LENGTH, length));

        StringBuilder pool = new StringBuilder();
        StringBuilder guaranteed = new StringBuilder();

        if (useUppercase) {
            pool.append(UPPERCASE);
            guaranteed.append(randomCharFrom(UPPERCASE));
        }

        if (useLowercase) {
            pool.append(LOWERCASE);
            guaranteed.append(randomCharFrom(LOWERCASE));
        }

        if (useDigits) {
            pool.append(DIGITS);
            guaranteed.append(randomCharFrom(DIGITS));
        }

        if (useSpecial) {
            pool.append(SPECIAL);
            guaranteed.append(randomCharFrom(SPECIAL));
        }

        if (pool.length() == 0) {
            throw new IllegalArgumentException(
                    "At least one character category must be enabled.");
        }

        char[] password = new char[length];

        int guaranteedCount = guaranteed.length();
        for (int i = 0; i < guaranteedCount && i < length; i++) {
            password[i] = guaranteed.charAt(i);
        }

        String poolStr = pool.toString();
        for (int i = guaranteedCount; i < length; i++) {
            password[i] = randomCharFrom(poolStr);
        }

        fisherYatesShuffle(password);

        return new String(password);
    }

    public static int evaluateStrength(String password) {
        if (password == null || password.isEmpty())
            return 0;
        int score = 0;
        if (password.length() > 8)
            score++;
        if (password.matches(".*[A-Z].*"))
            score++;
        if (password.matches(".*\\d.*"))
            score++;
        if (password.matches(".*[!@#$%^&*()\\-_=+\\[\\]{}|;:,.<>?].*"))
            score++;
        if (password.length() >= 16)
            score++;
        return score;
    }

    private static char randomCharFrom(String pool) {
        return pool.charAt(SECURE_RANDOM.nextInt(pool.length()));
    }

    private static void fisherYatesShuffle(char[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
}
