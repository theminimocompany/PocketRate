package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.TripRepository
import com.reganye.pocketrate.domain.model.Companion
import javax.inject.Inject

class AddCompanionUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(companion: Companion): String = tripRepository.addCompanion(companion)
}
