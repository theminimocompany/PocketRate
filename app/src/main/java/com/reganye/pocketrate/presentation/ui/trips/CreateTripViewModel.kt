package com.reganye.pocketrate.presentation.ui.trips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reganye.pocketrate.data.repository.SettingsRepository
import com.reganye.pocketrate.domain.model.Trip
import com.reganye.pocketrate.domain.usecase.CreateTripUseCase
import com.reganye.pocketrate.domain.usecase.GetAvailableCurrenciesUseCase
import com.reganye.pocketrate.domain.usecase.SearchCurrenciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Currency
import javax.inject.Inject

@HiltViewModel
class CreateTripViewModel @Inject constructor(
    private val createTripUseCase: CreateTripUseCase,
    private val getAvailableCurrenciesUseCase: GetAvailableCurrenciesUseCase,
    private val searchCurrenciesUseCase: SearchCurrenciesUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    var name by mutableStateOf("")
    var homeCurrency by mutableStateOf("USD")
    var startDate by mutableStateOf<Long?>(null)
    var endDate by mutableStateOf<Long?>(null)
    var defaultCurrency by mutableStateOf("USD")
        private set

    var isCurrencySelectorOpen by mutableStateOf(false)
        private set
    var currencyQuery by mutableStateOf("")
        private set
    val currencyResults = mutableStateListOf<SearchCurrenciesUseCase.CurrencyResult>()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            defaultCurrency = settingsRepository.defaultCurrency.first()
            homeCurrency = defaultCurrency
        }
    }

    fun openCurrencySelector() {
        isCurrencySelectorOpen = true
        currencyQuery = ""
        searchCurrencies("")
    }

    fun closeCurrencySelector() {
        isCurrencySelectorOpen = false
    }

    fun onCurrencyQueryChanged(query: String) {
        currencyQuery = query
        searchCurrencies(query)
    }

    private fun searchCurrencies(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotBlank()) delay(300)
            val results = if (query.isBlank()) {
                getAvailableCurrenciesUseCase()
                    .map { SearchCurrenciesUseCase.CurrencyResult(it, currencyName(it)) }
            } else {
                searchCurrenciesUseCase(query, allowFallback = false)
            }
            currencyResults.clear()
            currencyResults.addAll(results)
        }
    }

    private fun currencyName(code: String): String =
        runCatching { Currency.getInstance(code).displayName }.getOrDefault(code)

    fun selectHomeCurrency(code: String) {
        homeCurrency = code
        currencyQuery = code
        currencyResults.clear()
        closeCurrencySelector()
    }

    val isDateRangeValid: Boolean
        get() = startDate == null || endDate == null || endDate!! >= startDate!!

    val canSave: Boolean
        get() = name.isNotBlank() && isDateRangeValid

    fun createTrip(onCreated: (String) -> Unit) {
        if (!canSave) return
        viewModelScope.launch {
            val trip = Trip(
                name = name.trim(),
                homeCurrency = homeCurrency,
                startDate = startDate,
                endDate = endDate,
                budget = null
            )
            val id = createTripUseCase(trip)
            onCreated(id)
        }
    }
}
