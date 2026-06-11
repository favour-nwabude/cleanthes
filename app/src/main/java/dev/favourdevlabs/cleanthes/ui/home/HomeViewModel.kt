package dev.favourdevlabs.cleanthes.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

import dev.favourdevlabs.cleanthes.domain.usecase.DeleteVaultEntryUseCase
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntriesUseCase
import dev.favourdevlabs.cleanthes.domain.usecase.SaveVaultEntryUseCase

data class HomeUiState(
    val isLoading: Boolean = false,
    val entries: List<VaultEntry> = emptyList(),
    val categories: List<String> = emptyList(),
    val entryCount: Int = 0,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val pendingDeleteIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
) {
    val filteredEntries: List<VaultEntry>
        get() = entries
            .filter { it.id !in pendingDeleteIds }
            .filter { entry ->
                (selectedCategory == "All" ||
                        entry.category.equals(selectedCategory, ignoreCase = true)) &&
                (searchQuery.isEmpty() ||
                        entry.title.lowercase().contains(searchQuery.lowercase()) ||
                        (entry.username?.lowercase()?.contains(searchQuery.lowercase()) == true))
            }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getVaultEntries: GetVaultEntriesUseCase,
    private val deleteVaultEntry: DeleteVaultEntryUseCase,
    private val saveVaultEntry: SaveVaultEntryUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

   fun loadEntries() {
        val key = sessionManager.getSessionKey() ?: run {
            _uiState.update { it.copy(errorMessage = "Session expired. Please unlock again.") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { getVaultEntries(key) }
                _uiState.update {
                    it.copy(
                        isLoading  = false,
                        entries    = result.entries,
                        categories = result.categories,
                        entryCount = result.entries.size,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load entries: ${e.message}")
                }
            }
        }
    } 

    fun setSearchQuery(query: String) =
        _uiState.update { it.copy(searchQuery = query.trim()) }

    fun setCategory(category: String) =
        _uiState.update { it.copy(selectedCategory = category) }

    fun onEntrySwipedToDelete(entryId: Long) =
        _uiState.update { it.copy(pendingDeleteIds = it.pendingDeleteIds + entryId) }

    fun undoDelete(entryId: Long) =
        _uiState.update { it.copy(pendingDeleteIds = it.pendingDeleteIds - entryId) }

   fun confirmDelete(entryId: Long) {
        _uiState.update { it.copy(pendingDeleteIds = it.pendingDeleteIds - entryId) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { deleteVaultEntry(entryId) }
                loadEntries()
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete entry.") }
            }
        }
    } 

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

   fun toggleFavorite(entry: VaultEntry, plainPassword: String) {
        val key = sessionManager.getSessionKey() ?: return
        entry.isFavorite = !entry.isFavorite
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    saveVaultEntry(SaveVaultEntryUseCase.Params.Edit(entry, plainPassword, key))
                }
                loadEntries()
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update entry.") }
            }
        }
    } 
}
