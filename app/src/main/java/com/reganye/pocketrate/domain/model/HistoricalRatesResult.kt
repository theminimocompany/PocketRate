package com.reganye.pocketrate.domain.model

data class HistoricalRatesResult(
    val rates: List<HistoricalRate> = emptyList(),
    val errorMessage: String? = null
)
