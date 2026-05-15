package dev.favourdevlabs.cleanthes.data.repository;

import android.content.Context;

import dev.favourdevlabs.cleanthes.data.db.DatabaseHelper;
import dev.favourdevlabs.cleanthes.data.db.VaultDao;
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;
import dev.favourdevlabs.cleanthes.security.CryptoManager;

import java.util.List;
import javax.crypto.SecretKey;

public class VaultRepository {

    private final VaultDao vaultDao;

    private static VaultRepository instance;

    public static synchronized VaultRepository getInstance(Context context) {
        if (instance == null) {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context.getApplicationContext());
            instance = new VaultRepository(new VaultDao(dbHelper));
        }
        return instance;
    }

    public VaultRepository(VaultDao vaultDao) {
        this.vaultDao = vaultDao;
    }

    public long addEntry(String title, String userName, String plainPassword, String website, String category,
            String notes, boolean isFavorite, SecretKey secretKey) throws Exception {
        String encryptedPassword = CryptoManager.encrypt(plainPassword, secretKey);

        long now = System.currentTimeMillis();

        VaultEntry entry = new VaultEntry(title, userName, encryptedPassword, website, category, notes, isFavorite);
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);

        long newId = vaultDao.insert(entry);
        if (newId != -1) {
            entry.setId(newId);
        }
        return newId;
    }

    public int updateEntry(VaultEntry entry, String plainPassword, SecretKey secretKey) throws Exception {

        String encryptedPassword = CryptoManager.encrypt(plainPassword, secretKey);
        entry.setEncryptedPassword(encryptedPassword);
        entry.setUpdatedAt(System.currentTimeMillis());
        return vaultDao.update(entry);
    }

    public int deleteEntry(long id) {
        return vaultDao.deleteById(id);
    }

    public int wipeVault() {
        return vaultDao.deleteAll();
    }

    public List<VaultEntry> getAllEntries(SecretKey secretKey) throws Exception {
        List<VaultEntry> entries = vaultDao.getAllEntries();
        return decryptEntries(entries, secretKey);
    }

    public VaultEntry getEntryById(long id, SecretKey secretKey) throws Exception {
        VaultEntry entry = vaultDao.getEntryById(id);
        if (entry == null)
            return null;
        return decryptEntry(entry, secretKey);
    }

    public List<VaultEntry> searchEntries(String query, SecretKey secretKey) throws Exception {
        List<VaultEntry> entries = vaultDao.searchEntries(query);
        return decryptEntries(entries, secretKey);
    }

    public List<VaultEntry> getEntriesByCategory(String category, SecretKey secretKey) throws Exception {
        List<VaultEntry> entries = vaultDao.getEntriesByCategory(category);
        return decryptEntries(entries, secretKey);
    }

    public List<VaultEntry> getFavoriteEntries(SecretKey secretKey) throws Exception {
        List<VaultEntry> entries = vaultDao.getFavoriteEntries();
        return decryptEntries(entries, secretKey);
    }

    public List<String> getAllCategories() {
        return vaultDao.getAllCategories();
    }

    public int getEntryCount() {
        return vaultDao.getEntryCount();
    }

    private VaultEntry decryptEntry(VaultEntry entry, SecretKey secretKey) throws Exception {
        String plainPassword = CryptoManager.decrypt(entry.getEncryptedPassword(), secretKey);
        entry.setEncryptedPassword(plainPassword);
        return entry;
    }

    private List<VaultEntry> decryptEntries(List<VaultEntry> entries, SecretKey secretKey) throws Exception {

        for (VaultEntry entry : entries) {
            decryptEntry(entry, secretKey);
        }
        return entries;
    }
}
