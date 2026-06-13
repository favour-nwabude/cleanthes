package dev.favourdevlabs.cleanthes.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry

@Database(
    entities = [VaultEntry::class],
    version = 3,
    exportSchema = true
)
abstract class CleanthesDatabase : RoomDatabase() {

    abstract fun vaultDao(): VaultDao

    companion object {

        @Volatile
        private var INSTANCE: CleanthesDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vault_entries ADD COLUMN totpSecret TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE vault_entries ADD COLUMN totpIssuer TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE vault_entries ADD COLUMN totpDigits INTEGER NOT NULL DEFAULT 6")
                db.execSQL("ALTER TABLE vault_entries ADD COLUMN totpPeriod INTEGER NOT NULL DEFAULT 30")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vault_entries ADD COLUMN totpAlgorithm TEXT NOT NULL DEFAULT 'SHA1'")
            }
        }

        fun getInstance(context: Context): CleanthesDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CleanthesDatabase::class.java,
                    "cleanthes.db"
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}

