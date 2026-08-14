package com.reganye.pocketrate.domain.model

data class Trip(
    val id: String = "",
    val name: String,
    val homeCurrency: String,
    val settlementCurrency: String = homeCurrency,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val budget: Double? = null
)
