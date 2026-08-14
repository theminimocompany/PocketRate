package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.CurrencyRepository
import javax.inject.Inject

class ShouldRefreshRatesUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {
    suspend operator fun invoke(): Boolean = currencyRepository.isCacheStale()
}
