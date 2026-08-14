package com.reganye.pocketrate.data.remote

import com.reganye.pocketrate.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val EXCHANGE_RATE_BASE_URL = "https://api.exchangerate-api.com/"
    private const val FRANKFURTER_BASE_URL = "https://api.frankfurter.dev/v1/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
            }
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
        }.build()
    }

    @Provides
    @Singleton
    @Named("exchangeRateRetrofit")
    fun provideExchangeRateRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(EXCHANGE_RATE_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("frankfurterRetrofit")
    fun provideFrankfurterRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(FRANKFURTER_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideExchangeRateApiService(
        @Named("exchangeRateRetrofit") retrofit: Retrofit
    ): ExchangeRateApiService = retrofit.create(ExchangeRateApiService::class.java)

    @Provides
    @Singleton
    fun provideFrankfurterApiService(
        @Named("frankfurterRetrofit") retrofit: Retrofit
    ): FrankfurterApiService = retrofit.create(FrankfurterApiService::class.java)
}
