package com.reganye.pocketrate

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyConversionTest {

    @Test
    fun `cross rate conversion uses USD base correctly`() {
        val usdToEur = 0.85
        val usdToGbp = 0.75
        val amount = 100.0
        val rate = usdToGbp / usdToEur
        val converted = amount * rate
        assertEquals(88.24, converted, 0.01)
    }
}
