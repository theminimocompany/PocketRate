package com.reganye.pocketrate.util

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import timber.log.Timber

object ConsentManager {

    fun requestConsent(activity: Activity, onComplete: (Boolean) -> Unit) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                if (consentInformation.isConsentFormAvailable) {
                    loadForm(activity, consentInformation, onComplete)
                } else {
                    onComplete(consentInformation.canRequestAds())
                }
            },
            { error ->
                Timber.e("Consent info update failed: ${error.message}")
                // UMP persists consent locally; fall back to the last known state
                // instead of disabling ads for the whole session.
                onComplete(consentInformation.canRequestAds())
            }
        )
    }

    fun resetConsent(activity: Activity, onComplete: (Boolean) -> Unit) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.reset()
        requestConsent(activity, onComplete)
    }

    fun canRequestAds(context: Context): Boolean {
        return UserMessagingPlatform.getConsentInformation(context).canRequestAds()
    }

    private fun loadForm(
        activity: Activity,
        consentInformation: ConsentInformation,
        onComplete: (Boolean) -> Unit
    ) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form ->
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    form.show(activity) { error ->
                        if (error != null) {
                            Timber.e("Consent form show failed: ${error.message}")
                        }
                        onComplete(canRequestAds(activity))
                    }
                } else {
                    onComplete(canRequestAds(activity))
                }
            },
            { error ->
                Timber.e("Consent form load failed: ${error.message}")
                onComplete(consentInformation.canRequestAds())
            }
        )
    }
}
