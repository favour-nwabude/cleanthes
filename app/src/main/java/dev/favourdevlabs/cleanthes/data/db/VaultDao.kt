package dev.favourdevlabs.cleanthes.data.db

import android.content.ContentValues
import android.database.Cursor
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry

class VaultDao(private val dbHelper:DatabaseHelper)
{

    fun insert(entry: VaultEntry): Long =
        dbHelper.writableDatabase.insert(
            DatabaseHelper.TABLE_VAULT_ENTRIES, null, toContentValues(entry)
        )

    fun update(entry: VaultEntry): Int =
        dbHelper.writableDatabase.update(
            DatabaseHelper.TABLE_VAULT_ENTRIES,
            toContentValues(entry),
            "${DatabaseHelper.COLUMN_ID} = ?",
            arrayOf(entry.id.toString())
        )

    fun deleteById(id: Long): Int =
        dbHelper.writableDatabase.delete(
            DatabaseHelper.TABLE_VAULT_ENTRIES,
            "${DatabaseHelper.COLUMN_ID} = ?",
            arrayOf(id.toString())
        )

    fun deleteAll(): Int =
        dbHelper.writableDatabase.delete(DatabaseHelper.TABLE_VAULT_ENTRIES, null, null)

    fun getAllEntries(): List<VaultEntry> {
        val order = "${DatabaseHelper.COLUMN_IS_FAVORITE} DESC, ${DatabaseHelper.COLUMN_TITLE} ASC"
        return cursorToList(
            dbHelper.readableDatabase.query(
                DatabaseHelper.TABLE_VAULT_ENTRIES, null, null, null, null, null, order
            )
        )
    }

