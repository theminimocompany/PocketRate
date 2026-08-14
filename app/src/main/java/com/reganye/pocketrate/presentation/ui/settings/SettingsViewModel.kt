package com.reganye.pocketrate.presentation.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reganye.pocketrate.data.repository.SettingsRepository
import com.reganye.pocketrate.domain.usecase.SearchCurrenciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val searchCurrenciesUseCase: SearchCurrenciesUseCase
) : ViewModel() {

    var defaultCurrency by mutableStateOf("USD")
        private set
    var searchQuery by mutableStateOf("")
        private set
    val searchResults = mutableStateListOf<SearchCurrenciesUseCase.CurrencyResult>()

    private val _darkMode = mutableStateOf(false)
    var darkMode: Boolean
        get() = _darkMode.value
        set(value) {
            _darkMode.value = value
            viewModelScope.launch { settingsRepository.setThemeMode(if (value) "dark" else "light") }
        }

    var adFreeUntil by mutableStateOf(0L)
        private set

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            defaultCurrency = settingsRepository.defaultCurrency.first()
            searchQuery = defaultCurrency
            darkMode = settingsRepository.themeMode.first() == "dark"
            adFreeUntil = settingsRepository.adFreeUntil.first()
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults.clear()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            val results = searchCurrenciesUseCase(query)
            searchResults.clear()
            searchResults.addAll(results)
        }
    }

    fun selectDefaultCurrency(code: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultCurrency(code)
            defaultCurrency = code
            searchQuery = code
            searchResults.clear()
        }
    }

    fun onRewardedAdWatched() {
        viewModelScope.launch {
            settingsRepository.setAdFreeUntil(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
            adFreeUntil = settingsRepository.adFreeUntil.first()
        }
    }
}
