package com.reganye.pocketrate.domain.model

data class ExpenseSplit(
    val expenseId: String,
    val companionId: String,
    val share: Double
)
