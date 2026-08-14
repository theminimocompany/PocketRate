package com.reganye.pocketrate.domain.model

/**
 * Centralized currency configuration.
 *
 * Keeping these values in one place makes it easier to change supported
 * currencies, defaults, and historical-currency coverage without hunting
 * through repositories and ViewModels.
 */
object CurrencyConfig {

    /** Currency used when the user has not selected a default. */
    const val DEFAULT_CURRENCY = "USD"

    /** Limited fallback set shown before exchange rates have been synced. */
    val FALLBACK_CURRENCIES = listOf("USD", "EUR", "GBP", "JPY")

    /** Currencies for which historical rate data is available from Frankfurter. */
    val HISTORICAL_CURRENCIES = setOf(
        "EUR", "JPY", "BGN", "CZK", "DKK", "GBP", "HUF", "PLN", "RON", "SEK",
        "CHF", "ISK", "NOK", "HRK", "RUB", "TRY", "AUD", "BRL", "CAD", "CNY",
        "HKD", "IDR", "ILS", "INR", "KRW", "MXN", "MYR", "NZD", "PHP", "SGD",
        "THB", "ZAR"
    )
}
