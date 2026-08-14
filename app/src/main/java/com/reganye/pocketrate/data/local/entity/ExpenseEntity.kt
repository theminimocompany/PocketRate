package com.reganye.pocketrate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tripId"])]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val amount: Double,
    val currency: String,
    val convertedAmount: Double,
    val rateUsed: Double,
    /**
     * True when [convertedAmount]/[rateUsed] could not be computed (e.g. saved
     * offline with no cached rates) and hold placeholder values instead.
     */
    @ColumnInfo(name = "conversion_estimated", defaultValue = "0")
    val conversionEstimated: Boolean = false,
    @ColumnInfo(name = "settlement_amount")
    val settlementAmount: Double? = null,
    @ColumnInfo(name = "settlement_rate")
    val settlementRate: Double? = null,
    @ColumnInfo(name = "settlement_rate_date")
    val settlementRateDate: String? = null,
    @ColumnInfo(name = "settlement_buffer_percent", defaultValue = "0.0")
    val settlementBufferPercent: Double = 0.0,
    val category: String,
    val description: String,
    val date: Long,
    @ColumnInfo(defaultValue = "")
    val payerId: String = ""
)
