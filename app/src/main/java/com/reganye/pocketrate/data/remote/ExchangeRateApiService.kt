package com.reganye.pocketrate.data.remote

import retrofit2.Response
import retrofit2.http.GET

interface ExchangeRateApiService {
    @GET("v4/latest/USD")
    suspend fun getLatestRates(): Response<ExchangeRateResponse>
}
