package com.reganye.pocketrate.presentation.ui.trips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reganye.pocketrate.domain.model.Companion
import com.reganye.pocketrate.domain.model.SettlementResult
import com.reganye.pocketrate.domain.model.Trip
import com.reganye.pocketrate.domain.usecase.AddCompanionUseCase
import com.reganye.pocketrate.domain.usecase.CalculateSettlementUseCase
import com.reganye.pocketrate.domain.usecase.DeleteCompanionUseCase
import com.reganye.pocketrate.domain.usecase.GetAvailableCurrenciesUseCase
import com.reganye.pocketrate.domain.usecase.GetCompanionsUseCase
import com.reganye.pocketrate.domain.usecase.GetTripByIdUseCase
import com.reganye.pocketrate.domain.usecase.SearchCurrenciesUseCase
import com.reganye.pocketrate.domain.usecase.UpdateSettlementCurrencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Currency
import javax.inject.Inject

@HiltViewModel
class SplitCostsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCompanionsUseCase: GetCompanionsUseCase,
    private val addCompanionUseCase: AddCompanionUseCase,
    private val deleteCompanionUseCase: DeleteCompanionUseCase,
    private val calculateSettlementUseCase: CalculateSettlementUseCase,
    private val getTripByIdUseCase: GetTripByIdUseCase,
    private val updateSettlementCurrencyUseCase: UpdateSettlementCurrencyUseCase,
    private val getAvailableCurrenciesUseCase: GetAvailableCurrenciesUseCase,
    private val searchCurrenciesUseCase: SearchCurrenciesUseCase
) : ViewModel() {

    val tripId: String = savedStateHandle["tripId"] ?: ""

    var trip by mutableStateOf<Trip?>(null)
        private set
    var companions by mutableStateOf<List<Companion>>(emptyList())
        private set
    var settlement by mutableStateOf<List<SettlementResult>>(emptyList())
        private set
    var newCompanionName by mutableStateOf("")
    var isCurrencySelectorOpen by mutableStateOf(false)
        private set
    var currencyQuery by mutableStateOf("")
        private set
    val currencyResults = mutableStateListOf<SearchCurrenciesUseCase.CurrencyResult>()

    var isSettlementExpanded by mutableStateOf(true)
        private set
    var showAllCompanions by mutableStateOf(false)
        private set

    /** Name of a companion whose deletion was blocked, or null. */
    var companionDeleteError by mutableStateOf<String?>(null)
        private set

    private var currencySearchJob: Job? = null
    private var hasLoadedOnce = false

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            trip = getTripByIdUseCase(tripId)
            companions = getCompanionsUseCase(tripId)
            settlement = calculateSettlementUseCase(tripId)
            // Only derive the expand/collapse defaults on the first load;
            // afterwards the user's explicit toggles are left alone.
            if (!hasLoadedOnce) {
                isSettlementExpanded = settlement.size <= 5
                showAllCompanions = companions.size <= 6
                hasLoadedOnce = true
            }
        }
    }

    fun toggleSettlementExpanded() {
        isSettlementExpanded = !isSettlementExpanded
    }

    fun toggleShowAllCompanions() {
        showAllCompanions = !showAllCompanions
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
        currencySearchJob?.cancel()
        currencySearchJob = viewModelScope.launch {
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

    fun updateSettlementCurrency(currency: String) {
        viewModelScope.launch {
            updateSettlementCurrencyUseCase(tripId, currency)
            closeCurrencySelector()
            load()
        }
    }

    fun addCompanion() {
        if (newCompanionName.isBlank()) return
        viewModelScope.launch {
            val colors = listOf(
                0xFF8FA98F,
                0xFF7E93A8,
                0xFFB0A08E,
                0xFF9E8FA9,
                0xFF8FA9A5,
                0xFFA9958F
            ).map { it.toInt() }
            val companion = Companion(
                tripId = tripId,
                name = newCompanionName,
                color = colors[companions.size % colors.size]
            )
            addCompanionUseCase(companion)
            newCompanionName = ""
            load()
        }
    }

    fun deleteCompanion(id: String) {
        viewModelScope.launch {
            val deleted = deleteCompanionUseCase(id)
            if (deleted) {
                companionDeleteError = null
                load()
            } else {
                companionDeleteError = companions.firstOrNull { it.id == id }?.name ?: ""
            }
        }
    }

    fun dismissCompanionDeleteError() {
        companionDeleteError = null
    }
}
