package com.reganye.pocketrate.domain.model

data class ConversionResult(
    val amount: Double,
    val rate: Double,
    val convertedAmount: Double,
    val from: String,
    val to: String
)
