package com.reganye.pocketrate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reganye.pocketrate.data.local.entity.HistoricalRateEntity

@Dao
interface HistoricalRateDao {
    @Query(
        "SELECT * FROM historical_rates " +
                "WHERE currencyCode = :code AND date >= :startDate " +
                "ORDER BY date ASC"
    )
    suspend fun getRatesForPeriod(code: String, startDate: String): List<HistoricalRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: HistoricalRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<HistoricalRateEntity>)

    @Query("SELECT COUNT(*) FROM historical_rates WHERE currencyCode = :code")
    suspend fun getCountForCurrency(code: String): Int

    @Query("SELECT * FROM historical_rates WHERE currencyCode = :code AND date = :date LIMIT 1")
    suspend fun getRateForDate(code: String, date: String): HistoricalRateEntity?

    @Query(
        "SELECT * FROM historical_rates WHERE currencyCode = :code " +
                "ORDER BY ABS(julianday(date) - julianday(:date)) ASC LIMIT 1"
    )
    suspend fun getNearestRateForDate(code: String, date: String): HistoricalRateEntity?

    @Query("SELECT * FROM historical_rates WHERE currencyCode = :code ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRate(code: String): HistoricalRateEntity?
}
