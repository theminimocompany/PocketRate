package com.reganye.pocketrate.domain.model

/**
 * Typed exceptions for currency operations.
 *
 * These let the UI show specific, actionable messages instead of a generic
 * "conversion failed" toast.
 */

class NetworkUnavailableException(message: String = "Network unavailable") : Exception(message)

class ServerErrorException(message: String = "Server error") : Exception(message)

class CurrencyNotFoundException(currency: String) : Exception("Currency not found: $currency")

class NoCachedRatesException(message: String = "No cached exchange rates") : Exception(message)

class HistoricalDataUnavailableException(message: String = "Historical data unavailable") : Exception(message)
