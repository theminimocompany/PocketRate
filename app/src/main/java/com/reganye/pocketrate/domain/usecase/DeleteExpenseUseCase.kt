package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.TripRepository
import javax.inject.Inject

class DeleteExpenseUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(id: String) = tripRepository.deleteExpense(id)
}
