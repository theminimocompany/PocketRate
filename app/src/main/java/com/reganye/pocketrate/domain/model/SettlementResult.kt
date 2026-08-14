package com.reganye.pocketrate.domain.model

data class SettlementResult(
    val fromId: String,
    val fromName: String,
    val toId: String,
    val toName: String,
    val amount: Double,
    val currency: String = "",
    val isEstimated: Boolean = false
)
