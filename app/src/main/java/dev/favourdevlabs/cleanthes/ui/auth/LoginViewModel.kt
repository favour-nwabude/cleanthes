package dev.favourdevlabs.cleanthes.ui.auth

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.security.BiometricHelper
import dev.favourdevlabs.cleanthes.domain.usecase.UnlockVaultUseCase
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal const val MAX_ATTEMPTS             = 5
internal const val LOCKOUT_DURATION_SECONDS = 30

sealed interface LoginEvent {
    data object NavigateToHome   : LoginEvent
    data object TriggerBiometric : LoginEvent
}

data class LoginUiState(
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val failedAttempts: Int = 0,
    val isLockedOut: Boolean = false,
    val lockoutSecondsRemaining: Int = 0,
    val showBiometricSection: Boolean = false,
    val isAuthenticating: Boolean = false,
    val shakeCounter: Int = 0,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    app: Application,
    private val sessionManager: SessionManager,
    private val unlockVault: UnlockVaultUseCase,
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var storedAuthSalt:        String? = null
    private var storedEncSalt:         String? = null
    private var storedMasterHash:      String? = null
    private var storedBiometricSecret: String? = null
    private var failedAttempts                  = 0

    init { loadCredentials() }

    private fun loadCredentials() {
        try {
            val masterKey = MasterKey.Builder(getApplication<Application>())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                getApplication(),
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            storedAuthSalt        = prefs.getString(KEY_AUTH_SALT,        null)
            storedEncSalt         = prefs.getString(KEY_ENC_SALT,         null)
            storedMasterHash      = prefs.getString(KEY_MASTER_HASH,      null)
            storedBiometricSecret = prefs.getString(KEY_BIOMETRIC_SECRET, null)

            val biometricEnabled   = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
            val biometricAvailable = biometricEnabled &&
                BiometricHelper.isBiometricAvailable(getApplication())

            _uiState.update { it.copy(showBiometricSection = biometricAvailable) }
        } catch (_: Exception) { }
    }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun onPasswordVisibilityToggle() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun attemptPasswordUnlock() {
        val state = _uiState.value
        if (state.isLockedOut || state.isLoading) return
        if (state.password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter your master password") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch { verifyPassword(state.password) }
    }

    private suspend fun verifyPassword(attempt: String) {
        val authSalt   = storedAuthSalt   ?: return resetLoading("Vault data missing")
        val masterHash = storedMasterHash ?: return resetLoading("Vault data missing")
        try {
            val correct = withContext(Dispatchers.IO) {
                KeyDerivation.verifyMasterPassword(attempt.toCharArray(), authSalt, masterHash)
            }
            if (correct) {
                failedAttempts = 0
                deriveKeyAndNavigate(attempt, fromBiometric = false)
            } else {
                _uiState.update { it.copy(isLoading = false) }
                handleFailedAttempt()
            }
        } catch (_: Exception) {
            resetLoading("An error occurred. Please try again.")
        }
    }

    fun requestBiometricAuth() {
        val state = _uiState.value
        if (state.isAuthenticating || state.isLoading || state.isLockedOut) return
        _uiState.update { it.copy(isAuthenticating = true) }
        viewModelScope.launch { _events.send(LoginEvent.TriggerBiometric) }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(isAuthenticating = false, isLoading = true) }
        viewModelScope.launch { deriveKeyAndNavigate(null, fromBiometric = true) }
    }

    fun onBiometricFailure() =
        _uiState.update { it.copy(isAuthenticating = false) }

    fun onBiometricError(message: String) =
        _uiState.update { it.copy(isAuthenticating = false, errorMessage = message) }

private suspend fun deriveKeyAndNavigate(masterPassword: String?, fromBiometric: Boolean) {
        try {
            withContext(Dispatchers.IO) {
                val encSalt = storedEncSalt ?: throw IllegalStateException("Salt missing")
                val params  = if (fromBiometric)
                    UnlockVaultUseCase.Params.Biometric(storedBiometricSecret!!, encSalt)
                else
                    UnlockVaultUseCase.Params.Password(masterPassword!!, encSalt)
                unlockVault(params)
            }
            _events.send(LoginEvent.NavigateToHome)
        } catch (_: Exception) {
            _uiState.update {
                it.copy(isLoading = false, isAuthenticating = false,
                    errorMessage = "An error occurred. Please try again.")
            }
        }
    }

        private fun handleFailedAttempt() {
        failedAttempts++
        _uiState.update {
            it.copy(
                errorMessage   = "Wrong password",
                password       = "",
                failedAttempts = failedAttempts,
                shakeCounter   = it.shakeCounter + 1,
            )
        }
        if (failedAttempts >= MAX_ATTEMPTS) startLockout()
    }

    private fun startLockout() {
        _uiState.update { it.copy(isLockedOut = true) }
        viewModelScope.launch {
            var remaining = LOCKOUT_DURATION_SECONDS
            while (remaining > 0) {
                _uiState.update { it.copy(lockoutSecondsRemaining = remaining) }
                delay(1000)
                remaining--
            }
            failedAttempts = 0
            _uiState.update {
                it.copy(isLockedOut = false, lockoutSecondsRemaining = 0,
                    failedAttempts = 0, errorMessage = null)
            }
        }
    }

    private fun resetLoading(error: String) =
        _uiState.update { it.copy(isLoading = false, errorMessage = error) }
}
