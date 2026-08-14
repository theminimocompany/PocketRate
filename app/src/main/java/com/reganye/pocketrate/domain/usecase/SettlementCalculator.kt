package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.domain.model.Companion
import com.reganye.pocketrate.domain.model.Expense
import com.reganye.pocketrate.domain.model.ExpenseSplit
import com.reganye.pocketrate.domain.model.SettlementResult

object SettlementCalculator {

    data class CompanionSummary(
        val companionId: String,
        val name: String,
        val paid: Double,
        val owed: Double,
        val net: Double
    )

    /**
     * Calculates who owes whom.
     *
     * If [settlementCurrency] is provided and differs from an expense's home
     * currency, the expense's stored [Expense.settlementAmount] is used. An
     * expense without one is excluded from the totals (and the result flagged
     * as estimated) instead of falling back to home-currency values, which
     * would mix two currencies in the same ledger.
     */
    fun calculate(
        companions: List<Companion>,
        expenses: List<Expense>,
        splits: Map<String, List<ExpenseSplit>>,
        settlementCurrency: String = ""
    ): List<SettlementResult> {
        if (companions.isEmpty()) return emptyList()

        val netBalances = companions.associate { it.id to 0.0 }.toMutableMap()
        var hasEstimatedSettlement = false

        expenses.forEach { expense ->
            // An expense with no payer would credit nobody but debit everyone,
            // inventing debt — skip it entirely.
            if (expense.payerId.isBlank()) return@forEach

            val useSettlement = usesSettlementCurrency(expense, settlementCurrency)
            val paidAmount = if (useSettlement) {
                val base = expense.settlementAmountWithBuffer()
                if (base == null) {
                    hasEstimatedSettlement = true
                    return@forEach
                }
                base
            } else {
                expense.convertedAmount
            }
            hasEstimatedSettlement = hasEstimatedSettlement || expense.isEstimated

            netBalances[expense.payerId] = (netBalances[expense.payerId] ?: 0.0) + paidAmount

            val expenseSplits = splits[expense.id].orEmpty()
            if (expenseSplits.isEmpty()) {
                val share = paidAmount / companions.size
                companions.forEach {
                    netBalances[it.id] = (netBalances[it.id] ?: 0.0) - share
                }
            } else {
                expenseSplits.forEach { split ->
                    if (split.share < 0.0) return@forEach
                    val share = if (useSettlement) {
                        expense.shareInSettlementCurrency(split.share)
                    } else {
                        split.share * expense.rateUsed
                    }
                    netBalances[split.companionId] = (netBalances[split.companionId] ?: 0.0) - share
                }
            }
        }

        return settle(netBalances, companions, settlementCurrency, hasEstimatedSettlement)
    }

    /**
     * Returns per-companion paid/owed/net totals using the same rules as [calculate].
     */
    fun calculateSummary(
        companions: List<Companion>,
        expenses: List<Expense>,
        splits: Map<String, List<ExpenseSplit>>,
        settlementCurrency: String = ""
    ): List<CompanionSummary> {
        if (companions.isEmpty()) return emptyList()

        val paid = companions.associate { it.id to 0.0 }.toMutableMap()
        val owed = companions.associate { it.id to 0.0 }.toMutableMap()

        expenses.forEach { expense ->
            // Same guard as calculate(): no payer -> no ledger entry at all.
            if (expense.payerId.isBlank()) return@forEach

            val useSettlement = usesSettlementCurrency(expense, settlementCurrency)
            val paidAmount = if (useSettlement) {
                // Same policy as calculate(): skip expenses with no
                // settlement-currency amount rather than mixing units.
                expense.settlementAmountWithBuffer() ?: return@forEach
            } else {
                expense.convertedAmount
            }

            paid[expense.payerId] = (paid[expense.payerId] ?: 0.0) + paidAmount

            val expenseSplits = splits[expense.id].orEmpty()
            if (expenseSplits.isEmpty()) {
                val share = paidAmount / companions.size
                companions.forEach {
                    owed[it.id] = (owed[it.id] ?: 0.0) + share
                }
            } else {
                expenseSplits.forEach { split ->
                    if (split.share < 0.0) return@forEach
                    val share = if (useSettlement) {
                        expense.shareInSettlementCurrency(split.share)
                    } else {
                        split.share * expense.rateUsed
                    }
                    owed[split.companionId] = (owed[split.companionId] ?: 0.0) + share
                }
            }
        }

        return companions.map {
            val p = paid[it.id] ?: 0.0
            val o = owed[it.id] ?: 0.0
            CompanionSummary(it.id, it.name, p, o, p - o)
        }
    }

    private fun usesSettlementCurrency(expense: Expense, settlementCurrency: String): Boolean =
        settlementCurrency.isNotBlank() &&
            !settlementCurrency.equals(expense.homeCurrency, ignoreCase = true)

    private fun Expense.settlementAmountWithBuffer(): Double? {
        val base = settlementAmount ?: return null
        return base * (1 + settlementBufferPercent)
    }

    private fun Expense.shareInSettlementCurrency(shareInExpenseCurrency: Double): Double {
        val baseSettlement = settlementAmount ?: return shareInExpenseCurrency * rateUsed
        val bufferMultiplier = 1 + settlementBufferPercent
        return if (amount == 0.0) {
            0.0
        } else {
            (shareInExpenseCurrency / amount) * baseSettlement * bufferMultiplier
        }
    }

    private fun settle(
        netBalances: MutableMap<String, Double>,
        companions: List<Companion>,
        currency: String,
        isEstimated: Boolean
    ): List<SettlementResult> {
        val results = mutableListOf<SettlementResult>()
        val names = companions.associate { it.id to it.name }
        // Round each payment to the currency's minor units so nobody is asked
        // to pay fractional cents. The 0.01 dust threshold below absorbs any
        // residual left by rounding. Blank/unknown currencies default to 2.
        val fractionDigits = runCatching {
            java.util.Currency.getInstance(currency).defaultFractionDigits
        }.getOrDefault(2)

        while (true) {
            val debtor = netBalances.filter { it.value < -0.01 }.minByOrNull { it.value }
            val creditor = netBalances.filter { it.value > 0.01 }.maxByOrNull { it.value }
            if (debtor == null || creditor == null) break

            val amount = java.math.BigDecimal.valueOf(minOf(-debtor.value, creditor.value))
                .setScale(fractionDigits, java.math.RoundingMode.HALF_UP)
                .toDouble()
            // For 0-digit currencies a sub-unit residual rounds to zero; stop
            // instead of emitting empty payments forever.
            if (amount <= 0.0) break
            results.add(
                SettlementResult(
                    fromId = debtor.key,
                    fromName = names[debtor.key] ?: debtor.key,
                    toId = creditor.key,
                    toName = names[creditor.key] ?: creditor.key,
                    amount = amount,
                    currency = currency,
                    isEstimated = isEstimated
                )
            )

            netBalances[debtor.key] = netBalances[debtor.key]!! + amount
            netBalances[creditor.key] = netBalances[creditor.key]!! - amount
        }

        return results
    }
}
