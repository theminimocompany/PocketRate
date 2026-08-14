package com.reganye.pocketrate.presentation.ui.converter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reganye.pocketrate.data.repository.SettingsRepository
import com.reganye.pocketrate.domain.model.ConversionResult
import com.reganye.pocketrate.domain.model.CurrencyConfig
import com.reganye.pocketrate.domain.model.CurrencyNotFoundException
import com.reganye.pocketrate.domain.model.NoCachedRatesException
import com.reganye.pocketrate.domain.model.NetworkUnavailableException
import com.reganye.pocketrate.domain.model.ServerErrorException
import com.reganye.pocketrate.domain.usecase.ConvertCurrencyUseCase
import com.reganye.pocketrate.domain.usecase.GetAvailableCurrenciesUseCase
import com.reganye.pocketrate.domain.usecase.RefreshRatesUseCase
import com.reganye.pocketrate.domain.usecase.SearchCurrenciesUseCase
import com.reganye.pocketrate.domain.usecase.ShouldRefreshRatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val getAvailableCurrenciesUseCase: GetAvailableCurrenciesUseCase,
    private val refreshRatesUseCase: RefreshRatesUseCase,
    private val shouldRefreshRatesUseCase: ShouldRefreshRatesUseCase,
    private val searchCurrenciesUseCase: SearchCurrenciesUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val AD_FREE_RECHECK_INTERVAL_MS = 60_000L
    }

    var amount by mutableStateOf("100")
    var fromCurrency by mutableStateOf("USD")
    var toCurrency by mutableStateOf("EUR")
    var fromQuery by mutableStateOf("USD")
        private set
    var toQuery by mutableStateOf("EUR")
        private set
    val fromResults = mutableStateListOf<SearchCurrenciesUseCase.CurrencyResult>()
    val toResults = mutableStateListOf<SearchCurrenciesUseCase.CurrencyResult>()
    var currencies by mutableStateOf(CurrencyConfig.FALLBACK_CURRENCIES)
        private set
    var result by mutableStateOf<ConversionResult?>(null)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var isStale by mutableStateOf(false)
        private set
    var lastUpdated by mutableStateOf<Long?>(null)
        private set
    var conversionCount by mutableStateOf(0)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isFromSelectorOpen by mutableStateOf(false)
        private set
    var isToSelectorOpen by mutableStateOf(false)
        private set

    private val _showBanner = MutableStateFlow(false)
    val showBanner: StateFlow<Boolean> = _showBanner

    /**
     * One-shot events that tell the UI to show an interstitial ad.
     * Rendezvous channel + trySend: events are dropped when nobody is
     * collecting, so stale ad events are never queued while the screen is gone.
     */
    private val _interstitialEvents = Channel<Unit>(Channel.RENDEZVOUS)
    val interstitialEvents = _interstitialEvents.receiveAsFlow()

    private var fromSearchJob: Job? = null
    private var toSearchJob: Job? = null

    init {
        viewModelScope.launch {
            loadCurrencies()
            fromCurrency = settingsRepository.defaultCurrency.first()
            // Never leave FROM and TO on the same currency (e.g. default EUR
            // with the hardcoded TO=EUR initial value).
            if (toCurrency == fromCurrency) {
                toCurrency = if (fromCurrency == "EUR") "USD" else "EUR"
            }
            fromQuery = fromCurrency
            toQuery = toCurrency
            // Sync if stale, empty, or still using the limited fallback set.
            if (shouldRefreshRatesUseCase() || currencies.isEmpty() || currencies.size < 100) {
                refreshRates()
            }
            checkInterstitial()
            observeAdFree()
        }
    }

    private fun observeAdFree() {
        viewModelScope.launch {
            settingsRepository.adFreeUntil.collectLatest { until ->
                // Re-check the deadline every minute so the ad-free window
                // expires even while the app stays open.
                while (true) {
                    _showBanner.value = System.currentTimeMillis() > until
                    delay(AD_FREE_RECHECK_INTERVAL_MS)
                }
            }
        }
    }

    fun swap() {
        val temp = fromCurrency
        fromCurrency = toCurrency
        toCurrency = temp
        fromQuery = fromCurrency
        toQuery = toCurrency
        fromResults.clear()
        toResults.clear()
        errorMessage = null
    }

    fun openFromSelector() {
        isFromSelectorOpen = true
        fromQuery = ""
        searchFrom("")
    }

    fun openToSelector() {
        isToSelectorOpen = true
        toQuery = ""
        searchTo("")
    }

    fun closeSelectors() {
        isFromSelectorOpen = false
        isToSelectorOpen = false
    }

    fun onFromQueryChanged(query: String) {
        fromQuery = query
        searchFrom(query)
    }

    fun onToQueryChanged(query: String) {
        toQuery = query
        searchTo(query)
    }

    private fun searchFrom(query: String) {
        fromSearchJob?.cancel()
        fromSearchJob = viewModelScope.launch {
            if (query.isNotBlank()) delay(300)
            val results = if (query.isBlank()) {
                getAvailableCurrenciesUseCase()
                    .map { SearchCurrenciesUseCase.CurrencyResult(it, currencyName(it)) }
            } else {
                searchCurrenciesUseCase(query, allowFallback = false)
            }
            fromResults.clear()
            fromResults.addAll(results)
        }
    }

    private fun searchTo(query: String) {
        toSearchJob?.cancel()
        toSearchJob = viewModelScope.launch {
            if (query.isNotBlank()) delay(300)
            val results = if (query.isBlank()) {
                getAvailableCurrenciesUseCase()
                    .map { SearchCurrenciesUseCase.CurrencyResult(it, currencyName(it)) }
            } else {
                searchCurrenciesUseCase(query, allowFallback = false)
            }
            toResults.clear()
            toResults.addAll(results)
        }
    }

    private fun currencyName(code: String): String =
        runCatching { java.util.Currency.getInstance(code).displayName }.getOrDefault(code)

    fun selectFromCurrency(code: String) {
        fromCurrency = code
        fromQuery = code
        fromResults.clear()
        errorMessage = null
        closeSelectors()
    }

    fun selectToCurrency(code: String) {
        toCurrency = code
        toQuery = code
        toResults.clear()
        errorMessage = null
        closeSelectors()
    }

    fun convert() {
        viewModelScope.launch {
            errorMessage = null
            val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
            if (parsedAmount == null || !parsedAmount.isFinite() || parsedAmount <= 0.0) {
                errorMessage = "Please enter a valid amount."
                result = null
            } else {
                convertCurrencyUseCase(parsedAmount, fromCurrency, toCurrency)
                    .onSuccess { conversion ->
                        result = conversion
                    }
                    .onFailure { error ->
                        result = null
                        errorMessage = when (error) {
                            is NetworkUnavailableException -> "No internet connection. Please check your network and try again."
                            is ServerErrorException -> "Exchange rate server is unavailable. Please try again later."
                            is NoCachedRatesException -> "Rates not synced yet. Pull down to refresh."
                            is CurrencyNotFoundException -> "Currency not available: ${error.message}"
                            else -> "Conversion failed. Please check currencies and try again."
                        }
                    }
            }
            checkInterstitial()
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            isRefreshing = true
            errorMessage = null
            refreshRatesUseCase()
                .onSuccess {
                    loadCurrencies()
                    isStale = false
                }
                .onFailure { error ->
                    errorMessage = when (error) {
                        is NetworkUnavailableException -> "No internet connection. Couldn't refresh rates."
                        is ServerErrorException -> "Rate server is down. Please try again later."
                        else -> "Failed to refresh exchange rates. Please try again later."
                    }
                }
            isRefreshing = false
        }
    }

    private suspend fun loadCurrencies() {
        val available = getAvailableCurrenciesUseCase()
        if (available.isNotEmpty()) {
            currencies = available
        }
        isStale = shouldRefreshRatesUseCase()
        lastUpdated = settingsRepository.lastSync.first()
    }

    private suspend fun checkInterstitial() {
        if (settingsRepository.isAdFree.first()) {
            settingsRepository.resetConversionCount()
            conversionCount = 0
            return
        }
        conversionCount = settingsRepository.conversionCount.first()
        if (settingsRepository.consumeInterstitialIfDue()) {
            conversionCount = 0
            // Dropped when the screen isn't collecting — that's intentional.
            _interstitialEvents.trySend(Unit)
        }
    }
}
