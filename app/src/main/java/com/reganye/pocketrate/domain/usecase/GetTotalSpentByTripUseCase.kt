package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.TripRepository
import javax.inject.Inject

class GetTotalSpentByTripUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(): Map<String, Double> = tripRepository.getTotalSpentByTrip()
}
