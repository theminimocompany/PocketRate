package com.reganye.pocketrate.data.remote

import com.google.gson.annotations.SerializedName

data class ExchangeRateResponse(
    val base: String,
    val date: String? = null,
    val rates: Map<String, Double>,
    @SerializedName("time_last_updated")
    val timeLastUpdated: Long? = null
)
