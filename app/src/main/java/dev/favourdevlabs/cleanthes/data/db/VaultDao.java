package dev.favourdevlabs.cleanthes.data.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;

import java.util.List;
import java.util.ArrayList;

public class VaultDao {

    private final DatabaseHelper dbHelper;

    public VaultDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insert(VaultEntry entry) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = entryToContentValues(entry);
        return db.insert(DatabaseHelper.TABLE_VAULT_ENTRIES, null, values);
    }

    public int update(VaultEntry entry) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = entryToContentValues(entry);
        String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
        String[] whereArgs = { String.valueOf(entry.getId()) };
        return db.update(DatabaseHelper.TABLE_VAULT_ENTRIES, values, whereClause, whereArgs);
    }

    public int deleteById(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
        String[] whereArgs = { String.valueOf(id) };
        return db.delete(DatabaseHelper.TABLE_VAULT_ENTRIES, whereClause, whereArgs);
    }

    public int deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_VAULT_ENTRIES, null, null);
    }

    public List<VaultEntry> getAllEntries() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String orderBy = DatabaseHelper.COLUMN_IS_FAVORITE + " DESC, "
                + DatabaseHelper.COLUMN_TITLE + " ASC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_VAULT_ENTRIES, null, null, null, null, null, orderBy);
        return cursorToList(cursor);
    }

    public VaultEntry getEntryById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        Cursor cursor = db.query(DatabaseHelper.TABLE_VAULT_ENTRIES, null, selection, selectionArgs, null, null, null);

        if (cursor == null)
            return null;
        try {
            if (cursor.moveToFirst()) {
                return cursorToEntry(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public List<VaultEntry> searchEntries(String query) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String likeQuery = "%" + query + "%";

        String selection = DatabaseHelper.COLUMN_TITLE + " LIKE ? OR "
                + DatabaseHelper.COLUMN_USERNAME + " LIKE ?";

        String[] selectionArgs = { likeQuery, likeQuery };

        String orderBy = DatabaseHelper.COLUMN_IS_FAVORITE + " DESC, "
                + DatabaseHelper.COLUMN_TITLE + " ASC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_VAULT_ENTRIES, null, selection, selectionArgs, null, null,
                orderBy);
        return cursorToList(cursor);
    }

    public List<VaultEntry> getEntriesByCategory(String category) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = DatabaseHelper.COLUMN_CATEGORY + " = ? ";
        String[] selectionArgs = { category };
        String orderBy = DatabaseHelper.COLUMN_IS_FAVORITE + " DESC, "
                + DatabaseHelper.COLUMN_TITLE + " ASC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_VAULT_ENTRIES, null, selection, selectionArgs, null, null,
                orderBy);
        return cursorToList(cursor);
    }

    public List<VaultEntry> getFavoriteEntries() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = DatabaseHelper.COLUMN_IS_FAVORITE + " = ?";
        String[] selectionArgs = { "1" };
        String orderBy = DatabaseHelper.COLUMN_TITLE + " ASC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_VAULT_ENTRIES, null, selection, selectionArgs, null, null,
                orderBy);
        return cursorToList(cursor);
    }

    public int getEntryCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_VAULT_ENTRIES, null);

        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            cursor.close();
        }
    }

    public List<String> getAllCategories() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> categories = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT DISTINCT " + DatabaseHelper.COLUMN_CATEGORY + " FROM "
                + DatabaseHelper.TABLE_VAULT_ENTRIES + " ORDER BY " + DatabaseHelper.COLUMN_CATEGORY + " ASC", null);

        try {
            if (cursor.moveToFirst()) {
                do {
                    categories.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return categories;
    }

    private ContentValues entryToContentValues(VaultEntry entry) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TITLE, entry.getTitle());
        values.put(DatabaseHelper.COLUMN_USERNAME, entry.getUsername());
        values.put(DatabaseHelper.COLUMN_ENCRYPTED_PASSWORD, entry.getEncryptedPassword());
        values.put(DatabaseHelper.COLUMN_WEBSITE, entry.getWebsite());
        values.put(DatabaseHelper.COLUMN_CATEGORY, entry.getCategory());
        values.put(DatabaseHelper.COLUMN_NOTES, entry.getNotes());
        values.put(DatabaseHelper.COLUMN_CREATED_AT, entry.getCreatedAt());
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, entry.getUpdatedAt());
        values.put(DatabaseHelper.COLUMN_IS_FAVORITE, entry.isFavorite() ? 1 : 0);
        return values;
    }

    private VaultEntry cursorToEntry(Cursor cursor) {
        VaultEntry entry = new VaultEntry();
        entry.setId(cursor.getLong(DatabaseHelper.IDX_ID));
        entry.setTitle(cursor.getString(DatabaseHelper.IDX_TITLE));
        entry.setUsername(cursor.getString(DatabaseHelper.IDX_USERNAME));
        entry.setEncryptedPassword(cursor.getString(DatabaseHelper.IDX_ENCRYPTED_PASSWORD));
        entry.setWebsite(cursor.getString(DatabaseHelper.IDX_WEBSITE));
        entry.setCategory(cursor.getString(DatabaseHelper.IDX_CATEGORY));
        entry.setNotes(cursor.getString(DatabaseHelper.IDX_NOTES));
        entry.setCreatedAt(cursor.getLong(DatabaseHelper.IDX_CREATED_AT));
        entry.setUpdatedAt(cursor.getLong(DatabaseHelper.IDX_UPDATED_AT));
        entry.setFavorite(cursor.getInt(DatabaseHelper.IDX_IS_FAVORITE) == 1);
        return entry;
    }

    private List<VaultEntry> cursorToList(Cursor cursor) {
        List<VaultEntry> entries = new ArrayList<>();
        if (cursor == null)
            return entries;

        try {
            if (cursor.moveToFirst()) {
                do {
                    entries.add(cursorToEntry(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return entries;
    }
}
