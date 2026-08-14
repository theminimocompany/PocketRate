package com.reganye.pocketrate.util

import com.reganye.pocketrate.BuildConfig

/**
 * Provides AdMob ad unit IDs.
 *
 * In debug builds the official Google test IDs are returned.
 * In release builds the IDs come from `build.gradle.kts`; if they are blank
 * (the default before you configure production IDs), loading ads is skipped.
 */
object AdUnitIds {

    private fun String?.orNullIfBlank(): String? = if (isNullOrBlank()) null else this

    val banner: String?
        get() = BuildConfig.ADMOB_BANNER_ID.orNullIfBlank()

    val interstitial: String?
        get() = BuildConfig.ADMOB_INTERSTITIAL_ID.orNullIfBlank()

    val rewarded: String?
        get() = BuildConfig.ADMOB_REWARDED_ID.orNullIfBlank()
}
