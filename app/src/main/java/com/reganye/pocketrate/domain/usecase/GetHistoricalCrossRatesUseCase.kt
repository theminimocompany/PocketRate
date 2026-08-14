package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.CurrencyRepository
import com.reganye.pocketrate.domain.model.HistoricalRatesResult
import javax.inject.Inject

class GetHistoricalCrossRatesUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {
    suspend operator fun invoke(
        fromCurrency: String,
        toCurrency: String,
        daysBack: Int
    ): Result<HistoricalRatesResult> = currencyRepository.getHistoricalCrossRates(fromCurrency, toCurrency, daysBack)
}
