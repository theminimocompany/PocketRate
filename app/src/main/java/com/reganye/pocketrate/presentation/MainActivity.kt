package com.reganye.pocketrate.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.ads.MobileAds
import com.reganye.pocketrate.R
import com.reganye.pocketrate.presentation.navigation.PocketRateNavHost
import com.reganye.pocketrate.presentation.navigation.PocketRateNavHostViewModel
import com.reganye.pocketrate.presentation.theme.PocketRateTheme
import com.reganye.pocketrate.util.ConsentManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlin.concurrent.thread

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val navHostViewModel: PocketRateNavHostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the splash screen visible until we know whether onboarding has been completed.
        splashScreen.setKeepOnScreenCondition {
            navHostViewModel.onboardingCompleted.value == null
        }

        // Show the UI immediately; ads must not gate app startup.
        setAppContent(navHostViewModel)

        ConsentManager.requestConsent(this) { canRequestAds ->
            val appId = getString(R.string.admob_app_id)
            if (canRequestAds && appId.isNotBlank()) {
                // Google recommends initializing MobileAds off the main thread.
                thread {
                    MobileAds.initialize(this@MainActivity)
                }
            } else {
                Timber.d("Ads disabled: consent=$canRequestAds, appId blank=${appId.isBlank()}")
            }
        }
    }

    private fun setAppContent(navHostViewModel: PocketRateNavHostViewModel) {
        setContent {
            val darkTheme by navHostViewModel.darkTheme.collectAsState()
            PocketRateTheme(darkTheme = darkTheme ?: isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PocketRateNavHost(viewModel = navHostViewModel)
                }
            }
        }
    }
}
