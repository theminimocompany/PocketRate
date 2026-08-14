package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.domain.model.Companion
import com.reganye.pocketrate.domain.model.Expense
import com.reganye.pocketrate.domain.model.ExpenseSplit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementCalculatorTest {

    private val alice = Companion(id = "alice", tripId = "trip", name = "Alice", color = 0)
    private val bob = Companion(id = "bob", tripId = "trip", name = "Bob", color = 0)
    private val charlie = Companion(id = "charlie", tripId = "trip", name = "Charlie", color = 0)

    @Test
    fun `equal split among all companions balances out`() {
        val companions = listOf(alice, bob, charlie)
        val expenses = listOf(
            expense(id = "e1", amount = 90.0, converted = 90.0, rate = 1.0, payerId = alice.id)
        )
        val splits = emptyMap<String, List<ExpenseSplit>>()

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        assertEquals(2, results.size)
        assertSettlement(results, bob, alice, 30.0)
        assertSettlement(results, charlie, alice, 30.0)
    }

    @Test
    fun `payer is not among splitters`() {
        val companions = listOf(alice, bob, charlie)
        val expenses = listOf(
            expense(id = "e1", amount = 100.0, converted = 100.0, rate = 1.0, payerId = alice.id)
        )
        val splits = mapOf(
            "e1" to listOf(
                ExpenseSplit("e1", bob.id, 50.0),
                ExpenseSplit("e1", charlie.id, 50.0)
            )
        )

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        assertEquals(2, results.size)
        assertSettlement(results, bob, alice, 50.0)
        assertSettlement(results, charlie, alice, 50.0)
    }

    @Test
    fun `foreign currency expense is normalized to home currency`() {
        val companions = listOf(alice, bob)
        val expenses = listOf(
            expense(
                id = "e1",
                amount = 100.0,
                converted = 110.0,
                rate = 1.1,
                payerId = alice.id
            )
        )
        val splits = mapOf(
            "e1" to listOf(
                ExpenseSplit("e1", alice.id, 50.0),
                ExpenseSplit("e1", bob.id, 50.0)
            )
        )

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        // Alice paid 110 home-currency equivalent.
        // Each share is 50 original * 1.1 = 55 home currency.
        // Alice: +110 - 55 = +55, Bob: -55.
        assertEquals(1, results.size)
        assertSettlement(results, bob, alice, 55.0)
    }

    @Test
    fun `partial split among subset of companions`() {
        val companions = listOf(alice, bob, charlie)
        val expenses = listOf(
            expense(id = "e1", amount = 100.0, converted = 100.0, rate = 1.0, payerId = alice.id)
        )
        val splits = mapOf(
            "e1" to listOf(
                ExpenseSplit("e1", alice.id, 50.0),
                ExpenseSplit("e1", bob.id, 50.0)
            )
        )

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        assertEquals(1, results.size)
        assertSettlement(results, bob, alice, 50.0)
    }

    @Test
    fun `multiple expenses with cross payments`() {
        val companions = listOf(alice, bob, charlie)
        val expenses = listOf(
            expense(id = "e1", amount = 90.0, converted = 90.0, rate = 1.0, payerId = alice.id),
            expense(id = "e2", amount = 60.0, converted = 60.0, rate = 1.0, payerId = bob.id)
        )
        val splits = emptyMap<String, List<ExpenseSplit>>()

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        // e1: Alice +90, everyone -30 -> Alice +60, Bob -30, Charlie -30
        // e2: Bob +60, everyone -20 -> Alice -20, Bob +40, Charlie -20
        // Totals: Alice +40, Bob +10, Charlie -50
        assertEquals(2, results.size)
        assertSettlement(results, charlie, alice, 40.0)
        assertSettlement(results, charlie, bob, 10.0)
    }

    @Test
    fun `no companions returns empty settlement`() {
        val results = SettlementCalculator.calculate(emptyList(), emptyList(), emptyMap())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `no expenses returns empty settlement`() {
        val results = SettlementCalculator.calculate(listOf(alice, bob), emptyList(), emptyMap())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `split shares do not sum to full amount still balance because payer credit equals total debits`() {
        // This tests an intentionally unbalanced split to ensure the algorithm does not crash
        // and still produces deterministic output. In practice splits should always sum to amount.
        val companions = listOf(alice, bob)
        val expenses = listOf(
            expense(id = "e1", amount = 100.0, converted = 100.0, rate = 1.0, payerId = alice.id)
        )
        val splits = mapOf(
            "e1" to listOf(
                ExpenseSplit("e1", bob.id, 40.0) // only 40 of 100 assigned
            )
        )

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        // Alice +100, Bob -40. Alice is owed 60 net.
        assertEquals(1, results.size)
        assertSettlement(results, bob, alice, 40.0)
    }

    @Test
    fun `foreign currency with partial split normalizes correctly`() {
        val companions = listOf(alice, bob, charlie)
        val expenses = listOf(
            expense(
                id = "e1",
                amount = 200.0,
                converted = 250.0,
                rate = 1.25,
                payerId = bob.id
            )
        )
        val splits = mapOf(
            "e1" to listOf(
                ExpenseSplit("e1", alice.id, 100.0),
                ExpenseSplit("e1", charlie.id, 100.0)
            )
        )

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        // Bob paid 250 home-currency equivalent.
        // Alice and Charlie each owe 100 * 1.25 = 125.
        assertEquals(2, results.size)
        assertSettlement(results, alice, bob, 125.0)
        assertSettlement(results, charlie, bob, 125.0)
    }

    @Test
    fun `multiple expenses with different partial splits`() {
        val companions = listOf(alice, bob, charlie)
        val expenses = listOf(
            expense(id = "e1", amount = 120.0, converted = 120.0, rate = 1.0, payerId = alice.id),
            expense(id = "e2", amount = 80.0, converted = 80.0, rate = 1.0, payerId = bob.id),
            expense(id = "e3", amount = 60.0, converted = 60.0, rate = 1.0, payerId = charlie.id)
        )
        val splits = mapOf(
            // e1: Alice pays, split only between Alice and Bob.
            "e1" to listOf(
                ExpenseSplit("e1", alice.id, 60.0),
                ExpenseSplit("e1", bob.id, 60.0)
            ),
            // e2: Bob pays, split only between Bob and Charlie.
            "e2" to listOf(
                ExpenseSplit("e2", bob.id, 40.0),
                ExpenseSplit("e2", charlie.id, 40.0)
            ),
            // e3: Charlie pays, split among all three.
            "e3" to listOf(
                ExpenseSplit("e3", alice.id, 20.0),
                ExpenseSplit("e3", bob.id, 20.0),
                ExpenseSplit("e3", charlie.id, 20.0)
            )
        )

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        // e1: Alice +120 - 60 = +60, Bob -60, Charlie 0
        // e2: Bob +80 - 40 = +40, Charlie -40, Alice 0
        // e3: Charlie +60 - 20 = +40, Alice -20, Bob -20
        // Totals: Alice +40, Bob -40, Charlie 0
        assertEquals(1, results.size)
        assertSettlement(results, bob, alice, 40.0)
    }

    @Test
    fun `multiple foreign currency expenses with different split groups`() {
        val companions = listOf(alice, bob, charlie)
        val expenses = listOf(
            // 100 EUR -> 115 USD, paid by Alice, split Alice + Bob.
            expense(id = "e1", amount = 100.0, converted = 115.0, rate = 1.15, payerId = alice.id),
            // 200 EUR -> 220 USD, paid by Charlie, split Bob + Charlie.
            expense(id = "e2", amount = 200.0, converted = 220.0, rate = 1.10, payerId = charlie.id),
            // 90 EUR -> 99 USD, paid by Bob, split all three.
            expense(id = "e3", amount = 90.0, converted = 99.0, rate = 1.10, payerId = bob.id)
        )
        val splits = mapOf(
            "e1" to listOf(
                ExpenseSplit("e1", alice.id, 50.0),
                ExpenseSplit("e1", bob.id, 50.0)
            ),
            "e2" to listOf(
                ExpenseSplit("e2", bob.id, 100.0),
                ExpenseSplit("e2", charlie.id, 100.0)
            ),
            "e3" to listOf(
                ExpenseSplit("e3", alice.id, 30.0),
                ExpenseSplit("e3", bob.id, 30.0),
                ExpenseSplit("e3", charlie.id, 30.0)
            )
        )

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        // e1: Alice +115 - 57.5 = +57.5, Bob -57.5
        // e2: Charlie +220 - 110 = +110, Bob -110
        // e3: Bob +99 - 33 = +66, Alice -33, Charlie -33
        // Totals: Alice +24.5, Bob -101.5, Charlie +77
        assertEquals(2, results.size)
        assertSettlement(results, bob, alice, 24.5)
        assertSettlement(results, bob, charlie, 77.0)
    }

    @Test
    fun `settlement currency different from home currency uses settlement amount`() {
        val companions = listOf(alice, bob)
        val expenses = listOf(
            expense(
                id = "e1",
                amount = 100.0,
                converted = 110.0,
                rate = 1.1,
                settlementAmount = 90.0,
                payerId = alice.id
            )
        )
        val splits = mapOf(
            "e1" to listOf(
                ExpenseSplit("e1", alice.id, 50.0),
                ExpenseSplit("e1", bob.id, 50.0)
            )
        )

        val results = SettlementCalculator.calculate(
            companions,
            expenses,
            splits,
            settlementCurrency = "JPY"
        )

        assertEquals(1, results.size)
        assertSettlement(results, bob, alice, 45.0)
        assertEquals("JPY", results.first().currency)
    }

    @Test
    fun `settlement buffer is applied to settlement amounts`() {
        val companions = listOf(alice, bob)
        val expenses = listOf(
            expense(
                id = "e1",
                amount = 100.0,
                converted = 100.0,
                rate = 1.0,
                settlementAmount = 100.0,
                settlementBufferPercent = 0.05,
                payerId = alice.id
            )
        )
        val splits = emptyMap<String, List<ExpenseSplit>>()

        val results = SettlementCalculator.calculate(
            companions,
            expenses,
            splits,
            settlementCurrency = "EUR"
        )

        // 100 base + 5% buffer = 105. Split equally -> Bob owes Alice 52.5
        assertEquals(1, results.size)
        assertSettlement(results, bob, alice, 52.5)
    }

    @Test
    fun `complex trip with four companions and mixed splits`() {
        val dave = Companion(id = "dave", tripId = "trip", name = "Dave", color = 0)
        val companions = listOf(alice, bob, charlie, dave)
        val expenses = listOf(
            // Dinner: Alice pays, everyone splits.
            expense(id = "dinner", amount = 200.0, converted = 200.0, rate = 1.0, payerId = alice.id),
            // Taxi: Bob pays, only Bob and Charlie split.
            expense(id = "taxi", amount = 40.0, converted = 40.0, rate = 1.0, payerId = bob.id),
            // Museum: Charlie pays, only Charlie and Dave split.
            expense(id = "museum", amount = 60.0, converted = 60.0, rate = 1.0, payerId = charlie.id),
            // Drinks: Dave pays, only Dave and Alice split.
            expense(id = "drinks", amount = 50.0, converted = 50.0, rate = 1.0, payerId = dave.id)
        )
        val splits = mapOf(
            "dinner" to listOf(
                ExpenseSplit("dinner", alice.id, 50.0),
                ExpenseSplit("dinner", bob.id, 50.0),
                ExpenseSplit("dinner", charlie.id, 50.0),
                ExpenseSplit("dinner", dave.id, 50.0)
            ),
            "taxi" to listOf(
                ExpenseSplit("taxi", bob.id, 20.0),
                ExpenseSplit("taxi", charlie.id, 20.0)
            ),
            "museum" to listOf(
                ExpenseSplit("museum", charlie.id, 30.0),
                ExpenseSplit("museum", dave.id, 30.0)
            ),
            "drinks" to listOf(
                ExpenseSplit("drinks", dave.id, 25.0),
                ExpenseSplit("drinks", alice.id, 25.0)
            )
        )

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        // dinner: Alice +200 - 50 = +150; Bob -50; Charlie -50; Dave -50
        // taxi: Bob +40 - 20 = +20; Charlie -20
        // museum: Charlie +60 - 30 = +30; Dave -30
        // drinks: Dave +50 - 25 = +25; Alice -25
        // Totals: Alice +125, Bob -30, Charlie -40, Dave -55
        assertEquals(3, results.size)
        assertSettlement(results, bob, alice, 30.0)
        assertSettlement(results, charlie, alice, 40.0)
        assertSettlement(results, dave, alice, 55.0)
    }

    @Test
    fun `expense without settlement amount is excluded and result flagged estimated`() {
        val companions = listOf(alice, bob)
        val expenses = listOf(
            // Has a EUR settlement amount: included.
            expense(
                id = "e1",
                amount = 100.0,
                converted = 100.0,
                rate = 1.0,
                settlementAmount = 90.0,
                payerId = alice.id
            ),
            // Saved offline, no EUR rate: must be excluded rather than adding
            // its 500 home-currency units into the EUR ledger.
            expense(
                id = "e2",
                amount = 500.0,
                converted = 500.0,
                rate = 1.0,
                settlementAmount = null,
                payerId = bob.id
            )
        )
        val splits = emptyMap<String, List<ExpenseSplit>>()

        val results = SettlementCalculator.calculate(
            companions,
            expenses,
            splits,
            settlementCurrency = "EUR"
        )

        // Only e1 counts: Alice +90, split equally -> Bob owes Alice 45 EUR.
        assertEquals(1, results.size)
        assertSettlement(results, bob, alice, 45.0)
        assertTrue(results.first().isEstimated)

        // The summary follows the same exclusion rule.
        val summary = SettlementCalculator.calculateSummary(
            companions,
            expenses,
            splits,
            settlementCurrency = "EUR"
        )
        assertEquals(90.0, summary.first { it.companionId == alice.id }.paid, 0.001)
        assertEquals(0.0, summary.first { it.companionId == bob.id }.paid, 0.001)
    }

    @Test
    fun `estimated expense flags the settlement as estimated`() {
        val companions = listOf(alice, bob)
        val expenses = listOf(
            expense(
                id = "e1",
                amount = 100.0,
                converted = 100.0,
                rate = 1.0,
                payerId = alice.id,
                isEstimated = true
            )
        )
        val splits = emptyMap<String, List<ExpenseSplit>>()

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        assertEquals(1, results.size)
        assertSettlement(results, bob, alice, 50.0)
        assertTrue(results.first().isEstimated)
    }

    @Test
    fun `fully rate-based expenses are not flagged as estimated`() {
        val companions = listOf(alice, bob)
        val expenses = listOf(
            expense(id = "e1", amount = 100.0, converted = 100.0, rate = 1.0, payerId = alice.id)
        )
        val splits = emptyMap<String, List<ExpenseSplit>>()

        val results = SettlementCalculator.calculate(companions, expenses, splits)

        assertEquals(1, results.size)
        assertTrue(!results.first().isEstimated)
    }

    private fun expense(
        id: String,
        amount: Double,
        converted: Double,
        rate: Double,
        settlementAmount: Double? = null,
        settlementBufferPercent: Double = 0.0,
        payerId: String,
        isEstimated: Boolean = false
    ) = Expense(
        id = id,
        tripId = "trip",
        amount = amount,
        currency = "EUR",
        convertedAmount = converted,
        rateUsed = rate,
        settlementAmount = settlementAmount,
        settlementBufferPercent = settlementBufferPercent,
        category = "Food",
        description = "Test",
        date = 0L,
        homeCurrency = "USD",
        payerId = payerId,
        isEstimated = isEstimated
    )

    private fun assertSettlement(
        results: List<com.reganye.pocketrate.domain.model.SettlementResult>,
        from: Companion,
        to: Companion,
        amount: Double
    ) {
        val match = results.find { it.fromId == from.id && it.toId == to.id }
        assertTrue(
            "Expected ${from.name} -> ${to.name}: $amount, got $results",
            match != null && kotlin.math.abs(match.amount - amount) < 0.01
        )
    }
}
