package dev.favourdevlabs.cleanthes.data.repository

import dev.favourdevlabs.cleanthes.data.db.VaultDao
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.security.CryptoManager
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(private val vaultDao: VaultDao) {

    @Throws(Exception::class)
    fun addEntry(
        title: String, userName: String, plainPassword: String,
        website: String?, category: String, notes: String?,
        isFavorite: Boolean, key: SecretKey
    ): Long = addEntry(
        title, userName, plainPassword, website, category, notes,
        isFavorite, null, null, 6, 30, "SHA1", key
    )

    @Throws(Exception::class)
    fun addEntry(
        title: String, userName: String, plainPassword: String,
        website: String?, category: String, notes: String?,
        isFavorite: Boolean,
        plainTotpSecret: String?, totpIssuer: String?,
        totpDigits: Int, totpPeriod: Int, totpAlgorithm: String?,
        key: SecretKey
    ): Long {
        val encPwd  = CryptoManager.encrypt(plainPassword, key)
        val encTotp = if (!plainTotpSecret.isNullOrEmpty())
            CryptoManager.encrypt(plainTotpSecret, key) else null

        val now = System.currentTimeMillis()
        val entry = VaultEntry(
            title             = title,
            username          = userName,
            encryptedPassword = encPwd,
            website           = website,
            category          = category,
            notes             = notes,
            isFavorite        = isFavorite,
            createdAt         = now,
            updatedAt         = now,
            totpSecret        = encTotp,
            totpIssuer        = totpIssuer,
            totpDigits        = totpDigits,
            totpPeriod        = totpPeriod,
            totpAlgorithm     = totpAlgorithm ?: "SHA1"
        )
        val id = vaultDao.insert(entry)
        if (id != -1L) entry.id = id
        return id
    }

    @Throws(Exception::class)
    fun updateEntry(entry: VaultEntry, plainPassword: String, key: SecretKey): Int {
        entry.encryptedPassword = CryptoManager.encrypt(plainPassword, key)
        entry.totpSecret = if (!entry.totpSecret.isNullOrEmpty())
            CryptoManager.encrypt(entry.totpSecret!!, key) else null
        entry.updatedAt = System.currentTimeMillis()
        return vaultDao.update(entry)
    }

    fun deleteEntry(id: Long): Int = vaultDao.deleteById(id)
    fun wipeVault(): Int           = vaultDao.deleteAll()

    @Throws(Exception::class)
    fun getAllEntries(key: SecretKey): List<VaultEntry> =
        decryptAll(vaultDao.getAllEntries(), key)

    @Throws(Exception::class)
    fun getEntryById(id: Long, key: SecretKey): VaultEntry? =
        vaultDao.getEntryById(id)?.let { decrypt(it, key) }

    @Throws(Exception::class)
    fun searchEntries(query: String, key: SecretKey): List<VaultEntry> =
        decryptAll(vaultDao.searchEntries(query), key)

    @Throws(Exception::class)
    fun getEntriesByCategory(cat: String, key: SecretKey): List<VaultEntry> =
        decryptAll(vaultDao.getEntriesByCategory(cat), key)

    @Throws(Exception::class)
    fun getFavoriteEntries(key: SecretKey): List<VaultEntry> =
        decryptAll(vaultDao.getFavoriteEntries(), key)

    fun getAllCategories(): List<String> = vaultDao.getAllCategories()
    fun getEntryCount(): Int            = vaultDao.getEntryCount()

    @Throws(Exception::class)
    private fun decrypt(e: VaultEntry, key: SecretKey): VaultEntry {
        e.encryptedPassword = CryptoManager.decrypt(e.encryptedPassword, key)
        if (!e.totpSecret.isNullOrEmpty()) {
            e.totpSecret = CryptoManager.decrypt(e.totpSecret!!, key)
        }
        return e
    }

    @Throws(Exception::class)
    private fun decryptAll(list: List<VaultEntry>, key: SecretKey): List<VaultEntry> {
        list.forEach { decrypt(it, key) }
        return list
    }
}
