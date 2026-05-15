package dev.favourdevlabs.cleanthes.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository;
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager;

import java.util.List;
import java.util.ArrayList;
import javax.crypto.SecretKey;

public class HomeViewModel extends AndroidViewModel {

    private final MutableLiveData<List<VaultEntry>> filteredEntries = new MutableLiveData<>();

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final MutableLiveData<List<String>> categories = new MutableLiveData<>();

    private final MutableLiveData<Integer> entryCount = new MutableLiveData<>(0);

    private List<VaultEntry> masterList = new ArrayList<>();

    private String currentQuery = "";

    private String currentCategory = "All";

    private final VaultRepository repository;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = VaultRepository.getInstance(application);
    }

    public LiveData<List<VaultEntry>> getFilteredEntries() {
        return filteredEntries;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<List<String>> getCategories() {
        return categories;
    }

    public LiveData<Integer> getEntryCount() {
        return entryCount;
    }

    public void loadEntries() {
        SecretKey key = SessionManager.getSessionKey();
        if (key == null) {
            errorMessage.postValue("Session expired. Please Unlock again");
            return;
        }

        isLoading.postValue(true);

        new Thread(() -> {
            try {
                List<VaultEntry> all = repository.getAllEntries(key);
                masterList = all;

                entryCount.postValue(all.size());

                List<String> cats = repository.getAllCategories();
                categories.postValue(cats);

                applyFilter();

            } catch (Exception e) {
                errorMessage.postValue("Failed to load entries: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void setSearchQuery(String query) {
        this.currentQuery = (query == null) ? "" : query.trim();
        applyFilter();
    }

    public void setCategory(String category) {
        this.currentCategory = category;
        applyFilter();
    }

    public void deleteEntry(long entryId) {
        new Thread(() -> {
            try {
                repository.deleteEntry(entryId);
                loadEntries();
            } catch (Exception e) {
                errorMessage.postValue("Failed to delete entry.");
            }
        }).start();
    }

    public void toggleFavorite(VaultEntry entry, String plainPassword) {
        SecretKey key = SessionManager.getSessionKey();
        if (key == null)
            return;

        entry.setFavorite(!entry.isFavorite());

        new Thread(() -> {
            try {
                repository.updateEntry(entry, plainPassword, key);
                loadEntries();
            } catch (Exception e) {
                errorMessage.postValue("Failed to update entry");
            }
        }).start();
    }

    private void applyFilter() {
        List<VaultEntry> result = new ArrayList<>();

        for (VaultEntry entry : masterList) {
            boolean matchesCategory = currentCategory.equals("All")
                    || entry.getCategory().equalsIgnoreCase(currentCategory);

            boolean matchesQuery = currentQuery.isEmpty()
                    || entry.getTitle().toLowerCase().contains(currentQuery.toLowerCase())
                    || entry.getUsername().toLowerCase().contains(currentQuery.toLowerCase());

            if (matchesCategory && matchesQuery) {
                result.add(entry);
            }
        }

        filteredEntries.postValue(result);
    }
}
