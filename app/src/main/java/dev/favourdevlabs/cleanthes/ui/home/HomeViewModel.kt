package dev.favourdevlabs.cleanthes.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _filteredEntries = MutableLiveData<List<VaultEntry>>()
    private val _errorMessage    = MutableLiveData<String>()
    private val _isLoading       = MutableLiveData(false)
    private val _categories      = MutableLiveData<List<String>>()
    private val _entryCount      = MutableLiveData(0)

    val filteredEntries: LiveData<List<VaultEntry>> = _filteredEntries
    val errorMessage:    LiveData<String>           = _errorMessage
    val isLoading:       LiveData<Boolean>          = _isLoading
    val categories:      LiveData<List<String>>     = _categories
    val entryCount:      LiveData<Int>              = _entryCount

    private val repository   = VaultRepository.getInstance(application)
    private var masterList   = emptyList<VaultEntry>()
    private var currentQuery    = ""
    private var currentCategory = "All"

    fun loadEntries() {
        val key = SessionManager.getSessionKey() ?: run {
            _errorMessage.postValue("Session expired. Please Unlock again")
            return
        }

        _isLoading.postValue(true)

        Thread {
            try {
                val all = repository.getAllEntries(key)
                masterList = all
                _entryCount.postValue(all.size)
                _categories.postValue(repository.getAllCategories())
                applyFilter()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to load entries: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }.start()
    }

    fun setSearchQuery(query: String?) {
        currentQuery = query?.trim() ?: ""
        applyFilter()
    }

    fun setCategory(category: String) {
        currentCategory = category
        applyFilter()
    }

    fun deleteEntry(entryId: Long) {
        Thread {
            try {
                repository.deleteEntry(entryId)
                loadEntries()
            } catch (_: Exception) {
                _errorMessage.postValue("Failed to delete entry.")
            }
        }.start()
    }

    fun toggleFavorite(entry: VaultEntry, plainPassword: String) {
        val key = SessionManager.getSessionKey() ?: return
        entry.isFavorite = !entry.isFavorite
        Thread {
            try {
                repository.updateEntry(entry, plainPassword, key)
                loadEntries()
            } catch (_: Exception) {
                _errorMessage.postValue("Failed to update entry")
            }
        }.start()
    }

    private fun applyFilter() {
        val result = masterList.filter { entry ->
            val matchesCategory = currentCategory == "All" ||
                entry.category.equals(currentCategory, ignoreCase = true)
            val matchesQuery = currentQuery.isEmpty() ||
                entry.title.lowercase().contains(currentQuery.lowercase()) ||
                entry.username.lowercase().contains(currentQuery.lowercase())
            matchesCategory && matchesQuery
        }
        _filteredEntries.postValue(result)
    }
}
