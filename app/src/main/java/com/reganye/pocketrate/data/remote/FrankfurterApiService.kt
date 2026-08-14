package com.reganye.pocketrate.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FrankfurterApiService {
    @GET("latest")
    suspend fun getLatestRates(@Query("from") from: String = "USD"): Response<FrankfurterResponse>

    @GET("{date}")
    suspend fun getHistoricalRates(
        @Path("date") date: String,
        @Query("from") from: String = "USD"
    ): Response<FrankfurterResponse>

    @GET("{startDate}..{endDate}")
    suspend fun getTimeSeriesRates(
        @Path("startDate") startDate: String,
        @Path("endDate") endDate: String,
        @Query("from") from: String = "USD",
        @Query("to") to: String
    ): Response<FrankfurterTimeSeriesResponse>
}
