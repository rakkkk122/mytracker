package com.example.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Plan::class, Expense::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        /** v1 -> v2: tambah kolom gambar dan nada alarm. Data lama tetap aman. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plans ADD COLUMN imagePath TEXT")
                db.execSQL("ALTER TABLE plans ADD COLUMN alarmTone TEXT")
            }
        }

        /** v2 -> v3: tambah tabel expenses untuk pencatatan keuangan */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `reason` TEXT NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `imagePath` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "plan_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}