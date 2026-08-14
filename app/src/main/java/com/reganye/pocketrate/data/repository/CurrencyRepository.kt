package com.reganye.pocketrate.data.repository

import com.reganye.pocketrate.data.local.AppDatabase
import com.reganye.pocketrate.data.local.dao.ExchangeRateDao
import com.reganye.pocketrate.data.local.dao.HistoricalRateDao
import com.reganye.pocketrate.data.local.entity.ExchangeRateEntity
import com.reganye.pocketrate.data.local.entity.HistoricalRateEntity
import com.reganye.pocketrate.data.remote.ExchangeRateApiService
import com.reganye.pocketrate.data.remote.FrankfurterApiService
import com.reganye.pocketrate.domain.model.ConversionResult
import com.reganye.pocketrate.domain.model.CurrencyConfig
import com.reganye.pocketrate.domain.model.CurrencyNotFoundException
import com.reganye.pocketrate.domain.model.HistoricalDataUnavailableException
import com.reganye.pocketrate.domain.model.HistoricalRate
import com.reganye.pocketrate.domain.model.HistoricalRatesResult
import com.reganye.pocketrate.domain.model.NetworkUnavailableException
import com.reganye.pocketrate.domain.model.NoCachedRatesException
import com.reganye.pocketrate.domain.model.ServerErrorException
import com.reganye.pocketrate.util.DateFormatters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import timber.log.Timber
import java.io.IOException
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepository @Inject constructor(
    private val exchangeRateApi: ExchangeRateApiService,
    private val frankfurterApi: FrankfurterApiService,
    private val exchangeRateDao: ExchangeRateDao,
    private val historicalRateDao: HistoricalRateDao,
    private val settingsRepository: SettingsRepository,
    private val appDatabase: AppDatabase
) {
    companion object {
        private const val STALE_THRESHOLD_MS = 24 * 60 * 60 * 1000L
    }

    // Serializes syncs so the periodic worker and a manual refresh cannot
    // interleave their wipe-and-replace of the rate cache.
    private val syncMutex = Mutex()

    /**
     * Fetches the latest USD-based exchange rates from the network and caches them.
     *
     * @return [Result.success] when rates are cached, or [Result.failure] with a typed
     *   exception describing why the sync failed.
     */
    suspend fun syncRates(): Result<Unit> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val primary = runCatching { fetchFromExchangeRateApi() }
            primary.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            if (primary.isSuccess && primary.getOrDefault(false)) {
                settingsRepository.setLastSync(System.currentTimeMillis())
                return@withContext Result.success(Unit)
            }

            val fallback = runCatching { fetchFromFrankfurter() }
            fallback.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            if (fallback.isSuccess && fallback.getOrDefault(false)) {
                settingsRepository.setLastSync(System.currentTimeMillis())
                return@withContext Result.success(Unit)
            }

            Timber.e("Both exchange rate sources failed")
            val cause = primary.exceptionOrNull() ?: fallback.exceptionOrNull()
            Result.failure(cause ?: ServerErrorException("Both exchange rate sources failed"))
        }
    }

    private suspend fun fetchFromExchangeRateApi(): Boolean {
        return try {
            val response = exchangeRateApi.getLatestRates()
            if (!response.isSuccessful) {
                throw ServerErrorException("ExchangeRate-API returned ${response.code()}")
            }
            val body = response.body()
            val rates = body?.rates
                ?: throw ServerErrorException("ExchangeRate-API returned empty rates")
            val timestamp = body.timeLastUpdated?.times(1000) ?: System.currentTimeMillis()
            val usdBasedRates = if (body.base.equals("USD", true)) {
                rates
            } else {
                val baseRate = rates["USD"]
                    ?: throw ServerErrorException("ExchangeRate-API missing USD cross-rate")
                rates.mapValues { (_, rate) -> rate / baseRate }
                    .plus("USD" to 1.0)
            }
            val entities = usdBasedRates.map { (code, rate) ->
                ExchangeRateEntity(code, rate, timestamp)
            }
            appDatabase.withTransaction {
                exchangeRateDao.clearAll()
                exchangeRateDao.insertRates(entities)
                cacheHistoricalRates(usdBasedRates, timestamp)
            }
            Timber.d("ExchangeRate-API sync succeeded with ${entities.size} currencies")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w(e, "ExchangeRate-API sync failed: network")
            throw NetworkUnavailableException("Unable to reach ExchangeRate-API")
        } catch (e: ServerErrorException) {
            Timber.w(e, "ExchangeRate-API sync failed: server")
            throw e
        } catch (e: Exception) {
            Timber.w(e, "ExchangeRate-API sync failed")
            throw ServerErrorException(e.message ?: "ExchangeRate-API sync failed")
        }
    }

    private suspend fun fetchFromFrankfurter(): Boolean {
        return try {
            val response = frankfurterApi.getLatestRates("USD")
            if (!response.isSuccessful) {
                throw ServerErrorException("Frankfurter API returned ${response.code()}")
            }
            val body = response.body()
            val rates = body?.rates
                ?: throw ServerErrorException("Frankfurter API returned empty rates")
            val timestamp = System.currentTimeMillis()
            val usdBasedRates = if (body.base.equals("EUR", true)) {
                val eurToUsd = rates["USD"]
                    ?: throw ServerErrorException("Frankfurter API missing USD cross-rate")
                rates.mapValues { (_, rate) -> rate / eurToUsd }
                    .plus("USD" to 1.0)
            } else {
                // Frankfurter omits the base currency from the rates map; re-add
                // it so conversions from the base currency keep working.
                rates.plus(body.base.uppercase() to 1.0)
            }
            val entities = usdBasedRates.map { (code, rate) ->
                ExchangeRateEntity(code, rate, timestamp)
            }
            appDatabase.withTransaction {
                exchangeRateDao.clearAll()
                exchangeRateDao.insertRates(entities)
                cacheHistoricalRates(usdBasedRates, timestamp)
            }
            Timber.d("Frankfurter fallback sync succeeded with ${entities.size} currencies")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w(e, "Frankfurter sync failed: network")
            throw NetworkUnavailableException("Unable to reach Frankfurter API")
        } catch (e: ServerErrorException) {
            Timber.w(e, "Frankfurter sync failed: server")
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Frankfurter sync failed")
            throw ServerErrorException(e.message ?: "Frankfurter sync failed")
        }
    }

    private suspend fun cacheHistoricalRates(rates: Map<String, Double>, timestamp: Long) {
        val date = DateFormatters.isoDateUtc().format(timestamp)
        val historicalEntities = rates.filterKeys { it in CurrencyConfig.HISTORICAL_CURRENCIES }
            .map { (code, rate) ->
                HistoricalRateEntity(code, date, rate)
            }
        historicalRateDao.insertRates(historicalEntities)
    }

    /**
     * Converts [amount] from [from] currency to [to] currency using cached rates.
     *
     * @return [Result.success] with the conversion details, or [Result.failure] if a rate
     *   is missing or the cache is empty.
     */
    suspend fun convert(amount: Double, from: String, to: String): Result<ConversionResult> {
        if (from == to) {
            return Result.success(ConversionResult(amount, 1.0, amount, from, to))
        }

        if (exchangeRateDao.getAllRates().isEmpty()) {
            return Result.failure(NoCachedRatesException("Exchange rates have not been synced yet"))
        }

        val fromRate = exchangeRateDao.getRate(from)?.rateAgainstUsd
        val toRate = exchangeRateDao.getRate(to)?.rateAgainstUsd
        if (fromRate == null || toRate == null) {
            val missing = listOfNotNull(
                from.takeIf { fromRate == null },
                to.takeIf { toRate == null }
            ).joinToString(", ")
            return Result.failure(CurrencyNotFoundException(missing))
        }

        val crossRate = toRate / fromRate
        val converted = amount * crossRate
        return Result.success(ConversionResult(amount, crossRate, converted, from, to))
    }

    suspend fun getAvailableCurrencies(): List<String> {
        return exchangeRateDao.getAllRates().map { it.currencyCode }
    }

    suspend fun isCacheStale(): Boolean {
        val lastUpdate = exchangeRateDao.getLastUpdated() ?: return true
        return System.currentTimeMillis() - lastUpdate > STALE_THRESHOLD_MS
    }

    suspend fun getLastUpdateTime(): Long? = exchangeRateDao.getLastUpdated()

    suspend fun getHistoricalRates(currencyCode: String, daysBack: Int): List<HistoricalRate> {
        val startDate = DateFormatters.isoDateUtc().format(
            System.currentTimeMillis() - daysBack * 24 * 60 * 60 * 1000L
        )
        return historicalRateDao.getRatesForPeriod(currencyCode, startDate)
            .map { HistoricalRate(it.currencyCode, it.date, it.rateAgainstUsd) }
    }

    /**
     * Returns the best available exchange rate from [fromCurrency] to [toCurrency] for [date].
     *
     * Lookup order:
     * 1. Exact historical rate for [date]
     * 2. Nearest cached historical rate within a few days
     * 3. Most recent cached historical rate
     * 4. Latest live rate
     */
    suspend fun getRateForDate(
        fromCurrency: String,
        toCurrency: String,
        date: String
    ): Result<Double> = withContext(Dispatchers.IO) {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return@withContext Result.success(1.0)
        }

        val fromRateUsd = findHistoricalRate(fromCurrency, date)
        val toRateUsd = findHistoricalRate(toCurrency, date)

        if (fromRateUsd != null && toRateUsd != null) {
            return@withContext Result.success(toRateUsd / fromRateUsd)
        }

        // Fallback: use latest live rate.
        convert(1.0, fromCurrency, toCurrency)
            .map { it.rate }
            .onFailure {
                Timber.w("No rate available for $fromCurrency -> $toCurrency on $date")
            }
    }

    private suspend fun findHistoricalRate(currencyCode: String, date: String): Double? {
        if (currencyCode.equals("USD", ignoreCase = true)) return 1.0

        historicalRateDao.getRateForDate(currencyCode, date)?.let {
            return it.rateAgainstUsd
        }
        historicalRateDao.getNearestRateForDate(currencyCode, date)?.let {
            return it.rateAgainstUsd
        }
        historicalRateDao.getLatestRate(currencyCode)?.let {
            return it.rateAgainstUsd
        }
        return null
    }

    suspend fun backfillHistoricalRates(currencyCode: String, daysBack: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val endCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val startCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            startCalendar.add(Calendar.DAY_OF_YEAR, -daysBack)

            val startDate = DateFormatters.isoDateUtc().format(startCalendar.time)
            val endDate = DateFormatters.isoDateUtc().format(endCalendar.time)

            try {
                val response = frankfurterApi.getTimeSeriesRates(
                    startDate = startDate,
                    endDate = endDate,
                    from = "USD",
                    to = currencyCode
                )
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        ServerErrorException("Frankfurter returned ${response.code()}")
                    )
                }
                val body = response.body()
                    ?: return@withContext Result.failure(
                        ServerErrorException("Frankfurter returned empty body")
                    )
                val ratesMap = body.rates
                if (ratesMap.isEmpty()) {
                    return@withContext Result.failure(
                        HistoricalDataUnavailableException("No historical data from $startDate to $endDate")
                    )
                }
                val entities = ratesMap.mapNotNull { (date, rates) ->
                    val rate = when {
                        currencyCode.equals("USD", ignoreCase = true) -> 1.0
                        body.base.equals("EUR", ignoreCase = true) -> {
                            val eurToUsd = rates["USD"] ?: return@mapNotNull null
                            rates[currencyCode]?.div(eurToUsd)
                        }
                        else -> rates[currencyCode]
                    }
                    rate?.let { HistoricalRateEntity(currencyCode, date, it) }
                }
                historicalRateDao.insertRates(entities)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.w(e, "Failed to backfill $currencyCode from $startDate to $endDate")
                Result.failure(NetworkUnavailableException("Unable to reach Frankfurter API"))
            } catch (e: Exception) {
                Timber.w(e, "Failed to backfill $currencyCode from $startDate to $endDate")
                Result.failure(ServerErrorException(e.message ?: "Historical backfill failed"))
            }
        }
    }

    suspend fun getHistoricalCrossRates(
        fromCurrency: String,
        toCurrency: String,
        daysBack: Int
    ): Result<HistoricalRatesResult> = withContext(Dispatchers.IO) {
        val unsupportedFrom = !fromCurrency.equals("USD", ignoreCase = true) &&
            !CurrencyConfig.HISTORICAL_CURRENCIES.contains(fromCurrency.uppercase())
        val unsupportedTo = !toCurrency.equals("USD", ignoreCase = true) &&
            !CurrencyConfig.HISTORICAL_CURRENCIES.contains(toCurrency.uppercase())

        when {
            unsupportedFrom && unsupportedTo -> {
                return@withContext Result.success(
                    HistoricalRatesResult(
                        errorMessage = "Historical data is not available for $fromCurrency or $toCurrency. Please select supported currencies."
                    )
                )
            }
            unsupportedFrom -> {
                return@withContext Result.success(
                    HistoricalRatesResult(
                        errorMessage = "Historical data is not available for $fromCurrency. Please select a supported currency."
                    )
                )
            }
            unsupportedTo -> {
                return@withContext Result.success(
                    HistoricalRatesResult(
                        errorMessage = "Historical data is not available for $toCurrency. Please select a supported currency."
                    )
                )
            }
        }

        val endCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val startCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        startCalendar.add(Calendar.DAY_OF_YEAR, -daysBack)

        val startDate = DateFormatters.isoDateUtc().format(startCalendar.time)
        val endDate = DateFormatters.isoDateUtc().format(endCalendar.time)

        try {
            val response = frankfurterApi.getTimeSeriesRates(
                startDate = startDate,
                endDate = endDate,
                from = "USD",
                to = "$fromCurrency,$toCurrency"
            )
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    ServerErrorException("Frankfurter returned ${response.code()}")
                )
            }
            val body = response.body()
                ?: return@withContext Result.failure(
                    ServerErrorException("Frankfurter returned empty body")
                )
            val ratesMap = body.rates
            if (ratesMap.isEmpty()) {
                return@withContext Result.success(
                    HistoricalRatesResult(
                        errorMessage = "No historical data found for this period."
                    )
                )
            }

            val rates = ratesMap.mapNotNull { (date, rates) ->
                val fromRate = when {
                    fromCurrency.equals("USD", ignoreCase = true) -> 1.0
                    body.base.equals("EUR", ignoreCase = true) -> {
                        val eurToUsd = rates["USD"] ?: return@mapNotNull null
                        rates[fromCurrency]?.div(eurToUsd)
                    }
                    else -> rates[fromCurrency]
                }
                val toRate = when {
                    toCurrency.equals("USD", ignoreCase = true) -> 1.0
                    body.base.equals("EUR", ignoreCase = true) -> {
                        val eurToUsd = rates["USD"] ?: return@mapNotNull null
                        rates[toCurrency]?.div(eurToUsd)
                    }
                    else -> rates[toCurrency]
                }
                if (fromRate == null || toRate == null || fromRate == 0.0) return@mapNotNull null
                HistoricalRate(
                    currencyCode = "$fromCurrency$toCurrency",
                    date = date,
                    rateAgainstUsd = toRate / fromRate
                )
            }.sortedBy { it.date }

            if (rates.isEmpty()) {
                Result.success(
                    HistoricalRatesResult(
                        errorMessage = "Historical data is not available for this currency pair."
                    )
                )
            } else {
                Result.success(HistoricalRatesResult(rates = rates))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w(e, "Failed to fetch cross rates $fromCurrency/$toCurrency")
            Result.failure(NetworkUnavailableException("Unable to reach Frankfurter API"))
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch cross rates $fromCurrency/$toCurrency")
            Result.failure(ServerErrorException(e.message ?: "Failed to load historical data"))
        }
    }

}
