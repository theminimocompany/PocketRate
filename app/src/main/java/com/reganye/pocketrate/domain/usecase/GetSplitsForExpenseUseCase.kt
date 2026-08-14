package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.TripRepository
import com.reganye.pocketrate.domain.model.ExpenseSplit
import javax.inject.Inject

class GetSplitsForExpenseUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(expenseId: String): List<ExpenseSplit> =
        tripRepository.getSplitsForExpense(expenseId)
}
