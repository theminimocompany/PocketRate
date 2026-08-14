package com.reganye.pocketrate.data.remote

import com.google.gson.annotations.SerializedName

data class FrankfurterResponse(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
