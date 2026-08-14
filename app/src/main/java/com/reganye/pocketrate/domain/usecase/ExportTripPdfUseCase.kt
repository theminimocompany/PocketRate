package com.reganye.pocketrate.domain.usecase

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.reganye.pocketrate.data.repository.TripRepository
import com.reganye.pocketrate.domain.model.Expense
import com.reganye.pocketrate.domain.model.ExpenseSplit
import com.reganye.pocketrate.domain.model.SettlementResult
import com.reganye.pocketrate.domain.model.Trip
import com.reganye.pocketrate.util.DateFormatters
import java.io.ByteArrayOutputStream
import java.util.Date
import javax.inject.Inject

// A4 at 72 dpi.
private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
private const val CELL_PADDING = 4f

/**
 * Exports a trip report as PDF bytes using Android's built-in
 * [android.graphics.pdf.PdfDocument] (no third-party PDF library).
 */
class ExportTripPdfUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
    }
    private val linePaint = Paint().apply {
        strokeWidth = 1f
    }

    suspend operator fun invoke(tripId: String): ByteArray {
        val trip = tripRepository.getTripById(tripId)
        val expenses = tripRepository.getExpensesForTrip(tripId)
        val companions = tripRepository.getCompanionsForTrip(tripId)
        val total = tripRepository.getTotalSpent(tripId)
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

        val document = PdfDocument()
        try {
            val writer = PageWriter(document)
            addHeader(writer, trip, total)
            addExpensesTable(writer, expenses, companions, splitsByExpense)
            addCompanionSummaryTable(writer, summary)
            addSettlementTable(writer, settlement)
            addCalculationExplanation(writer)
            writer.finish()

            val outputStream = ByteArrayOutputStream()
            document.writeTo(outputStream)
            return outputStream.toByteArray()
        } finally {
            document.close()
        }
    }

    private fun addHeader(writer: PageWriter, trip: Trip?, total: Double) {
        drawLine(writer, "PocketRate Trip Report", titlePaint, spacingAfter = 8f)
        drawLine(writer, "Trip: ${trip?.name ?: ""}", bodyPaint)
        drawLine(writer, "Home Currency: ${trip?.homeCurrency ?: ""}", bodyPaint)
        drawLine(writer, "Settlement Currency: ${trip?.settlementCurrency?.ifBlank { trip.homeCurrency } ?: ""}", bodyPaint)
        drawLine(writer, "Total Spent: ${"%.2f".format(total)} ${trip?.homeCurrency ?: ""}", bodyPaint)
        writer.advance(bodyPaint.textSize)
    }

    private fun addExpensesTable(
        writer: PageWriter,
        expenses: List<Expense>,
        companions: List<com.reganye.pocketrate.domain.model.Companion>,
        splitsByExpense: Map<String, List<ExpenseSplit>>
    ) {
        drawLine(writer, "Expenses", sectionPaint, spacingAfter = 4f)

        val companionNames = companions.associate { it.id to it.name }
        val rows = expenses.map { expense ->
            listOf(
                DateFormatters.isoDateUs().format(Date(expense.date)),
                expense.description,
                expense.category,
                companionNames[expense.payerId] ?: "",
                "${"%.2f".format(expense.amount)} ${expense.currency}",
                formatSplit(expense, companions, splitsByExpense[expense.id].orEmpty())
            )
        }

        drawTable(
            writer = writer,
            headers = listOf("Date", "Description", "Category", "Payer", "Original", "Split"),
            widths = floatArrayOf(70f, 110f, 65f, 70f, 75f, 125f),
            rows = rows
        )
        writer.advance(bodyPaint.textSize)
    }

    private fun formatSplit(
        expense: Expense,
        companions: List<com.reganye.pocketrate.domain.model.Companion>,
        splits: List<ExpenseSplit>
    ): String {
        if (splits.isEmpty()) {
            if (companions.isEmpty()) return ""
            val share = if (companions.isNotEmpty()) expense.amount / companions.size else 0.0
            return companions.joinToString(", ") { "${it.name} ${"%.2f".format(share)}" }
        }
        val names = companions.associate { it.id to it.name }
        return splits.joinToString(", ") { split ->
            val name = names[split.companionId] ?: split.companionId
            "$name ${"%.2f".format(split.share)}"
        }
    }

    private fun addCompanionSummaryTable(
        writer: PageWriter,
        summary: List<SettlementCalculator.CompanionSummary>
    ) {
        drawLine(writer, "Companion Summary", sectionPaint, spacingAfter = 4f)

        val rows = summary.map { item ->
            listOf(
                item.name,
                "%.2f".format(item.paid),
                "%.2f".format(item.owed),
                "%.2f".format(item.net)
            )
        }

        drawTable(
            writer = writer,
            headers = listOf("Companion", "Paid", "Owed", "Net"),
            widths = floatArrayOf(155f, 120f, 120f, 120f),
            rows = rows
        )
        writer.advance(bodyPaint.textSize)
    }

    private fun addSettlementTable(writer: PageWriter, settlement: List<SettlementResult>) {
        drawLine(writer, "Settlement", sectionPaint, spacingAfter = 4f)

        if (settlement.isEmpty()) {
            drawLine(writer, "No settlements needed.", bodyPaint)
            writer.advance(bodyPaint.textSize)
            return
        }

        val rows = settlement.map { result ->
            listOf(
                result.fromName,
                result.toName,
                "%.2f".format(result.amount),
                result.currency
            )
        }

        drawTable(
            writer = writer,
            headers = listOf("From", "To", "Amount", "Currency"),
            widths = floatArrayOf(150f, 150f, 120f, 95f),
            rows = rows
        )
        writer.advance(bodyPaint.textSize)
    }

    private fun addCalculationExplanation(writer: PageWriter) {
        drawLine(writer, "How the calculation works", sectionPaint, spacingAfter = 4f)
        drawLine(
            writer,
            "For each expense, the payer is credited the full amount paid. Each companion is debited their share. " +
                "A companion's share is either an equal split among all companions, or a custom amount set when the expense was added. " +
                "Net = Paid minus Owed. A positive net means the person is owed money; a negative net means they owe money. " +
                "The settlement list above shows the smallest number of payments needed to make every net balance zero.",
            bodyPaint
        )
    }

    /** Draws [text] wrapped to the content width, paginating between lines. */
    private fun drawLine(writer: PageWriter, text: String, paint: Paint, spacingAfter: Float = 0f) {
        val lineHeight = paint.textSize * 1.35f
        wrapText(text, paint, CONTENT_WIDTH).forEach { line ->
            writer.ensureSpace(lineHeight)
            writer.canvas?.drawText(line, MARGIN, writer.y + paint.textSize, paint)
            writer.advance(lineHeight)
        }
        if (spacingAfter > 0f) writer.advance(spacingAfter)
    }

    /**
     * Draws a simple table. Long cell text is wrapped to the column width; when a
     * row no longer fits on the current page a new page is started and the header
     * row is repeated.
     */
    private fun drawTable(
        writer: PageWriter,
        headers: List<String>,
        widths: FloatArray,
        rows: List<List<String>>
    ) {
        val lineHeight = bodyPaint.textSize * 1.35f

        fun rowHeight(cells: List<String>, paint: Paint): Float {
            val maxLines = cells.mapIndexed { i, cell ->
                wrapText(cell, paint, widths[i] - 2 * CELL_PADDING).size
            }.maxOrNull() ?: 1
            return maxLines * lineHeight + 2 * CELL_PADDING
        }

        fun drawRow(cells: List<String>, paint: Paint, underline: Boolean) {
            val canvas = writer.canvas ?: return
            var x = MARGIN
            cells.forEachIndexed { i, cell ->
                wrapText(cell, paint, widths[i] - 2 * CELL_PADDING).forEachIndexed { lineIndex, line ->
                    canvas.drawText(
                        line,
                        x + CELL_PADDING,
                        writer.y + CELL_PADDING + paint.textSize + lineIndex * lineHeight,
                        paint
                    )
                }
                x += widths[i]
            }
            writer.advance(rowHeight(cells, paint))
            if (underline) {
                canvas.drawLine(MARGIN, writer.y, MARGIN + CONTENT_WIDTH, writer.y, linePaint)
            }
        }

        writer.ensureSpace(rowHeight(headers, tableHeaderPaint))
        drawRow(headers, tableHeaderPaint, underline = true)
        rows.forEach { row ->
            if (writer.ensureSpace(rowHeight(row, bodyPaint))) {
                drawRow(headers, tableHeaderPaint, underline = true)
            }
            drawRow(row, bodyPaint, underline = false)
        }
    }

    /** Splits [text] into lines that each fit within [maxWidth] for [paint]. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val lines = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty()) {
            var count = paint.breakText(remaining, true, maxWidth, null)
            if (count <= 0) count = 1 // Always make progress, even if one char overflows.
            if (count < remaining.length) {
                val lastSpace = remaining.lastIndexOf(' ', count - 1)
                if (lastSpace > 0) count = lastSpace
            }
            lines.add(remaining.take(count).trim())
            remaining = remaining.drop(count).trim()
        }
        return lines
    }

    /**
     * Tracks the current page/canvas and vertical cursor, starting and finishing
     * A4 pages on demand.
     */
    private class PageWriter(private val document: PdfDocument) {
        var y = MARGIN
            private set
        var canvas: Canvas? = null
            private set
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0

        /**
         * Ensures [height] points of vertical space remain, starting a new page
         * when the current one is full. Returns true if a new page was started.
         */
        fun ensureSpace(height: Float): Boolean {
            if (page == null) {
                startPage()
                return true
            }
            if (y + height > PAGE_HEIGHT - MARGIN) {
                finishPage()
                startPage()
                return true
            }
            return false
        }

        fun advance(amount: Float) {
            y += amount
        }

        /** Finishes the current page, if any. Must be called before [PdfDocument.writeTo]. */
        fun finish() {
            finishPage()
        }

        private fun startPage() {
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val newPage = document.startPage(pageInfo)
            page = newPage
            canvas = newPage.canvas
            y = MARGIN
        }

        private fun finishPage() {
            page?.let { document.finishPage(it) }
            page = null
            canvas = null
        }
    }
}
