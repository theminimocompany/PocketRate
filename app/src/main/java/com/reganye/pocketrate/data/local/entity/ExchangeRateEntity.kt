package com.reganye.pocketrate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey
    val currencyCode: String,
    val rateAgainstUsd: Double,
    val lastUpdated: Long
)
