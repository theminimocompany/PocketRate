package com.reganye.pocketrate.domain.util

/**
 * Provides stable ARGB colors for expense categories.
 *
 * Colors are stored as raw ARGB integers to keep this class independent of
 * any UI framework (Compose, Views, etc.).
 */
object CategoryColorProvider {
    private val colors = mapOf(
        "Food" to 0xFF4CAF50.toInt(),
        "Transport" to 0xFF2196F3.toInt(),
        "Accommodation" to 0xFFFF9800.toInt(),
        "Shopping" to 0xFFE91E63.toInt(),
        "Activities" to 0xFF9C27B0.toInt(),
        "Tips" to 0xFF00BCD4.toInt(),
        "Misc" to 0xFF607D8B.toInt()
    )

    private val fallbackGray = 0xFF9E9E9E.toInt()

    fun colorFor(category: String): Int {
        return colors[category] ?: fallbackGray
    }
}
