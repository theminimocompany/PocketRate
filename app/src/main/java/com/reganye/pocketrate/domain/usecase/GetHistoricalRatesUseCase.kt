package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.CurrencyRepository
import com.reganye.pocketrate.domain.model.HistoricalRate
import javax.inject.Inject

class GetHistoricalRatesUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {
    suspend operator fun invoke(currencyCode: String, daysBack: Int): Result<List<HistoricalRate>> {
        val rates = currencyRepository.getHistoricalRates(currencyCode, daysBack)
        return if (rates.size < daysBack / 2) {
            currencyRepository.backfillHistoricalRates(currencyCode, daysBack)
                .onFailure { return Result.failure(it) }
            Result.success(currencyRepository.getHistoricalRates(currencyCode, daysBack))
        } else {
            Result.success(rates)
        }
    }
}
