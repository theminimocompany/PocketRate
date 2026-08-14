package com.reganye.pocketrate.presentation.ui.charts

import com.google.gson.Gson
import com.reganye.pocketrate.data.remote.FrankfurterTimeSeriesResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartFormattersTest {

    @Test
    fun `axis spacing is daily for one week`() {
        assertEquals(1, calculateXAxisLabelSpacing(7))
    }

    @Test
    fun `axis spacing is every five days for one month`() {
        assertEquals(5, calculateXAxisLabelSpacing(30))
    }

    @Test
    fun `axis spacing is monthly for one year`() {
        assertEquals(30, calculateXAxisLabelSpacing(365))
    }

    @Test
    fun `axis spacing is quarterly for three years`() {
        assertEquals(90, calculateXAxisLabelSpacing(1095))
    }

    @Test
    fun `axis spacing is yearly for five years`() {
        assertEquals(365, calculateXAxisLabelSpacing(1825))
    }

    @Test
    fun `marker label shows rate and formatted date for short ranges`() {
        val label = formatMarkerLabel("2024-06-15", 1.1234, daysBack = 30)
        assertEquals("1.1234\nJun 15", label)
    }

    @Test
    fun `marker label includes year for long ranges`() {
        val label = formatMarkerLabel("2024-06-15", 1.1234, daysBack = 365)
        assertEquals("1.1234\nJun 15, 2024", label)
    }

    @Test
    fun `marker label falls back to raw date when unparseable`() {
        val label = formatMarkerLabel("bad-date", 1.0, daysBack = 30)
        assertEquals("1.0000\nbad-date", label)
    }

    @Test
    fun `x axis label shows month and day for short ranges`() {
        val label = formatXAxisLabel("2024-06-15", daysBack = 30)
        assertEquals("Jun 15", label)
    }

    @Test
    fun `x axis label shows month and short year for one year range`() {
        val label = formatXAxisLabel("2024-06-15", daysBack = 365)
        assertEquals("Jun '24", label)
    }

    @Test
    fun `x axis label shows quarter for three year ranges`() {
        val label = formatXAxisLabel("2024-06-15", daysBack = 1095)
        assertEquals("Q2 '24", label)
    }

    @Test
    fun `x axis label shows year only for multi year ranges`() {
        val label = formatXAxisLabel("2024-06-15", daysBack = 1825)
        assertEquals("2024", label)
    }

    @Test
    fun `time series response parses correctly`() {
        val json = """
            {
                "base": "USD",
                "start_date": "2024-06-01",
                "end_date": "2024-06-03",
                "rates": {
                    "2024-06-01": { "EUR": 0.92 },
                    "2024-06-02": { "EUR": 0.93 },
                    "2024-06-03": { "EUR": 0.91 }
                }
            }
        """.trimIndent()

        val response = Gson().fromJson(json, FrankfurterTimeSeriesResponse::class.java)

        assertEquals("USD", response.base)
        assertEquals("2024-06-01", response.startDate)
        assertEquals("2024-06-03", response.endDate)
        assertEquals(3, response.rates.size)
        assertEquals(0.92, response.rates["2024-06-01"]?.get("EUR") ?: 0.0, 0.0001)
        assertEquals(0.93, response.rates["2024-06-02"]?.get("EUR") ?: 0.0, 0.0001)
        assertEquals(0.91, response.rates["2024-06-03"]?.get("EUR") ?: 0.0, 0.0001)
    }

    @Test
    fun `cross rate is computed correctly from USD based rates`() {
        val json = """
            {
                "base": "USD",
                "start_date": "2024-06-01",
                "end_date": "2024-06-01",
                "rates": {
                    "2024-06-01": { "EUR": 0.92, "GBP": 0.79 }
                }
            }
        """.trimIndent()

        val response = Gson().fromJson(json, FrankfurterTimeSeriesResponse::class.java)
        val rates = response.rates["2024-06-01"] ?: emptyMap()
        val eurRate = rates["EUR"] ?: 0.0
        val gbpRate = rates["GBP"] ?: 0.0
        val eurGbpCross = gbpRate / eurRate

        // EUR/GBP = GBP/USD / EUR/USD
        assertEquals(0.8587, eurGbpCross, 0.0001)
    }
}
