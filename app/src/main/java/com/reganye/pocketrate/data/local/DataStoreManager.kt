package com.reganye.pocketrate.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.reganye.pocketrate.domain.model.CurrencyConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pocketrate_prefs")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_LAST_SYNC = longPreferencesKey("last_sync_timestamp")
        val KEY_DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val KEY_FAVORITE_CURRENCIES = stringPreferencesKey("favorite_currencies")
        val KEY_AD_FREE_UNTIL = longPreferencesKey("ad_free_until")
        val KEY_CONVERSION_COUNT = intPreferencesKey("conversion_count")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    suspend fun setLastSync(timestamp: Long) = dataStore.edit { it[KEY_LAST_SYNC] = timestamp }
    val lastSync: Flow<Long?> = dataStore.data.map { it[KEY_LAST_SYNC] }

    suspend fun setDefaultCurrency(code: String) = dataStore.edit { it[KEY_DEFAULT_CURRENCY] = code }
    val defaultCurrency: Flow<String> = dataStore.data.map { it[KEY_DEFAULT_CURRENCY] ?: CurrencyConfig.DEFAULT_CURRENCY }

    suspend fun setFavoriteCurrencies(codes: List<String>) =
        dataStore.edit { it[KEY_FAVORITE_CURRENCIES] = codes.joinToString(",") }
    val favoriteCurrencies: Flow<List<String>> = dataStore.data.map {
        it[KEY_FAVORITE_CURRENCIES]?.split(",")?.filter { code -> code.isNotBlank() } ?: emptyList()
    }

    suspend fun setAdFreeUntil(timestamp: Long) = dataStore.edit { it[KEY_AD_FREE_UNTIL] = timestamp }
    val adFreeUntil: Flow<Long> = dataStore.data.map { it[KEY_AD_FREE_UNTIL] ?: 0L }

    suspend fun incrementConversionCount() = dataStore.edit { it[KEY_CONVERSION_COUNT] = (it[KEY_CONVERSION_COUNT] ?: 0) + 1 }
    val conversionCount: Flow<Int> = dataStore.data.map { it[KEY_CONVERSION_COUNT] ?: 0 }
    suspend fun resetConversionCount() = dataStore.edit { it[KEY_CONVERSION_COUNT] = 0 }

    suspend fun setThemeMode(mode: String) = dataStore.edit { it[KEY_THEME_MODE] = mode }
    val themeMode: Flow<String> = dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }

    suspend fun setOnboardingCompleted(completed: Boolean) =
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }
}
