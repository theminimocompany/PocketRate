package com.reganye.pocketrate.data.repository

import com.reganye.pocketrate.data.local.DataStoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStoreManager: DataStoreManager
) {
    val onboardingCompleted: Flow<Boolean> = dataStoreManager.onboardingCompleted
    suspend fun setOnboardingCompleted(completed: Boolean) = dataStoreManager.setOnboardingCompleted(completed)

    val themeMode: Flow<String> = dataStoreManager.themeMode
    suspend fun setThemeMode(mode: String) = dataStoreManager.setThemeMode(mode)

    val defaultCurrency: Flow<String> = dataStoreManager.defaultCurrency
    suspend fun setDefaultCurrency(code: String) = dataStoreManager.setDefaultCurrency(code)

    val favoriteCurrencies: Flow<List<String>> = dataStoreManager.favoriteCurrencies
    suspend fun setFavoriteCurrencies(codes: List<String>) = dataStoreManager.setFavoriteCurrencies(codes)

    val adFreeUntil: Flow<Long> = dataStoreManager.adFreeUntil
    suspend fun setAdFreeUntil(timestamp: Long) = dataStoreManager.setAdFreeUntil(timestamp)

    val isAdFree: Flow<Boolean> = adFreeUntil.map { it > System.currentTimeMillis() }

    val conversionCount: Flow<Int> = dataStoreManager.conversionCount
    suspend fun incrementConversionCount() = dataStoreManager.incrementConversionCount()
    suspend fun resetConversionCount() = dataStoreManager.resetConversionCount()

    private val interstitialMutex = Mutex()

    /**
     * Atomically checks the conversion count against [threshold]; if it is due,
     * resets the count to 0 and returns true so the caller can show one interstitial.
     * The mutex ensures two rapid conversions can't both pass the check.
     */
    suspend fun consumeInterstitialIfDue(threshold: Int = 5): Boolean =
        interstitialMutex.withLock {
            if ((conversionCount.first()) >= threshold) {
                resetConversionCount()
                true
            } else {
                false
            }
        }

    val lastSync: Flow<Long?> = dataStoreManager.lastSync
    suspend fun setLastSync(timestamp: Long) = dataStoreManager.setLastSync(timestamp)
}
