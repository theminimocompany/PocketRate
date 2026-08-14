package com.reganye.pocketrate.data.local.entity

/**
 * Room projection POJO for the per-trip total spent query in ExpenseDao.
 */
data class TripTotal(val tripId: String, val total: Double)
