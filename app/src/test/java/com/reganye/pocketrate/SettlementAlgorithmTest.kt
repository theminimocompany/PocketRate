package com.reganye.pocketrate

import com.reganye.pocketrate.domain.model.Companion
import com.reganye.pocketrate.domain.model.Expense
import com.reganye.pocketrate.domain.model.ExpenseSplit
import com.reganye.pocketrate.domain.model.SettlementResult
import com.reganye.pocketrate.domain.usecase.SettlementCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the production settlement algorithm through [SettlementCalculator].
 *
 * Splits are stored in the original expense currency and normalized to the
 * home currency by [Expense.rateUsed] inside the calculator.
 */
class SettlementAlgorithmTest {

    @Test
    fun `equal splits paid by one companion produces settlements to payer`() {
        val balances = SettlementCalculator.calculate(
            companions = listOf(
                companion("a", "Alice"),
                companion("b", "Bob"),
                companion("c", "Carol")
            ),
            expenses = listOf(
                expense(
                    id = "e1",
                    amount = 300.0,
                    converted = 300.0,
                    rate = 1.0,
                    payerId = "a"
                )
            ),
            splits = mapOf(
                "e1" to listOf(
                    split("e1", "a", 100.0),
                    split("e1", "b", 100.0),
                    split("e1", "c", 100.0)
                )
            )
        )

        assertEquals(2, balances.size)
        assertSettlement(balances, fromId = "b", toId = "a", amount = 100.0)
        assertSettlement(balances, fromId = "c", toId = "a", amount = 100.0)
    }

    @Test
    fun `expense paid and owed by same companion results in no settlement`() {
        val balances = SettlementCalculator.calculate(
            companions = listOf(
                companion("a", "Alice"),
                companion("b", "Bob"),
                companion("c", "Carol")
            ),
            expenses = listOf(
                expense(
                    id = "e1",
                    amount = 300.0,
                    converted = 300.0,
                    rate = 1.0,
                    payerId = "a"
                )
            ),
            splits = mapOf(
                "e1" to listOf(split("e1", "a", 300.0))
            )
        )

        assertEquals(emptyList<SettlementResult>(), balances)
    }

    @Test
    fun `mixed payments net correctly`() {
        val balances = SettlementCalculator.calculate(
            companions = listOf(
                companion("a", "Alice"),
                companion("b", "Bob"),
                companion("c", "Carol")
            ),
            expenses = listOf(
                expense(
                    id = "e1",
                    amount = 300.0,
                    converted = 300.0,
                    rate = 1.0,
                    payerId = "a"
                ),
                expense(
                    id = "e2",
                    amount = 200.0,
                    converted = 200.0,
                    rate = 1.0,
                    payerId = "b"
                )
            ),
            splits = mapOf(
                "e1" to listOf(
                    split("e1", "a", 100.0),
                    split("e1", "b", 100.0),
                    split("e1", "c", 100.0)
                ),
                "e2" to listOf(
                    split("e2", "a", 100.0),
                    split("e2", "b", 100.0)
                )
            )
        )

        assertEquals(1, balances.size)
        assertSettlement(balances, fromId = "c", toId = "a", amount = 100.0)
    }

    @Test
    fun `foreign currency splits are normalized by rate used`() {
        val balances = SettlementCalculator.calculate(
            companions = listOf(
                companion("a", "Alice"),
                companion("b", "Bob")
            ),
            expenses = listOf(
                expense(
                    id = "e1",
                    amount = 100.0,
                    converted = 110.0,
                    rate = 1.1,
                    payerId = "a"
                )
            ),
            splits = mapOf(
                "e1" to listOf(
                    split("e1", "a", 50.0),
                    split("e1", "b", 50.0)
                )
            )
        )

        assertEquals(1, balances.size)
        assertSettlement(balances, fromId = "b", toId = "a", amount = 55.0)
    }

    @Test
    fun `settlement currency uses stored settlement amount with buffer`() {
        val balances = SettlementCalculator.calculate(
            companions = listOf(
                companion("a", "Alice"),
                companion("b", "Bob")
            ),
            expenses = listOf(
                Expense(
                    id = "e1",
                    tripId = "trip",
                    amount = 100.0,
                    currency = "USD",
                    convertedAmount = 100.0,
                    rateUsed = 1.0,
                    settlementAmount = 95.0,
                    settlementBufferPercent = 0.05,
                    category = "Misc",
                    description = "",
                    date = 0L,
                    homeCurrency = "USD",
                    payerId = "a"
                )
            ),
            splits = mapOf(
                "e1" to listOf(
                    split("e1", "a", 50.0),
                    split("e1", "b", 50.0)
                )
            ),
            settlementCurrency = "EUR"
        )

        // 95 base + 5% buffer = 99.75. Split equally -> Bob owes Alice 49.875,
        // rounded HALF_UP to EUR's 2 minor units -> 49.88.
        assertEquals(1, balances.size)
        assertSettlement(balances, fromId = "b", toId = "a", amount = 49.88)
        assertEquals("EUR", balances.first().currency)
    }

    private fun expense(
        id: String,
        amount: Double,
        converted: Double,
        rate: Double,
        payerId: String
    ) = Expense(
        id = id,
        tripId = "trip",
        amount = amount,
        currency = "USD",
        convertedAmount = converted,
        rateUsed = rate,
        category = "Misc",
        description = "",
        date = 0L,
        homeCurrency = "USD",
        payerId = payerId
    )

    private fun split(expenseId: String, companionId: String, share: Double) =
        ExpenseSplit(expenseId = expenseId, companionId = companionId, share = share)

    private fun companion(id: String, name: String) =
        Companion(id = id, tripId = "trip", name = name, color = 0)

    private fun assertSettlement(
        results: List<SettlementResult>,
        fromId: String,
        toId: String,
        amount: Double
    ) {
        val match = results.find { it.fromId == fromId && it.toId == toId }
        assertEquals(
            "Expected $fromId -> $toId: $amount, got $results",
            amount,
            match?.amount ?: 0.0,
            0.01
        )
    }
}
