package com.reganye.pocketrate.presentation.ui.charts

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reganye.pocketrate.data.repository.SettingsRepository
import com.reganye.pocketrate.domain.model.HistoricalDataUnavailableException
import com.reganye.pocketrate.domain.model.HistoricalRate
import com.reganye.pocketrate.domain.model.NetworkUnavailableException
import com.reganye.pocketrate.domain.model.ServerErrorException
import com.reganye.pocketrate.domain.usecase.GetAvailableCurrenciesUseCase
import com.reganye.pocketrate.domain.usecase.GetHistoricalCrossRatesUseCase
import com.reganye.pocketrate.domain.usecase.SearchCurrenciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Currency
import javax.inject.Inject

@HiltViewModel
class HistoricalChartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHistoricalCrossRatesUseCase: GetHistoricalCrossRatesUseCase,
    private val getAvailableCurrenciesUseCase: GetAvailableCurrenciesUseCase,
    private val searchCurrenciesUseCase: SearchCurrenciesUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val navFromCurrency: String? = savedStateHandle["fromCurrency"]
    private val navToCurrency: String? = savedStateHandle["toCurrency"]

    var fromCurrency by mutableStateOf(navFromCurrency ?: "")
    var toCurrency by mutableStateOf(navToCurrency ?: "")
    var daysBack by mutableStateOf(30)
    var rates by mutableStateOf<List<HistoricalRate>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var isFromSelectorOpen by mutableStateOf(false)
        private set
    var isToSelectorOpen by mutableStateOf(false)
        private set
    var fromQuery by mutableStateOf("")
        private set
    var toQuery by mutableStateOf("")
        private set
    val fromResults = mutableStateListOf<SearchCurrenciesUseCase.CurrencyResult>()
    val toResults = mutableStateListOf<SearchCurrenciesUseCase.CurrencyResult>()

    private var fromSearchJob: Job? = null
    private var toSearchJob: Job? = null
    private var loadJob: Job? = null

    init {
        if (fromCurrency.isBlank()) {
            viewModelScope.launch {
                fromCurrency = settingsRepository.defaultCurrency.first()
            }
        }
        if (toCurrency.isBlank()) {
            viewModelScope.launch {
                toCurrency = settingsRepository.defaultCurrency.first()
            }
        }
    }

    val currentRate: Double?
        get() = rates.lastOrNull()?.rateAgainstUsd

    val highRate: Double?
        get() = rates.map { it.rateAgainstUsd }.maxOrNull()

    val lowRate: Double?
        get() = rates.map { it.rateAgainstUsd }.minOrNull()

    val rateChange: Double?
        get() {
            if (rates.size < 2) return null
            val first = rates.first().rateAgainstUsd
            val last = rates.last().rateAgainstUsd
            return if (first == 0.0) null else ((last - first) / first) * 100
        }

    fun load() {
        if (fromCurrency.isBlank() || toCurrency.isBlank()) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            error = null
            getHistoricalCrossRatesUseCase(fromCurrency, toCurrency, daysBack)
                .onSuccess { result ->
                    rates = result.rates
                    error = result.errorMessage
                }
                .onFailure { throwable ->
                    rates = emptyList()
                    error = when (throwable) {
                        is NetworkUnavailableException -> "No internet connection. Please check your network and try again."
                        is ServerErrorException -> "Historical rate server is unavailable. Please try again later."
                        is HistoricalDataUnavailableException -> throwable.message
                        else -> throwable.message ?: "Failed to load historical data."
                    }
                }
            isLoading = false
        }
    }

    fun swap() {
        val temp = fromCurrency
        fromCurrency = toCurrency
        toCurrency = temp
        load()
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
        runCatching { Currency.getInstance(code).displayName }.getOrDefault(code)

    fun selectFromCurrency(code: String) {
        fromCurrency = code
        closeSelectors()
        load()
    }

    fun selectToCurrency(code: String) {
        toCurrency = code
        closeSelectors()
        load()
    }
}
