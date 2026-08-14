package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.TripRepository
import javax.inject.Inject

class GetTotalSpentUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String): Double = tripRepository.getTotalSpent(tripId)
}
