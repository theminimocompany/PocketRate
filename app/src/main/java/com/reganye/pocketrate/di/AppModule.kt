package com.reganye.pocketrate.di

import android.content.Context
import androidx.room.Room
import com.reganye.pocketrate.data.local.AppDatabase
import com.reganye.pocketrate.data.local.DataStoreManager
import com.reganye.pocketrate.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pocketrate_db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .build()
    }

    @Provides
    fun provideExchangeRateDao(database: AppDatabase) = database.exchangeRateDao()

    @Provides
    fun provideHistoricalRateDao(database: AppDatabase) = database.historicalRateDao()

    @Provides
    fun provideTripDao(database: AppDatabase) = database.tripDao()

    @Provides
    fun provideExpenseDao(database: AppDatabase) = database.expenseDao()

    @Provides
    fun provideCompanionDao(database: AppDatabase) = database.companionDao()

    @Provides
    fun provideExpenseSplitDao(database: AppDatabase) = database.expenseSplitDao()

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStoreManager: DataStoreManager): SettingsRepository {
        return SettingsRepository(dataStoreManager)
    }
}
