package dev.favourdevlabs.cleanthes.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper
private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "cleanthes.db"
        private const val DATABASE_VERSION = 3 // v3: totpAlgorithm column

        const val TABLE_VAULT_ENTRIES = "vault_entries"

        // Column names
        const val COLUMN_ID                 = "id"
        const val COLUMN_TITLE              = "title"
        const val COLUMN_USERNAME           = "username"
        const val COLUMN_ENCRYPTED_PASSWORD = "encryptedPassword"
        const val COLUMN_WEBSITE            = "website"
        const val COLUMN_CATEGORY           = "category"
        const val COLUMN_NOTES              = "notes"
        const val COLUMN_CREATED_AT         = "createdAt"
        const val COLUMN_UPDATED_AT         = "updatedAt"
        const val COLUMN_IS_FAVORITE        = "isFavorite"
        // v2 — TOTP core
        const val COLUMN_TOTP_SECRET        = "totpSecret"
        const val COLUMN_TOTP_ISSUER        = "totpIssuer"
        const val COLUMN_TOTP_DIGITS        = "totpDigits"
        const val COLUMN_TOTP_PERIOD        = "totpPeriod"
        // v3 — TOTP algorithm
        const val COLUMN_TOTP_ALGORITHM     = "totpAlgorithm"

        // Cursor indices — must match SELECT * column order exactly
        const val IDX_ID                 = 0
        const val IDX_TITLE              = 1
        const val IDX_USERNAME           = 2
        const val IDX_ENCRYPTED_PASSWORD = 3
        const val IDX_WEBSITE            = 4
        const val IDX_CATEGORY           = 5
        const val IDX_NOTES              = 6
        const val IDX_CREATED_AT         = 7
        const val IDX_UPDATED_AT         = 8
        const val IDX_IS_FAVORITE        = 9
        // v2
        const val IDX_TOTP_SECRET        = 10
        const val IDX_TOTP_ISSUER        = 11
        const val IDX_TOTP_DIGITS        = 12
        const val IDX_TOTP_PERIOD        = 13
        // v3
        const val IDX_TOTP_ALGORITHM     = 14

    @Volatile private var instance:DatabaseHelper?=null

    @Synchronized
        fun getInstance(context: Context): DatabaseHelper =
            instance ?: DatabaseHelper(context).also { instance = it }
    }

    override fun

    onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_VAULT_ENTRIES (
                $COLUMN_ID                 INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE              TEXT NOT NULL,
                $COLUMN_USERNAME           TEXT NOT NULL,
                $COLUMN_ENCRYPTED_PASSWORD TEXT NOT NULL,
                $COLUMN_WEBSITE            TEXT,
                $COLUMN_CATEGORY           TEXT NOT NULL DEFAULT 'General',
                $COLUMN_NOTES              TEXT,
                $COLUMN_CREATED_AT         INTEGER NOT NULL,
                $COLUMN_UPDATED_AT         INTEGER NOT NULL,
                $COLUMN_IS_FAVORITE        INTEGER NOT NULL DEFAULT 0,
                $COLUMN_TOTP_SECRET        TEXT DEFAULT NULL,
                $COLUMN_TOTP_ISSUER        TEXT DEFAULT NULL,
                $COLUMN_TOTP_DIGITS        INTEGER NOT NULL DEFAULT 6,
                $COLUMN_TOTP_PERIOD        INTEGER NOT NULL DEFAULT 30,
                $COLUMN_TOTP_ALGORITHM     TEXT NOT NULL DEFAULT 'SHA1'
            );
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_category ON $TABLE_VAULT_ENTRIES ($COLUMN_CATEGORY);")
        db.execSQL("CREATE INDEX idx_favorite ON $TABLE_VAULT_ENTRIES ($COLUMN_IS_FAVORITE);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_VAULT_ENTRIES ADD COLUMN $COLUMN_TOTP_SECRET TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE $TABLE_VAULT_ENTRIES ADD COLUMN $COLUMN_TOTP_ISSUER TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE $TABLE_VAULT_ENTRIES ADD COLUMN $COLUMN_TOTP_DIGITS INTEGER NOT NULL DEFAULT 6")
            db.execSQL("ALTER TABLE $TABLE_VAULT_ENTRIES ADD COLUMN $COLUMN_TOTP_PERIOD INTEGER NOT NULL DEFAULT 30")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_VAULT_ENTRIES ADD COLUMN $COLUMN_TOTP_ALGORITHM TEXT NOT NULL DEFAULT 'SHA1'")
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) db.enableWriteAheadLogging()
    }
}
