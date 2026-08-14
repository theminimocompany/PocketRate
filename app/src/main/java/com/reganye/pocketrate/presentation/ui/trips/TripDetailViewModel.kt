package com.reganye.pocketrate.presentation.ui.trips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reganye.pocketrate.domain.model.Expense
import com.reganye.pocketrate.domain.model.Trip
import com.reganye.pocketrate.domain.usecase.DeleteExpenseUseCase
import com.reganye.pocketrate.domain.usecase.ExportTripCsvUseCase
import com.reganye.pocketrate.domain.usecase.ExportTripPdfUseCase
import com.reganye.pocketrate.domain.usecase.GetTotalSpentUseCase
import com.reganye.pocketrate.domain.usecase.GetTripByIdUseCase
import com.reganye.pocketrate.domain.usecase.GetTripExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTripByIdUseCase: GetTripByIdUseCase,
    private val getTripExpensesUseCase: GetTripExpensesUseCase,
    private val getTotalSpentUseCase: GetTotalSpentUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val exportTripCsvUseCase: ExportTripCsvUseCase,
    private val exportTripPdfUseCase: ExportTripPdfUseCase
) : ViewModel() {

    val tripId: String = savedStateHandle["tripId"] ?: ""

    var trip by mutableStateOf<Trip?>(null)
        private set
    var expenses by mutableStateOf<List<Expense>>(emptyList())
        private set
    var totalSpent by mutableStateOf(0.0)
        private set
    var isExporting by mutableStateOf(false)
        private set

    fun loadTrip() {
        viewModelScope.launch {
            trip = getTripByIdUseCase(tripId)
            expenses = getTripExpensesUseCase(tripId)
            totalSpent = getTotalSpentUseCase(tripId)
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            deleteExpenseUseCase(id)
            loadTrip()
        }
    }

    suspend fun exportCsv(): String = exportTripCsvUseCase(tripId)
    suspend fun exportPdf(): ByteArray = exportTripPdfUseCase(tripId)

    /**
     * Writes the trip's CSV export to a file on [Dispatchers.IO].
     * Returns the file, or null if the trip name cannot be sanitized or the write fails.
     */
    suspend fun exportCsvFile(cacheDir: File): File? {
        isExporting = true
        return try {
            withContext(Dispatchers.IO) {
                runCatching {
                    val csv = exportTripCsvUseCase(tripId)
                    val name = "PocketRate_${sanitizedTripName()}.csv"
                    File(cacheDir, "exports/$name").apply {
                        parentFile?.mkdirs()
                        writeText(csv)
                    }
                }.getOrNull()
            }
        } finally {
            isExporting = false
        }
    }

    /**
     * Writes the trip's PDF export to a file on [Dispatchers.IO].
     * Returns the file, or null if the trip name cannot be sanitized or the write fails.
     */
    suspend fun exportPdfFile(cacheDir: File): File? {
        isExporting = true
        return try {
            withContext(Dispatchers.IO) {
                runCatching {
                    val pdf = exportTripPdfUseCase(tripId)
                    val name = "PocketRate_${sanitizedTripName()}.pdf"
                    File(cacheDir, "exports/$name").apply {
                        parentFile?.mkdirs()
                        writeBytes(pdf)
                    }
                }.getOrNull()
            }
        } finally {
            isExporting = false
        }
    }

    private fun sanitizedTripName(): String {
        val raw = trip?.name?.trim() ?: "trip"
        // Replace filesystem-unfriendly characters with underscores.
        val sanitized = raw.replace(Regex("[^a-zA-Z0-9\\s_-]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(50)
        return sanitized.ifBlank { "trip" }
    }
}