    fun getEntryById(id: Long): VaultEntry? {
        val cursor = dbHelper.readableDatabase.query(
            DatabaseHelper.TABLE_VAULT_ENTRIES, null,
            "${DatabaseHelper.COLUMN_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use { if (it.moveToFirst()) fromCursor(it) else null }
    }

    fun searchEntries(query: String): List<VaultEntry> {
        val like = "%$query%"
        val where = "${DatabaseHelper.COLUMN_TITLE} LIKE ? OR ${DatabaseHelper.COLUMN_USERNAME} LIKE ?"
        val order = "${DatabaseHelper.COLUMN_IS_FAVORITE} DESC, ${DatabaseHelper.COLUMN_TITLE} ASC"
        return cursorToList(
            dbHelper.readableDatabase.query(
                DatabaseHelper.TABLE_VAULT_ENTRIES, null,
                where, arrayOf(like, like), null, null, order
            )
        )
    }

    fun getEntriesByCategory(category: String): List<VaultEntry> {
        val order = "${DatabaseHelper.COLUMN_IS_FAVORITE} DESC, ${DatabaseHelper.COLUMN_TITLE} ASC"
        return cursorToList(
            dbHelper.readableDatabase.query(
                DatabaseHelper.TABLE_VAULT_ENTRIES, null,
                "${DatabaseHelper.COLUMN_CATEGORY} = ?",
                arrayOf(category), null, null, order
            )
        )
    }

    fun getFavoriteEntries(): List<VaultEntry> =
        cursorToList(
            dbHelper.readableDatabase.query(
                DatabaseHelper.TABLE_VAULT_ENTRIES, null,
                "${DatabaseHelper.COLUMN_IS_FAVORITE} = ?",
                arrayOf("1"), null, null,
                "${DatabaseHelper.COLUMN_TITLE} ASC"
            )
        )

    fun getEntryCount(): Int {
        val c = dbHelper.readableDatabase
            .rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_VAULT_ENTRIES}", null)
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun getAllCategories(): List<String> {
        val list = mutableListOf<String>()
        val c = dbHelper.readableDatabase.rawQuery(
            "SELECT DISTINCT ${DatabaseHelper.COLUMN_CATEGORY} " +
            "FROM ${DatabaseHelper.TABLE_VAULT_ENTRIES} " +
            "ORDER BY ${DatabaseHelper.COLUMN_CATEGORY} ASC", null
        )
        c.use { if (it.moveToFirst()) do { list.add(it.getString(0)) } while (it.moveToNext()) }
        return list
    }

    // -------------------------------------------------------------------------

    private fun toContentValues(e: VaultEntry) = ContentValues().apply {
        put(DatabaseHelper.COLUMN_TITLE,              e.title)
        put(DatabaseHelper.COLUMN_USERNAME,           e.username)
        put(DatabaseHelper.COLUMN_ENCRYPTED_PASSWORD, e.encryptedPassword)
        put(DatabaseHelper.COLUMN_WEBSITE,            e.website)
        put(DatabaseHelper.COLUMN_CATEGORY,           e.category)
        put(DatabaseHelper.COLUMN_NOTES,              e.notes)
        put(DatabaseHelper.COLUMN_CREATED_AT,         e.createdAt)
        put(DatabaseHelper.COLUMN_UPDATED_AT,         e.updatedAt)
        put(DatabaseHelper.COLUMN_IS_FAVORITE,        if (e.isFavorite) 1 else 0)
        put(DatabaseHelper.COLUMN_TOTP_SECRET,        e.totpSecret)
        put(DatabaseHelper.COLUMN_TOTP_ISSUER,        e.totpIssuer)
        put(DatabaseHelper.COLUMN_TOTP_DIGITS,        e.totpDigits)
        put(DatabaseHelper.COLUMN_TOTP_PERIOD,        e.totpPeriod)
        put(DatabaseHelper.COLUMN_TOTP_ALGORITHM,     e.totpAlgorithm)
    }

    private fun fromCursor(c: Cursor) = VaultEntry(
        id                = c.getLong(DatabaseHelper.IDX_ID),
        title             = c.getString(DatabaseHelper.IDX_TITLE),
        username          = c.getString(DatabaseHelper.IDX_USERNAME),
        encryptedPassword = c.getString(DatabaseHelper.IDX_ENCRYPTED_PASSWORD),
        website           = c.getString(DatabaseHelper.IDX_WEBSITE),
        category          = c.getString(DatabaseHelper.IDX_CATEGORY),
        notes             = c.getString(DatabaseHelper.IDX_NOTES),
        createdAt         = c.getLong(DatabaseHelper.IDX_CREATED_AT),
        updatedAt         = c.getLong(DatabaseHelper.IDX_UPDATED_AT),
        isFavorite        = c.getInt(DatabaseHelper.IDX_IS_FAVORITE) == 1,
        totpSecret        = if (c.isNull(DatabaseHelper.IDX_TOTP_SECRET)) null
                            else c.getString(DatabaseHelper.IDX_TOTP_SECRET),
        totpIssuer        = if (c.isNull(DatabaseHelper.IDX_TOTP_ISSUER)) null
                            else c.getString(DatabaseHelper.IDX_TOTP_ISSUER),
        totpDigits        = if (c.isNull(DatabaseHelper.IDX_TOTP_DIGITS)) 6
                            else c.getInt(DatabaseHelper.IDX_TOTP_DIGITS),
        totpPeriod        = if (c.isNull(DatabaseHelper.IDX_TOTP_PERIOD)) 30
                            else c.getInt(DatabaseHelper.IDX_TOTP_PERIOD),
        totpAlgorithm     = if (c.isNull(DatabaseHelper.IDX_TOTP_ALGORITHM)) "SHA1"
                            else c.getString(DatabaseHelper.IDX_TOTP_ALGORITHM)
    )

    private fun cursorToList(c: Cursor): List<VaultEntry> {
        val list = mutableListOf<VaultEntry>()
        c.use { if (it.moveToFirst()) do { list.add(fromCursor(it)) } while (it.moveToNext()) }
        return list
    }
}
