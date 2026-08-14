package com.reganye.pocketrate.domain.model

data class Expense(
    val id: String = "",
    val tripId: String,
    val amount: Double,
    val currency: String,
    val convertedAmount: Double = 0.0,
    val rateUsed: Double = 1.0,
    val settlementAmount: Double? = null,
    val settlementRate: Double? = null,
    val settlementRateDate: String? = null,
    val settlementBufferPercent: Double = 0.0,
    val category: String,
    val description: String,
    val date: Long,
    val homeCurrency: String = "",
    val payerId: String = "",
    /**
     * True when any converted value on this expense is an estimate rather than
     * a rate-based conversion (e.g. saved offline, or a settlement amount
     * derived after the fact from a cached rate).
     */
    val isEstimated: Boolean = false
)
