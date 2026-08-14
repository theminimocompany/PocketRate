package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.TripRepository
import com.reganye.pocketrate.domain.model.Expense
import javax.inject.Inject

class GetExpenseByIdUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(id: String): Expense? = tripRepository.getExpenseById(id)
}
