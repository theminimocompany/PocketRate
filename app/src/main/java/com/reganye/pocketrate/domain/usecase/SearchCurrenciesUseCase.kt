package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.CurrencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

class SearchCurrenciesUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {
    @Volatile
    private var resultsCache: Pair<List<String>, List<CurrencyResult>>? = null

    suspend operator fun invoke(query: String, allowFallback: Boolean = true): List<CurrencyResult> {
        if (query.isBlank()) return emptyList()

        val normalizedQuery = query.trim().lowercase(Locale.getDefault())

        return withContext(Dispatchers.Default) {
            val cachedCodes = currencyRepository.getAvailableCurrencies()

            val codesToSearch = cachedCodes.ifEmpty {
                if (allowFallback) Currency.getAvailableCurrencies().map { it.currencyCode }
                else return@withContext emptyList()
            }

            cachedResultsFor(codesToSearch)
                .filter { result ->
                    result.code.lowercase().contains(normalizedQuery) ||
                        result.name.lowercase().contains(normalizedQuery)
                }
                .sortedWith(compareByDescending<CurrencyResult> {
                    it.code.lowercase().startsWith(normalizedQuery)
                }.thenBy { it.name })
                .take(MAX_RESULTS)
        }
    }

    /**
     * Maps codes to display names only once per code list; the ICU lookups are
     * expensive, so subsequent searches reuse the cached entries.
     */
    private fun cachedResultsFor(codes: List<String>): List<CurrencyResult> {
        resultsCache?.let { if (it.first == codes) return it.second }
        synchronized(this) {
            resultsCache?.let { if (it.first == codes) return it.second }
            val results = codes.map { code ->
                val displayName = runCatching {
                    Currency.getInstance(code).getDisplayName(Locale.getDefault())
                }.getOrDefault(code)
                CurrencyResult(code, displayName)
            }
            resultsCache = codes to results
            return results
        }
    }

    data class CurrencyResult(
        val code: String,
        val name: String
    )

    companion object {
        private const val MAX_RESULTS = 10
    }
}
