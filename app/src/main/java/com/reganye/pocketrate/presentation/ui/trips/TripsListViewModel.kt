package com.reganye.pocketrate.presentation.ui.trips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reganye.pocketrate.data.repository.TripRepository
import com.reganye.pocketrate.domain.model.Trip
import com.reganye.pocketrate.domain.usecase.DeleteTripUseCase
import com.reganye.pocketrate.domain.usecase.GetTotalSpentByTripUseCase
import com.reganye.pocketrate.domain.usecase.GetTripsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripsListViewModel @Inject constructor(
    private val getTripsUseCase: GetTripsUseCase,
    private val deleteTripUseCase: DeleteTripUseCase,
    private val getTotalSpentByTripUseCase: GetTotalSpentByTripUseCase
) : ViewModel() {

    var trips by mutableStateOf<List<Trip>>(emptyList())
        private set
    var totalSpentMap by mutableStateOf<Map<String, Double>>(emptyMap())
        private set
    var isLoading by mutableStateOf(false)
        private set

    // Loading is driven by TripsListScreen's LaunchedEffect so the list
    // refreshes every time the screen is shown, not only on VM creation.
    fun loadTrips() {
        viewModelScope.launch {
            isLoading = true
            trips = getTripsUseCase()
            totalSpentMap = getTotalSpentByTripUseCase()
            isLoading = false
        }
    }

    fun deleteTrip(id: String) {
        viewModelScope.launch {
            deleteTripUseCase(id)
            loadTrips()
        }
    }
}
