package com.reganye.pocketrate.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reganye.pocketrate.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PocketRateNavHostViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * Onboarding completion state.
     *
     * `null` means the value is still being read from DataStore. MainActivity keeps
     * the splash screen visible until this becomes non-null, so returning users never
     * see a flash of the onboarding page.
     */
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.onboardingCompleted
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val darkTheme: StateFlow<Boolean?> = settingsRepository.themeMode.map { mode ->
        when (mode) {
            "dark" -> true
            "light" -> false
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
