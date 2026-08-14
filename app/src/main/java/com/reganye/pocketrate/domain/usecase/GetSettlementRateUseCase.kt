package com.reganye.pocketrate.domain.usecase

import com.reganye.pocketrate.data.repository.CurrencyRepository
import com.reganye.pocketrate.util.DateFormatters
import javax.inject.Inject

/**
 * Resolves the exchange rate to use when converting an expense into a trip's
 * settlement currency.
 *
 * The rate is looked up for the expense date so settlements reflect the actual
 * cost at the time of purchase.
 */
class GetSettlementRateUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {
    suspend operator fun invoke(
        expenseCurrency: String,
        settlementCurrency: String,
        dateTimestamp: Long
    ): Result<Double> {
        val date = DateFormatters.isoDateUs().format(dateTimestamp)
        return currencyRepository.getRateForDate(expenseCurrency, settlementCurrency, date)
    }
}
