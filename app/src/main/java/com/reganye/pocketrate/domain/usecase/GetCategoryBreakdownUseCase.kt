package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.TripRepository
import com.reganye.pocketrate.domain.model.CategoryBreakdown
import com.reganye.pocketrate.domain.util.CategoryColorProvider
import javax.inject.Inject

class GetCategoryBreakdownUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String): List<CategoryBreakdown> {
        val expenses = tripRepository.getExpensesForTrip(tripId)
        val total = expenses.sumOf { it.convertedAmount }
        if (total == 0.0) return emptyList()

        return expenses
            .groupBy { it.category }
            .map { (category, items) ->
                val amount = items.sumOf { it.convertedAmount }
                CategoryBreakdown(
                    category = category,
                    amount = amount,
                    percentage = (amount / total).toFloat(),
                    color = CategoryColorProvider.colorFor(category)
                )
            }
            .sortedByDescending { it.amount }
    }
}
