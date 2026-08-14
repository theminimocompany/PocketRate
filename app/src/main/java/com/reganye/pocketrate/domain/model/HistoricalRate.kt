package com.reganye.pocketrate.domain.model

data class HistoricalRate(
    val currencyCode: String,
    val date: String,
    val rateAgainstUsd: Double
)
