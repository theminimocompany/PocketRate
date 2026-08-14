package com.reganye.pocketrate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.reganye.pocketrate.data.local.dao.CompanionDao
import com.reganye.pocketrate.data.local.dao.ExchangeRateDao
import com.reganye.pocketrate.data.local.dao.ExpenseDao
import com.reganye.pocketrate.data.local.dao.ExpenseSplitDao
import com.reganye.pocketrate.data.local.dao.HistoricalRateDao
import com.reganye.pocketrate.data.local.dao.TripDao
import com.reganye.pocketrate.data.local.entity.CompanionEntity
import com.reganye.pocketrate.data.local.entity.ExchangeRateEntity
import com.reganye.pocketrate.data.local.entity.ExpenseEntity
import com.reganye.pocketrate.data.local.entity.ExpenseSplitEntity
import com.reganye.pocketrate.data.local.entity.HistoricalRateEntity
import com.reganye.pocketrate.data.local.entity.TripEntity

@Database(
    entities = [
        ExchangeRateEntity::class,
        HistoricalRateEntity::class,
        TripEntity::class,
        ExpenseEntity::class,
        CompanionEntity::class,
        ExpenseSplitEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun historicalRateDao(): HistoricalRateDao
    abstract fun tripDao(): TripDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun companionDao(): CompanionDao
    abstract fun expenseSplitDao(): ExpenseSplitDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN payerId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN settlementCurrency TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE trips SET settlementCurrency = homeCurrency WHERE settlementCurrency = ''")
                db.execSQL("ALTER TABLE expenses ADD COLUMN settlement_amount REAL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN settlement_rate REAL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN settlement_rate_date TEXT")
                db.execSQL("ALTER TABLE expenses ADD COLUMN settlement_buffer_percent REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN conversion_estimated INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
