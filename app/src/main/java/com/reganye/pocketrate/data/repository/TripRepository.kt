package com.reganye.pocketrate.data.repository

import androidx.room.withTransaction
import com.reganye.pocketrate.data.local.AppDatabase
import com.reganye.pocketrate.data.local.dao.CompanionDao
import com.reganye.pocketrate.data.local.dao.ExpenseDao
import com.reganye.pocketrate.data.local.dao.ExpenseSplitDao
import com.reganye.pocketrate.data.local.dao.TripDao
import com.reganye.pocketrate.data.local.entity.CompanionEntity
import com.reganye.pocketrate.data.local.entity.ExpenseEntity
import com.reganye.pocketrate.data.local.entity.ExpenseSplitEntity
import com.reganye.pocketrate.data.local.entity.TripEntity
import com.reganye.pocketrate.domain.model.Companion
import com.reganye.pocketrate.domain.model.Expense
import com.reganye.pocketrate.domain.model.SettlementResult
import com.reganye.pocketrate.domain.model.Trip
import com.reganye.pocketrate.util.DateFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val expenseDao: ExpenseDao,
    private val companionDao: CompanionDao,
    private val expenseSplitDao: ExpenseSplitDao,
    private val currencyRepository: CurrencyRepository,
    private val appDatabase: AppDatabase
) {
    suspend fun createTrip(trip: Trip): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        tripDao.insertTrip(
            TripEntity(
                id = id,
                name = trip.name,
                homeCurrency = trip.homeCurrency,
                settlementCurrency = trip.settlementCurrency.ifBlank { trip.homeCurrency },
                startDate = trip.startDate,
                endDate = trip.endDate,
                budget = trip.budget
            )
        )
        id
    }

    suspend fun getAllTrips(): List<Trip> = withContext(Dispatchers.IO) {
        tripDao.getAllTrips().map { it.toDomain() }
    }

    suspend fun getTripById(id: String): Trip? = withContext(Dispatchers.IO) {
        tripDao.getTripById(id)?.toDomain()
    }

    suspend fun updateTrip(trip: Trip) = withContext(Dispatchers.IO) {
        tripDao.updateTrip(
            TripEntity(
                id = trip.id,
                name = trip.name,
                homeCurrency = trip.homeCurrency,
                settlementCurrency = trip.settlementCurrency.ifBlank { trip.homeCurrency },
                startDate = trip.startDate,
                endDate = trip.endDate,
                budget = trip.budget
            )
        )
    }

    /**
     * Changes the settlement currency for a trip and recomputes the settlement
     * amount for every expense using the daily rate on each expense date.
     *
     * The whole migration runs in one transaction: a failure rolls everything
     * back. When no rate is available for an expense, its previous settlement
     * values are kept — overwriting them with null would destroy valid data.
     */
    suspend fun updateSettlementCurrency(tripId: String, newSettlementCurrency: String) =
        withContext(Dispatchers.IO) {
            val trip = tripDao.getTripById(tripId) ?: return@withContext
            val normalizedCurrency = newSettlementCurrency.uppercase()
            if (trip.settlementCurrency.uppercase() == normalizedCurrency) return@withContext

            appDatabase.withTransaction {
                tripDao.updateTrip(trip.copy(settlementCurrency = normalizedCurrency))

                val expenses = expenseDao.getExpensesForTrip(tripId)
                expenses.forEach { entity ->
                    val settlement = computeSettlement(
                        amount = entity.amount,
                        currency = entity.currency,
                        settlementCurrency = normalizedCurrency,
                        date = entity.date,
                        bufferPercent = entity.settlementBufferPercent
                    )
                    if (settlement != null) {
                        expenseDao.updateExpense(
                            entity.copy(
                                settlementAmount = settlement.first,
                                settlementRate = settlement.second,
                                settlementRateDate = settlement.third
                            )
                        )
                    }
                }
            }
        }

    private suspend fun computeSettlement(
        amount: Double,
        currency: String,
        settlementCurrency: String,
        date: Long,
        bufferPercent: Double
    ): Triple<Double, Double, String>? {
        val dateString = DateFormatters.isoDateUtc().format(date)
        if (currency.equals(settlementCurrency, ignoreCase = true)) {
            return Triple(amount, 1.0, dateString)
        }

        return currencyRepository.getRateForDate(currency, settlementCurrency, dateString)
            .map { rate ->
                val base = amount * rate
                Triple(base, rate, dateString)
            }
            .getOrNull()
    }

    suspend fun deleteTrip(id: String) = withContext(Dispatchers.IO) {
        tripDao.deleteTripWithDependencies(id)
    }

    suspend fun addExpense(expense: Expense): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val trip = tripDao.getTripById(expense.tripId)
        val settlementCurrency = trip?.settlementCurrency ?: expense.homeCurrency
        // Expenses built by the UI may lack homeCurrency; fall back to the
        // trip's, then to the expense's own currency (identity conversion).
        val homeCurrency = expense.homeCurrency.ifBlank {
            trip?.homeCurrency ?: expense.currency
        }

        val conversion = currencyRepository.convert(
            expense.amount,
            expense.currency,
            homeCurrency
        )
        val conversionResult = conversion.getOrNull()
        // No rate available (e.g. offline, never synced): keep the original
        // amount as a placeholder but flag it so nothing treats it as a real
        // conversion.
        val convertedAmount = conversionResult?.convertedAmount ?: expense.amount
        val rateUsed = conversionResult?.rate ?: 1.0

        val settlement = computeSettlement(
            amount = expense.amount,
            currency = expense.currency,
            settlementCurrency = settlementCurrency,
            date = expense.date,
            bufferPercent = expense.settlementBufferPercent
        )

        expenseDao.insertExpense(
            ExpenseEntity(
                id = id,
                tripId = expense.tripId,
                amount = expense.amount,
                currency = expense.currency,
                convertedAmount = convertedAmount,
                rateUsed = rateUsed,
                conversionEstimated = conversionResult == null,
                settlementAmount = settlement?.first,
                settlementRate = settlement?.second,
                settlementRateDate = settlement?.third,
                settlementBufferPercent = expense.settlementBufferPercent,
                category = expense.category,
                description = expense.description,
                date = expense.date,
                payerId = expense.payerId
            )
        )
        id
    }

    suspend fun getExpenseById(id: String): Expense? = withContext(Dispatchers.IO) {
        val entity = expenseDao.getExpenseById(id) ?: return@withContext null
        val trip = tripDao.getTripById(entity.tripId)
        entity.toDomain(trip?.homeCurrency ?: "")
    }

    suspend fun updateExpense(expense: Expense) = withContext(Dispatchers.IO) {
        val trip = tripDao.getTripById(expense.tripId)
        val settlementCurrency = trip?.settlementCurrency ?: expense.homeCurrency
        val homeCurrency = expense.homeCurrency.ifBlank {
            trip?.homeCurrency ?: expense.currency
        }

        val conversion = currencyRepository.convert(
            expense.amount,
            expense.currency,
            homeCurrency
        )
        val conversionResult = conversion.getOrNull()
        // Same placeholder policy as addExpense: flag when no real rate existed.
        val convertedAmount = conversionResult?.convertedAmount ?: expense.amount
        val rateUsed = conversionResult?.rate ?: 1.0

        val settlement = computeSettlement(
            amount = expense.amount,
            currency = expense.currency,
            settlementCurrency = settlementCurrency,
            date = expense.date,
            bufferPercent = expense.settlementBufferPercent
        )

        expenseDao.updateExpense(
            ExpenseEntity(
                id = expense.id,
                tripId = expense.tripId,
                amount = expense.amount,
                currency = expense.currency,
                convertedAmount = convertedAmount,
                rateUsed = rateUsed,
                conversionEstimated = conversionResult == null,
                settlementAmount = settlement?.first,
                settlementRate = settlement?.second,
                settlementRateDate = settlement?.third,
                settlementBufferPercent = expense.settlementBufferPercent,
                category = expense.category,
                description = expense.description,
                date = expense.date,
                payerId = expense.payerId
            )
        )
    }

    suspend fun getExpensesForTrip(tripId: String): List<Expense> = withContext(Dispatchers.IO) {
        val trip = tripDao.getTripById(tripId)
        expenseDao.getExpensesForTrip(tripId).map { it.toDomain(trip?.homeCurrency ?: "") }
    }

    suspend fun deleteExpense(id: String) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpenseWithSplits(id)
    }

    suspend fun getTotalSpent(tripId: String): Double = withContext(Dispatchers.IO) {
        expenseDao.getTotalSpent(tripId)
    }

    suspend fun addCompanion(companion: Companion): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        companionDao.insertCompanion(
            CompanionEntity(
                id = id,
                tripId = companion.tripId,
                name = companion.name,
                color = companion.color
            )
        )
        id
    }

    suspend fun getCompanionsForTrip(tripId: String): List<Companion> = withContext(Dispatchers.IO) {
        companionDao.getCompanionsForTrip(tripId).map { it.toDomain() }
    }

    /**
     * Deletes a companion, or returns false without deleting when the companion
     * still pays for expenses or appears in splits — removing them would leave
     * the settlement ledger unbalanced.
     */
    suspend fun deleteCompanion(id: String): Boolean = withContext(Dispatchers.IO) {
        if (expenseDao.countExpensesPaidBy(id) > 0 || expenseSplitDao.countSplitsFor(id) > 0) {
            return@withContext false
        }
        companionDao.deleteCompanion(id)
        true
    }

    suspend fun splitExpense(expenseId: String, splits: List<com.reganye.pocketrate.domain.model.ExpenseSplit>) =
        withContext(Dispatchers.IO) {
            expenseSplitDao.replaceSplitsForExpense(
                expenseId = expenseId,
                splits = splits.map {
                    ExpenseSplitEntity(
                        expenseId = expenseId,
                        companionId = it.companionId,
                        share = it.share
                    )
                }
            )
        }

    suspend fun getSplitsForExpense(expenseId: String): List<com.reganye.pocketrate.domain.model.ExpenseSplit> =
        withContext(Dispatchers.IO) {
            expenseSplitDao.getSplitsForExpense(expenseId).map {
                com.reganye.pocketrate.domain.model.ExpenseSplit(
                    expenseId = it.expenseId,
                    companionId = it.companionId,
                    share = it.share
                )
            }
        }

    suspend fun getSplitsGroupedByExpense(
        expenseIds: List<String>
    ): Map<String, List<com.reganye.pocketrate.domain.model.ExpenseSplit>> =
        withContext(Dispatchers.IO) {
            if (expenseIds.isEmpty()) return@withContext emptyMap()
            expenseSplitDao.getSplitsForExpenses(expenseIds)
                .map {
                    com.reganye.pocketrate.domain.model.ExpenseSplit(
                        expenseId = it.expenseId,
                        companionId = it.companionId,
                        share = it.share
                    )
                }
                .groupBy { it.expenseId }
        }

    suspend fun getTotalSpentByTrip(): Map<String, Double> = withContext(Dispatchers.IO) {
        expenseDao.getTotalsByTrip().associate { it.tripId to it.total }
    }

    suspend fun getSettlement(tripId: String): List<SettlementResult> = withContext(Dispatchers.IO) {
        val companions = companionDao.getCompanionsForTrip(tripId)
        val expenses = expenseDao.getExpensesForTrip(tripId)
        val trip = tripDao.getTripById(tripId)
        val homeCurrency = trip?.homeCurrency ?: ""
        val settlementCurrency = trip?.settlementCurrency ?: homeCurrency
        val splits = getSplitsGroupedByExpense(expenses.map { it.id })

        val domainExpenses = expenses.map { entity ->
            fillMissingSettlement(entity.toDomain(homeCurrency), settlementCurrency)
        }

        com.reganye.pocketrate.domain.usecase.SettlementCalculator.calculate(
            companions = companions.map { it.toDomain() },
            expenses = domainExpenses,
            splits = splits,
            settlementCurrency = settlementCurrency
        )
    }

    /**
     * Expenses saved while offline may have no settlement-currency amount. Try
     * to derive one from the cached rates and flag the expense as estimated. If
     * no rate is available the amount stays null, and [SettlementCalculator]
     * excludes the expense from settlement totals rather than mixing
     * home-currency values into settlement-currency results.
     */
    private suspend fun fillMissingSettlement(expense: Expense, settlementCurrency: String): Expense {
        val needsSettlement = settlementCurrency.isNotBlank() &&
            !settlementCurrency.equals(expense.homeCurrency, ignoreCase = true)
        if (!needsSettlement || expense.settlementAmount != null) return expense

        val dateString = expense.settlementRateDate
            ?: DateFormatters.isoDateUtc().format(expense.date)
        val rate = currencyRepository.getRateForDate(
            expense.currency,
            settlementCurrency,
            dateString
        ).getOrNull()

        return if (rate != null) {
            expense.copy(
                settlementAmount = expense.amount * rate,
                settlementRate = rate,
                settlementRateDate = dateString,
                isEstimated = true
            )
        } else {
            expense.copy(isEstimated = true)
        }
    }

    private fun TripEntity.toDomain(): Trip = Trip(
        id = id,
        name = name,
        homeCurrency = homeCurrency,
        settlementCurrency = settlementCurrency.ifBlank { homeCurrency },
        startDate = startDate,
        endDate = endDate,
        budget = budget
    )

    private fun ExpenseEntity.toDomain(homeCurrency: String): Expense = Expense(
        id = id,
        tripId = tripId,
        amount = amount,
        currency = currency,
        convertedAmount = convertedAmount,
        rateUsed = rateUsed,
        settlementAmount = settlementAmount,
        settlementRate = settlementRate,
        settlementRateDate = settlementRateDate,
        settlementBufferPercent = settlementBufferPercent,
        category = category,
        description = description,
        date = date,
        homeCurrency = homeCurrency,
        payerId = payerId,
        isEstimated = conversionEstimated
    )

    private fun CompanionEntity.toDomain(): Companion = Companion(
        id = id,
        tripId = tripId,
        name = name,
        color = color
    )
}
