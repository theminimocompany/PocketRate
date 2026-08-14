package com.reganye.pocketrate.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "historical_rates",
    primaryKeys = ["currencyCode", "date"]
)
data class HistoricalRateEntity(
    val currencyCode: String,
    val date: String,
    val rateAgainstUsd: Double
)
