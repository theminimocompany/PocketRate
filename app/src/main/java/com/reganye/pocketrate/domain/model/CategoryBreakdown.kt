package com.reganye.pocketrate.domain.model

data class CategoryBreakdown(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Int
)
