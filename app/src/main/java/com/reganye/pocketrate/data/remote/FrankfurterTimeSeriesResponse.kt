package com.reganye.pocketrate.data.remote

import com.google.gson.annotations.SerializedName

data class FrankfurterTimeSeriesResponse(
    val base: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    val rates: Map<String, Map<String, Double>>
)
