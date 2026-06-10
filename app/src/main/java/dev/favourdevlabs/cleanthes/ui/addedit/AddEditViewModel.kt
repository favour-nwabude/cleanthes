package dev.favourdevlabs.cleanthes.ui.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import dev.favourdevlabs.cleanthes.security.OtpAuthParser
import dev.favourdevlabs.cleanthes.security.TOTPGenerator
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager
import dev.favourdevlabs.cleanthes.utils.PasswordGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AddEditEvent {
    data object NavigateBack : AddEditEvent
}

data class AddEditUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val website: String = "",
    val category: String = "",
    val notes: String = "",
    val totpSecret: String = "",
    val isFavorite: Boolean = false,
    val strengthScore: Int = 0,
    val errorMessage: String? = null,
    val showDeleteDialog: Boolean = false,
    // TOTP metadata — RFC 6238 defaults, overwritten on QR scan or entry load
    val totpAlgorithm: String = "SHA1",
    val totpDigits: Int = 6,
    val totpPeriod: Int = 30,
    val totpIssuer: String? = null,
)

class AddEditViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = VaultRepository.getInstance(getApplication())

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var existingEntry: VaultEntry? = null
    private var initialized  = false

    fun initForEntry(entryId: Long) {
        if (initialized) return
        initialized = true
        if (entryId == -1L) return  // new entry — defaults are fine

        _uiState.update { it.copy(isEditMode = true, isLoading = true) }
        viewModelScope.launch {
            try {
                val key = SessionManager.getSessionKey() ?: run {
                    _events.send(AddEditEvent.NavigateBack); return@launch
                }
                val entry = withContext(Dispatchers.IO) { repository.getEntryById(entryId, key) }
                if (entry == null) { _events.send(AddEditEvent.NavigateBack); return@launch }
                existingEntry = entry
                _uiState.update {
                    it.copy(
                        isLoading     = false,
                        title         = entry.title          ?: "",
                        username      = entry.username       ?: "",
                        password      = entry.encryptedPassword ?: "",
                        website       = entry.website        ?: "",
                        category      = entry.category       ?: "",
                        notes         = entry.notes          ?: "",
                        totpSecret    = entry.totpSecret     ?: "",
                        isFavorite    = entry.isFavorite,
                        totpAlgorithm = entry.totpAlgorithm,
                        totpDigits    = entry.totpDigits,
                        totpPeriod    = entry.totpPeriod,
                        totpIssuer    = entry.totpIssuer,
                        strengthScore = PasswordGenerator.evaluateStrength(entry.encryptedPassword ?: ""),
                    )
                }
            } catch (_: Exception) { _events.send(AddEditEvent.NavigateBack) }
        }
    }

    fun onTitleChange(v: String)    = _uiState.update { it.copy(title    = v, errorMessage = null) }
    fun onUsernameChange(v: String) = _uiState.update { it.copy(username = v, errorMessage = null) }
    fun onWebsiteChange(v: String)  = _uiState.update { it.copy(website  = v) }
    fun onCategoryChange(v: String) = _uiState.update { it.copy(category = v) }
    fun onNotesChange(v: String)    = _uiState.update { it.copy(notes    = v) }
    fun onFavoriteToggle(c: Boolean) = _uiState.update { it.copy(isFavorite = c) }
    fun showDeleteDialog(show: Boolean) = _uiState.update { it.copy(showDeleteDialog = show) }

    fun onPasswordChange(v: String) = _uiState.update {
        it.copy(
            password      = v,
            errorMessage  = null,
            strengthScore = PasswordGenerator.evaluateStrength(v),
        )
    }

    fun onPasswordVisibilityToggle() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onTotpSecretChange(v: String) =
        _uiState.update { it.copy(totpSecret = v, errorMessage = null) }

    fun onGeneratedPassword(password: String) = _uiState.update {
        it.copy(
            password      = password,
            passwordVisible = true,
            strengthScore = PasswordGenerator.evaluateStrength(password),
        )
    }

    fun onQrResult(rawContent: String) {
        try {
            val parsed = OtpAuthParser.parse(rawContent)
            _uiState.update {
                it.copy(
                    totpSecret    = parsed.secret,
                    totpAlgorithm = parsed.algorithm,
                    totpDigits    = parsed.digits,
                    totpPeriod    = parsed.period,
                    totpIssuer    = parsed.issuer,
                    errorMessage  = null,
                    title = if (!it.isEditMode && it.title.isEmpty() && parsed.issuer != null)
                        parsed.issuer else it.title,
                )
            }
        } catch (e: UnsupportedOperationException) {
            _uiState.update { it.copy(errorMessage = "HOTP is not supported — only TOTP (time-based) codes.") }
        } catch (_: Exception) {
            _uiState.update { it.copy(errorMessage = "Invalid QR code — not a valid 2FA setup code.") }
        }
    }

    fun attemptSave() {
        val s        = _uiState.value
        val title    = s.title.trim()
        val username = s.username.trim()
        val password = s.password.trim()
        val totpRaw  = s.totpSecret.trim().uppercase().replace("\\s+".toRegex(), "")

        val error = when {
            title.isEmpty()    -> "Title is required"
            username.isEmpty() -> "Username or Email is required"
            password.isEmpty() -> "Password is required"
            totpRaw.isNotEmpty() && !isValidTotp(totpRaw, s) ->
                "Invalid authenticator secret — check the Base32 code."
            else -> null
        }
        if (error != null) { _uiState.update { it.copy(errorMessage = error) }; return }

        val key = SessionManager.getSessionKey() ?: run {
            viewModelScope.launch { _events.send(AddEditEvent.NavigateBack) }; return
        }

        val finalTotp   = totpRaw.ifEmpty { null }
        val finalIssuer = if (finalTotp != null) s.totpIssuer else null

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (s.isEditMode && existingEntry != null) {
                        existingEntry!!.apply {
                            this.title         = title
                            this.username      = username
                            this.website       = s.website.trim().ifEmpty { null }
                            this.category      = s.category
                            this.notes         = s.notes.trim().ifEmpty { null }
                            this.isFavorite    = s.isFavorite
                            this.totpSecret    = finalTotp
                            this.totpIssuer    = finalIssuer
                            this.totpAlgorithm = s.totpAlgorithm
                            this.totpDigits    = s.totpDigits
                            this.totpPeriod    = s.totpPeriod
                        }
                        repository.updateEntry(existingEntry!!, password, key)
                    } else {
                        repository.addEntry(
                            title, username, password,
                            s.website.trim().ifEmpty { null },
                            s.category,
                            s.notes.trim().ifEmpty { null },
                            s.isFavorite, finalTotp, finalIssuer,
                            s.totpDigits, s.totpPeriod, s.totpAlgorithm, key,
                        )
                    }
                }
                _events.send(AddEditEvent.NavigateBack)
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to save. Please try again.") }
            }
        }
    }

    fun confirmDelete() {
        val entry = existingEntry ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.deleteEntry(entry.id) }
                _events.send(AddEditEvent.NavigateBack)
            } catch (_: Exception) {
                _uiState.update { it.copy(showDeleteDialog = false, errorMessage = "Failed to delete entry.") }
            }
        }
    }

    private fun isValidTotp(secret: String, s: AddEditUiState): Boolean = try {
        TOTPGenerator.generate(secret, s.totpDigits, s.totpPeriod, s.totpAlgorithm); true
    } catch (_: Exception) { false }
}
