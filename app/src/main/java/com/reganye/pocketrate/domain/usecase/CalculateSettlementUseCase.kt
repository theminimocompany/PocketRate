package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.TripRepository
import com.reganye.pocketrate.domain.model.SettlementResult
import javax.inject.Inject

class CalculateSettlementUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String): List<SettlementResult> = tripRepository.getSettlement(tripId)
}
