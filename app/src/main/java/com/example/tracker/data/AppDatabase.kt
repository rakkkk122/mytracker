package com.example.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Plan::class, Expense::class, DailyCheckIn::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun dailyCheckInDao(): DailyCheckInDao

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

        /** v3 -> v4: tambah tabel daily_check_ins untuk Smart Daily Check-in & No-Spend Day */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_check_ins` (
                        `dateStr` TEXT PRIMARY KEY NOT NULL,
                        `isCheckedIn` INTEGER NOT NULL DEFAULT 1,
                        `isNoSpendDay` INTEGER NOT NULL DEFAULT 0,
                        `mood` TEXT,
                        `note` TEXT,
                        `checkedInAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "plan_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
