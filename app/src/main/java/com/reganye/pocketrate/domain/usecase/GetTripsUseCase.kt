package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.TripRepository
import com.reganye.pocketrate.domain.model.Trip
import javax.inject.Inject

class GetTripsUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(): List<Trip> = tripRepository.getAllTrips()
}
