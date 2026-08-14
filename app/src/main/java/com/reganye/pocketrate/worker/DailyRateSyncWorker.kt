package com.reganye.pocketrate.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reganye.pocketrate.data.repository.CurrencyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class DailyRateSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val currencyRepository: CurrencyRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_rate_sync"
    }

    override suspend fun doWork(): Result {
        Timber.d("DailyRateSyncWorker started")
        return currencyRepository.syncRates().fold(
            onSuccess = {
                Timber.d("DailyRateSyncWorker succeeded")
                Result.success()
            },
            onFailure = { error ->
                Timber.e(error, "DailyRateSyncWorker failed")
                Result.retry()
            }
        )
    }
}
