package dev.favourdevlabs.cleanthes.ui.auth

import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    companion object {
        private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L
    }

    @Volatile private var sessionKey: SecretKey? = null
    @Volatile private var sessionStartTime: Long = 0L

    fun setSessionKey(key: SecretKey) {
        sessionKey       = key
        sessionStartTime = System.currentTimeMillis()
    }

    fun getSessionKey(): SecretKey? {
        if (isSessionExpired()) {
            clearSession()
            return null
        }
        return sessionKey
    }

    fun isUnlocked(): Boolean {
        if (isSessionExpired()) {
            clearSession()
            return false
        }
        return sessionKey != null
    }

    fun refreshSession() {
        if (sessionKey != null) sessionStartTime = System.currentTimeMillis()
    }

    fun clearSession() {
        sessionKey       = null
        sessionStartTime = 0L
    }

    private fun isSessionExpired(): Boolean {
        if (sessionKey == null) return true
        return (System.currentTimeMillis() - sessionStartTime) > SESSION_TIMEOUT_MS
    }
}
