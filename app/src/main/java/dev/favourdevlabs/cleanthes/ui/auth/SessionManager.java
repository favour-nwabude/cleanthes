package dev.favourdevlabs.cleanthes.ui.auth;

import javax.crypto.SecretKey;

public class SessionManager {

    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    private static volatile SecretKey sessionKey = null;
    private static volatile long sessionStartTime = 0L;

    private SessionManager() {
    }

    public static void setSessionKey(SecretKey key) {
        sessionKey = key;
        sessionStartTime = System.currentTimeMillis();
    }

    public static SecretKey getSessionKey() {
        if (isSessionExpired()) {
            clearSession();
            return null;
        }
        return sessionKey;
    }

    public static boolean isUnlocked() {
        if (isSessionExpired()) {
            clearSession();
            return false;
        }
        return sessionKey != null;
    }

    public static void refreshSession() {
        if (sessionKey != null) {
            sessionStartTime = System.currentTimeMillis();
        }
    }

    public static void clearSession() {
        sessionKey = null;
        sessionStartTime = 0L;
    }

    private static boolean isSessionExpired() {
        if (sessionKey == null)
            return true;
        return (System.currentTimeMillis() - sessionStartTime) > SESSION_TIMEOUT_MS;
    }
}
