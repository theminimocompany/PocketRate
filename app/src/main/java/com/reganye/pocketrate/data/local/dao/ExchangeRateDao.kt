package com.reganye.pocketrate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reganye.pocketrate.data.local.entity.ExchangeRateEntity

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :code LIMIT 1")
    suspend fun getRate(code: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates ORDER BY currencyCode ASC")
    suspend fun getAllRates(): List<ExchangeRateEntity>

    @Query("SELECT MAX(lastUpdated) FROM exchange_rates")
    suspend fun getLastUpdated(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    @Query("DELETE FROM exchange_rates")
    suspend fun clearAll()
}
