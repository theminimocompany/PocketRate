package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.CurrencyRepository
import com.reganye.pocketrate.data.repository.SettingsRepository
import com.reganye.pocketrate.domain.model.ConversionResult
import javax.inject.Inject

class ConvertCurrencyUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(amount: Double, from: String, to: String): Result<ConversionResult> {
        settingsRepository.incrementConversionCount()
        return currencyRepository.convert(amount, from, to)
    }
}
