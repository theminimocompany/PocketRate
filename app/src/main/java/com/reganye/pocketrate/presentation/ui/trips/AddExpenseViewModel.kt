package com.reganye.pocketrate.presentation.ui.trips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reganye.pocketrate.domain.model.Companion
import com.reganye.pocketrate.domain.model.Expense
import com.reganye.pocketrate.domain.model.ExpenseSplit
import com.reganye.pocketrate.domain.usecase.AddExpenseUseCase
import com.reganye.pocketrate.domain.usecase.GetAvailableCurrenciesUseCase
import com.reganye.pocketrate.domain.usecase.GetCompanionsUseCase
import com.reganye.pocketrate.domain.usecase.GetExpenseByIdUseCase
import com.reganye.pocketrate.domain.usecase.GetSettlementRateUseCase
import com.reganye.pocketrate.domain.usecase.GetSplitsForExpenseUseCase
import com.reganye.pocketrate.domain.usecase.GetTripByIdUseCase
import com.reganye.pocketrate.domain.usecase.SearchCurrenciesUseCase
import com.reganye.pocketrate.domain.usecase.SplitExpenseUseCase
import com.reganye.pocketrate.domain.usecase.UpdateExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Currency
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getTripByIdUseCase: GetTripByIdUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val getCompanionsUseCase: GetCompanionsUseCase,
    private val getSplitsForExpenseUseCase: GetSplitsForExpenseUseCase,
    private val splitExpenseUseCase: SplitExpenseUseCase,
    private val getAvailableCurrenciesUseCase: GetAvailableCurrenciesUseCase,
    private val searchCurrenciesUseCase: SearchCurrenciesUseCase,
    private val getSettlementRateUseCase: GetSettlementRateUseCase
) : ViewModel() {

    val tripId: String = savedStateHandle["tripId"] ?: ""
    private val expenseId: String? = savedStateHandle["expenseId"]

    var isEditing by mutableStateOf(false)
        private set

    /** False until the initial trip/companion/expense load finishes. */
    var isLoaded by mutableStateOf(false)
        private set

    /** Set when a save is attempted with an invalid amount. */
    var amountError by mutableStateOf(false)
        private set

    var amount by mutableStateOf("")
    var currency by mutableStateOf("USD")
    var category by mutableStateOf("Food")
    var description by mutableStateOf("")
    var date by mutableLongStateOf(System.currentTimeMillis())
    var payerId by mutableStateOf("")
        private set
    var companions by mutableStateOf<List<Companion>>(emptyList())
        private set
    val selectedCompanionIds = mutableStateListOf<String>()

    var settlementCurrency by mutableStateOf("USD")
        private set
    private var tripHomeCurrency = ""
    var applyBuffer by mutableStateOf(false)
        private set
    var settlementPreview by mutableStateOf<SettlementPreview?>(null)
        private set

    var isCurrencySelectorOpen by mutableStateOf(false)
        private set
    var currencyQuery by mutableStateOf("")
        private set
    val currencyResults = mutableStateListOf<SearchCurrenciesUseCase.CurrencyResult>()

    private var currencySearchJob: Job? = null
    private var settlementPreviewJob: Job? = null

    val categories = listOf("Food", "Transport", "Accommodation", "Shopping", "Activities", "Tips", "Misc")

    data class SettlementPreview(
        val baseAmount: Double,
        val bufferedAmount: Double,
        val rate: Double,
        val rateDate: String,
        val currency: String
    )

    init {
        viewModelScope.launch {
            val trip = getTripByIdUseCase(tripId)
            trip?.let {
                currency = it.homeCurrency
                settlementCurrency = it.settlementCurrency
                tripHomeCurrency = it.homeCurrency
            }
            companions = getCompanionsUseCase(tripId)
            if (payerId.isBlank() && companions.isNotEmpty()) {
                payerId = companions.first().id
            }
            selectedCompanionIds.clear()
            selectedCompanionIds.addAll(companions.map { it.id })

            expenseId?.let { id ->
                val existing = getExpenseByIdUseCase(id)
                existing?.let { expense ->
                    isEditing = true
                    amount = expense.amount.toString()
                    currency = expense.currency
                    category = expense.category
                    description = expense.description
                    date = expense.date
                    payerId = expense.payerId
                    applyBuffer = expense.settlementBufferPercent > 0
                    loadSelectedSplits(id)
                }
            }
            isLoaded = true
        }
    }

    private suspend fun loadSelectedSplits(expenseId: String) {
        val splits = getSplitsForExpenseUseCase(expenseId)
        if (splits.isNotEmpty()) {
            selectedCompanionIds.clear()
            selectedCompanionIds.addAll(splits.map { it.companionId })
        }
    }

    fun selectPayer(id: String) {
        payerId = id
    }

    fun onAmountChanged(value: String) {
        amount = value
        amountError = false
    }

    /**
     * Parses the entered amount, accepting comma decimals. Returns null for
     * anything unparseable, non-finite (NaN/Infinity), or not positive.
     */
    fun validAmount(): Double? =
        amount.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }

    fun toggleCompanionSelection(id: String) {
        if (selectedCompanionIds.contains(id)) {
            if (selectedCompanionIds.size > 1) {
                selectedCompanionIds.remove(id)
            }
        } else {
            selectedCompanionIds.add(id)
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

    fun selectCurrency(code: String) {
        currency = code
        currencyQuery = code
        currencyResults.clear()
        closeCurrencySelector()
        recalculateSettlementPreview()
    }

    fun toggleBuffer(enabled: Boolean) {
        applyBuffer = enabled
        recalculateSettlementPreview()
    }

    fun recalculateSettlementPreview() {
        settlementPreviewJob?.cancel()
        settlementPreviewJob = viewModelScope.launch {
            delay(300)
            val parsedAmount = validAmount() ?: run {
                settlementPreview = null
                return@launch
            }
            val bufferPercent = if (applyBuffer) 0.05 else 0.0
            getSettlementRateUseCase(currency, settlementCurrency, date)
                .onSuccess { rate ->
                    val base = parsedAmount * rate
                    val buffered = base * (1 + bufferPercent)
                    settlementPreview = SettlementPreview(
                        baseAmount = base,
                        bufferedAmount = buffered,
                        rate = rate,
                        rateDate = com.reganye.pocketrate.util.DateFormatters.isoDateUs().format(date),
                        currency = settlementCurrency
                    )
                }
                .onFailure {
                    settlementPreview = null
                }
        }
    }

    fun saveExpense(onSaved: () -> Unit) {
        viewModelScope.launch {
            // Don't save before the initial load finished: payer and splits
            // would be persisted empty.
            if (!isLoaded) return@launch
            val parsedAmount = validAmount()
            if (parsedAmount == null) {
                amountError = true
                return@launch
            }
            val bufferPercent = if (applyBuffer) 0.05 else 0.0
            val expense = Expense(
                id = expenseId ?: "",
                tripId = tripId,
                amount = parsedAmount,
                currency = currency,
                settlementBufferPercent = bufferPercent,
                category = category,
                description = description,
                date = date,
                homeCurrency = tripHomeCurrency,
                payerId = payerId
            )
            val savedExpenseId = if (isEditing && expenseId != null) {
                updateExpenseUseCase(expense)
                expenseId
            } else {
                addExpenseUseCase(expense)
            }

            // Always rewrite splits, even with an empty selection — otherwise
            // deselecting everyone while editing would leave the old splits
            // behind and double-count the expense.
            val selectedCompanions = companions.filter { selectedCompanionIds.contains(it.id) }
            val splits = if (selectedCompanions.isEmpty()) {
                emptyList()
            } else {
                // Split in integer cents so the displayed shares always sum
                // exactly to the total; the first `remainder` companions get
                // the extra cent.
                val totalCents = (parsedAmount * 100).roundToInt()
                val baseCents = totalCents / selectedCompanions.size
                val remainder = totalCents % selectedCompanions.size
                selectedCompanions.mapIndexed { index, companion ->
                    val cents = if (index < remainder) baseCents + 1 else baseCents
                    ExpenseSplit(
                        expenseId = savedExpenseId,
                        companionId = companion.id,
                        share = cents / 100.0
                    )
                }
            }
            splitExpenseUseCase(savedExpenseId, splits)

            onSaved()
        }
    }
}
