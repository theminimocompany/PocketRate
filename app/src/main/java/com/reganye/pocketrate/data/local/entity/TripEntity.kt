package com.reganye.pocketrate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val homeCurrency: String,
    val settlementCurrency: String = homeCurrency,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val budget: Double? = null
)
