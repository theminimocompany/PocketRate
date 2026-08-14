package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.TripRepository
import com.reganye.pocketrate.domain.model.Companion
import com.reganye.pocketrate.domain.model.Expense
import com.reganye.pocketrate.domain.model.ExpenseSplit
import com.reganye.pocketrate.domain.model.SettlementResult
import com.reganye.pocketrate.domain.model.Trip
import com.reganye.pocketrate.util.DateFormatters
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportTripCsvUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String): String {
        val trip = tripRepository.getTripById(tripId)
        val expenses = tripRepository.getExpensesForTrip(tripId)
        val companions = tripRepository.getCompanionsForTrip(tripId)
        val splitsByExpense = tripRepository.getSplitsGroupedByExpense(expenses.map { it.id })

        val settlementCurrency = trip?.settlementCurrency?.ifBlank { trip.homeCurrency } ?: ""
        val summary = SettlementCalculator.calculateSummary(
            companions = companions,
            expenses = expenses,
            splits = splitsByExpense,
            settlementCurrency = settlementCurrency
        )
        val settlement = SettlementCalculator.calculate(
            companions = companions,
            expenses = expenses,
            splits = splitsByExpense,
            settlementCurrency = settlementCurrency
        )

        return buildString {
            appendTripSection(trip, expenses, companions)
            appendLine()
            appendExpensesSection(expenses, companions, splitsByExpense)
            appendLine()
            appendCompanionSummary(summary)
            appendLine()
            appendSettlement(settlement)
            appendLine()
            appendCalculationExplanation()
        }
    }

    private fun StringBuilder.appendTripSection(
        trip: Trip?,
        expenses: List<Expense>,
        companions: List<Companion>
    ) {
        appendLine("POCKETRATE TRIP EXPORT")
        appendLine("Trip,${(trip?.name ?: "").toCsvValue()}")
        appendLine("Home Currency,${(trip?.homeCurrency ?: "").toCsvValue()}")
        appendLine("Settlement Currency,${(trip?.settlementCurrency?.ifBlank { trip.homeCurrency } ?: "").toCsvValue()}")
        appendLine("Companions,${companions.joinToString(", ") { it.name }.toCsvValue()}")
        appendLine("Total Expenses,${expenses.size}")
        appendLine("Total Spent,${"%.2f".format(Locale.US, expenses.sumOf { it.convertedAmount })}")
    }

    private fun StringBuilder.appendExpensesSection(
        expenses: List<Expense>,
        companions: List<Companion>,
        splitsByExpense: Map<String, List<ExpenseSplit>>
    ) {
        appendLine("EXPENSES")
        appendLine(
            "Date,Description,Category,Payer,Original Amount,Original Currency," +
                "Converted Amount,Home Currency,Exchange Rate,Split,Settlement Amount,Settlement Rate,Buffer %"
        )

        expenses.forEach { expense ->
            appendLine(expense.toCsvRow(companions, splitsByExpense[expense.id].orEmpty()))
        }
    }

    private fun Expense.toCsvRow(
        companions: List<Companion>,
        splits: List<ExpenseSplit>
    ): String {
        val companionNames = companions.associate { it.id to it.name }
        val splitText = formatSplits(this, splits, companions)
        val settlementText = settlementAmount?.let { "%.2f".format(Locale.US, it) } ?: ""
        val settlementRateText = settlementRate?.let { "%.6f".format(Locale.US, it) } ?: ""
        val bufferText = "%.0f".format(Locale.US, settlementBufferPercent * 100)

        return buildString {
            append(DateFormatters.isoDateUs().format(Date(date)))
            append(",")
            append(description.toCsvValue())
            append(",")
            append(category.toCsvValue())
            append(",")
            append(companionNames[payerId].orEmpty().toCsvValue())
            append(",")
            append("%.2f".format(Locale.US, amount))
            append(",")
            append(currency.toCsvValue())
            append(",")
            append("%.2f".format(Locale.US, convertedAmount))
            append(",")
            append(homeCurrency.toCsvValue())
            append(",")
            append("%.6f".format(Locale.US, rateUsed))
            append(",")
            append(splitText.toCsvValue())
            append(",")
            append(settlementText)
            append(",")
            append(settlementRateText)
            append(",")
            append(bufferText)
        }
    }

    private fun formatSplits(
        expense: Expense,
        splits: List<ExpenseSplit>,
        companions: List<Companion>
    ): String {
        if (splits.isEmpty()) {
            if (companions.isEmpty()) return ""
            val equalShare = expense.amount / companions.size
            return companions.joinToString(", ") { "${it.name} ${"%.2f".format(Locale.US, equalShare)}" }
        }
        val names = companions.associate { it.id to it.name }
        return splits.joinToString(", ") { split ->
            val name = names[split.companionId] ?: split.companionId
            "$name ${"%.2f".format(Locale.US, split.share)}"
        }
    }

    private fun StringBuilder.appendCompanionSummary(summary: List<SettlementCalculator.CompanionSummary>) {
        appendLine("COMPANION SUMMARY")
        appendLine("Companion,Paid,Owed,Net")
        summary.forEach { item ->
            appendLine(
                "${item.name.toCsvValue()},${"%.2f".format(Locale.US, item.paid)},${"%.2f".format(Locale.US, item.owed)},${"%.2f".format(Locale.US, item.net)}"
            )
        }
    }

    private fun StringBuilder.appendSettlement(settlement: List<SettlementResult>) {
        appendLine("SETTLEMENT")
        appendLine("From,To,Amount,Currency")
        if (settlement.isEmpty()) {
            appendLine("No settlements needed.,,,")
        } else {
            settlement.forEach { result ->
                appendLine(
                    "${result.fromName.toCsvValue()},${result.toName.toCsvValue()},${"%.2f".format(Locale.US, result.amount)},${result.currency.toCsvValue()}"
                )
            }
        }
    }

    private fun StringBuilder.appendCalculationExplanation() {
        appendLine("HOW IT WAS CALCULATED")
        appendLine(
            (
                "For each expense, the payer is credited the full amount paid. Each companion is debited their share. " +
                    "A companion's share is either an equal split among all companions, or a custom amount set when the expense was added. " +
                    "Net = Paid minus Owed. A positive net means the person is owed money; a negative net means they owe money. " +
                    "The settlement list shows the smallest number of payments needed to make every net balance zero."
                ).toCsvValue()
        )
    }

    private fun String.toCsvValue(): String {
        // Guard against spreadsheet formula injection: a value starting with
        // '=', '+', '-', '@', or a tab would be evaluated as a formula when
        // opened in Excel/Sheets, so prefix it with a single quote first.
        val guarded = if (startsWith('=') || startsWith('+') || startsWith('-') ||
            startsWith('@') || startsWith('\t')
        ) {
            "'$this"
        } else {
            this
        }
        val escaped = guarded.replace("\"", "\"\"")
        return if (guarded.contains(",") || guarded.contains("\"") || guarded.contains("\n") || guarded.contains("\r")) {
            "\"$escaped\""
        } else {
            guarded
        }
    }
}
