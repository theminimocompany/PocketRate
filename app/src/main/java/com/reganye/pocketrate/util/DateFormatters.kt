package com.reganye.pocketrate.util

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Cached [SimpleDateFormat] instances.
 *
 * [SimpleDateFormat] is not thread-safe, so each formatter is stored in a
 * [ThreadLocal] to avoid synchronization overhead while still reusing instances.
 */
object DateFormatters {

    private fun threadLocal(pattern: String, locale: Locale) = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat(pattern, locale)
    }

    private val isoDateUs = threadLocal("yyyy-MM-dd", Locale.US)
    private val isoDateUtc = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
    }
    private val isoDateDefault = threadLocal("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeDefault = threadLocal("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val monthDayDefault = threadLocal("MMM dd", Locale.getDefault())
    private val monthDayYearDefault = threadLocal("MMM dd, yyyy", Locale.getDefault())
    private val monthYearDefault = threadLocal("MMM ''yy", Locale.getDefault())
    private val yearDefault = threadLocal("yyyy", Locale.getDefault())

    fun isoDateUs(): SimpleDateFormat = isoDateUs.get()!!
    /** UTC-pinned `yyyy-MM-dd` for rate-cache keys, which must match ECB dates. */
    fun isoDateUtc(): SimpleDateFormat = isoDateUtc.get()!!
    fun isoDateDefault(): SimpleDateFormat = isoDateDefault.get()!!
    fun dateTimeDefault(): SimpleDateFormat = dateTimeDefault.get()!!
    fun monthDayDefault(): SimpleDateFormat = monthDayDefault.get()!!
    fun monthDayYearDefault(): SimpleDateFormat = monthDayYearDefault.get()!!
    fun monthYearDefault(): SimpleDateFormat = monthYearDefault.get()!!
    fun yearDefault(): SimpleDateFormat = yearDefault.get()!!
}
