package com.reganye.pocketrate

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.reganye.pocketrate.worker.DailyRateSyncWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit as JavaTimeUnit
import javax.inject.Inject
import kotlin.concurrent.thread

@HiltAndroidApp
class PocketRateApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        thread { scheduleDailySync() }
    }

    private fun scheduleDailySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val initialDelay = calculateInitialDelayTo1430Utc()

        val syncRequest = PeriodicWorkRequestBuilder<DailyRateSyncWorker>(24, JavaTimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, JavaTimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyRateSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun calculateInitialDelayTo1430Utc(): Long {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val target = now.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, 14)
        target.set(Calendar.MINUTE, 30)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
