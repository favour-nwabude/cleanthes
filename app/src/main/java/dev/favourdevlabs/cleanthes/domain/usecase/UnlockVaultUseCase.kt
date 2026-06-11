package dev.favourdevlabs.cleanthes.domain.usecase

import android.util.Base64
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager
import javax.inject.Inject

class UnlockVaultUseCase @Inject constructor(
    private val sessionManager: SessionManager,
) {
    sealed interface Params {
        data class Password(val masterPassword: String, val encSalt: String) : Params
        data class Biometric(val biometricSecret: String, val encSalt: String) : Params
    }

    @Throws(Exception::class)
    operator fun invoke(params: Params) {
        val (source, encSalt) = when (params) {
            is Params.Password  -> params.masterPassword to params.encSalt
            is Params.Biometric -> params.biometricSecret to params.encSalt
        }
        val saltBytes = Base64.decode(encSalt, Base64.DEFAULT)
        val key       = KeyDerivation.deriveKey(source.toCharArray(), saltBytes)
        sessionManager.setSessionKey(key)
    }
}
