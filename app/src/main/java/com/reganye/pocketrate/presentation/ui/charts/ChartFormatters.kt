package com.reganye.pocketrate.presentation.ui.charts

import com.reganye.pocketrate.util.DateFormatters
import java.text.SimpleDateFormat
import java.util.Calendar

fun calculateXAxisLabelSpacing(daysBack: Int): Int = when {
    daysBack <= 7 -> 1
    daysBack <= 30 -> 5
    daysBack <= 365 -> 30
    daysBack <= 1095 -> 90
    else -> 365
}

fun formatMarkerLabel(
    dateString: String,
    rate: Double,
    daysBack: Int,
    parser: SimpleDateFormat = DateFormatters.isoDateUs()
): String {
    val formatter = if (daysBack <= 30) DateFormatters.monthDayDefault() else DateFormatters.monthDayYearDefault()
    val formattedDate = runCatching { parser.parse(dateString)?.let { formatter.format(it) } }.getOrNull()
        ?: dateString
    return "${"%.4f".format(rate)}\n$formattedDate"
}

fun formatXAxisLabel(
    dateString: String,
    daysBack: Int,
    parser: SimpleDateFormat = DateFormatters.isoDateUs()
): String {
    val date = runCatching { parser.parse(dateString) }.getOrNull() ?: return dateString
    return when {
        daysBack <= 30 -> DateFormatters.monthDayDefault().format(date)
        daysBack <= 365 -> DateFormatters.monthYearDefault().format(date)
        daysBack <= 1095 -> {
            val calendar = Calendar.getInstance().apply { time = date }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val quarter = (month / 3) + 1
            "Q$quarter '${year % 100}"
        }
        else -> DateFormatters.yearDefault().format(date)
    }
}
