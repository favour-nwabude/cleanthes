package dev.favourdevlabs.cleanthes.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntryUseCase
import dev.favourdevlabs.cleanthes.security.TOTPGenerator
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val shouldFinish: Boolean = false,
    val title: String = "",
    val category: String = "",
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val website: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val hasTOTP: Boolean = false,
    val totpCode: String = "",
    val totpPeriod: Int = 30,
    val totpSecondsRemaining: Int = 30,
) {
    val displayPassword: String
        get() = if (passwordVisible) password else "••••••••••••"
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getVaultEntry: GetVaultEntryUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var currentEntry: VaultEntry? = null
    private var totpJob: Job? = null

    fun loadEntry(entryId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val key = sessionManager.getSessionKey() ?: run {
                    _uiState.update { it.copy(shouldFinish = true) }
                    return@launch
                }
                val entry = withContext(Dispatchers.IO) { getVaultEntry(entryId, key) }
                
                if (entry == null) {
                    _uiState.update { it.copy(shouldFinish = true) }
                    return@launch
                }
                currentEntry = entry
                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        title           = entry.title               ?: "",
                        category        = entry.category            ?: "",
                        username        = entry.username            ?: "",
                        password        = entry.encryptedPassword   ?: "",
                        passwordVisible = false,
                        website         = entry.website?.takeIf     { w -> w.isNotEmpty() },
                        notes           = entry.notes?.takeIf       { n -> n.isNotEmpty() },
                        isFavorite      = entry.isFavorite,
                        hasTOTP         = entry.hasTOTP(),
                        totpPeriod      = entry.totpPeriod,
                    )
                }
                if (entry.hasTOTP()) startTotpUpdater(entry) else stopTotpUpdater()
            } catch (_: Exception) {
                _uiState.update { it.copy(shouldFinish = true) }
            }
        }
    }

    fun togglePasswordVisibility() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun pauseTotpUpdater()  = stopTotpUpdater()
    fun resumeTotpUpdater() = currentEntry?.let { if (it.hasTOTP()) startTotpUpdater(it) }

    private fun startTotpUpdater(entry: VaultEntry) {
        stopTotpUpdater()
        totpJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val secret = entry.totpSecret ?: break
                    val code = withContext(Dispatchers.IO) {
                        TOTPGenerator.generate(
                            secret,
                            entry.totpDigits,
                            entry.totpPeriod,
                            entry.totpAlgorithm,
                        )
                    }
                    val secsLeft = TOTPGenerator.getSecondsRemaining(entry.totpPeriod)
                    val display  = if (code.length == 6)
                        "${code.substring(0, 3)} ${code.substring(3)}" else code
                    _uiState.update { it.copy(totpCode = display, totpSecondsRemaining = secsLeft) }
                } catch (_: Exception) {
                    _uiState.update { it.copy(totpCode = "ERR") }
                }
                delay(1000)
            }
        }
    }

    private fun stopTotpUpdater() {
        totpJob?.cancel()
        totpJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTotpUpdater()
    }
}
