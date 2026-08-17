package com.reganye.pocketrate.domain.util

/**
 * Provides stable ARGB colors for expense categories.
 *
 * Colors are stored as raw ARGB integers to keep this class independent of
 * any UI framework (Compose, Views, etc.).
 */
object CategoryColorProvider {
    // Tonal palette coordinated with the brand (navy + orange) — muted enough
    // for the Nordic UI, distinct enough for the pie chart.
    private val colors = mapOf(
        "Food" to 0xFFFF5C19.toInt(),           // brand orange
        "Transport" to 0xFF0D3D6E.toInt(),      // brand navy
        "Accommodation" to 0xFF5B84A8.toInt(),  // steel blue
        "Shopping" to 0xFFC78D6B.toInt(),       // terracotta
        "Activities" to 0xFF4E8D8D.toInt(),     // teal
        "Tips" to 0xFF7A9E7E.toInt(),           // sage
        "Misc" to 0xFF7A8794.toInt()            // slate
    )

    private val fallbackGray = 0xFF9E9E9E.toInt()

    fun colorFor(category: String): Int {
        return colors[category] ?: fallbackGray
    }
}
