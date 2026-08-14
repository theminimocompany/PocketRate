package com.reganye.pocketrate.presentation.ui.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.reganye.pocketrate.R
import com.reganye.pocketrate.presentation.ui.common.CurrencySelectorBottomSheet
import com.reganye.pocketrate.util.AdUnitIds
import com.reganye.pocketrate.util.ConsentManager
import com.reganye.pocketrate.util.DateFormatters
import com.reganye.pocketrate.util.findActivity
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    onNavigateToTrips: () -> Unit,
    onNavigateToCharts: (String, String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val showBanner by viewModel.showBanner.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.interstitialEvents.collect {
            val interstitialId = AdUnitIds.interstitial ?: return@collect
            if (!ConsentManager.canRequestAds(context)) return@collect
            val activity = context.findActivity() ?: return@collect
            InterstitialAd.load(
                context,
                interstitialId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        // The load is async; the captured activity may have been
                        // destroyed by a config change in the meantime.
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            ad.show(activity)
                        }
                    }

                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        // Ignore; we'll try again at the next trigger.
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            val bannerId = AdUnitIds.banner
            if (showBanner && bannerId != null && ConsentManager.canRequestAds(context)) {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val adSize = remember(screenWidth) {
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidth)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AndroidView(
                        factory = {
                            AdView(context).apply {
                                setAdSize(adSize)
                                adUnitId = bannerId
                                loadAd(AdRequest.Builder().build())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        onRelease = { adView -> adView.destroy() }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = viewModel::refreshRates,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main converter card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "CONVERT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Amount
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.amount).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = viewModel.amount,
                                onValueChange = { viewModel.amount = it },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Currency row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyChip(
                                label = stringResource(R.string.from),
                                code = viewModel.fromCurrency,
                                modifier = Modifier.weight(1f),
                                onClick = viewModel::openFromSelector
                            )

                            IconButton(
                                onClick = viewModel::swap,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = stringResource(R.string.swap),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            CurrencyChip(
                                label = stringResource(R.string.to),
                                code = viewModel.toCurrency,
                                modifier = Modifier.weight(1f),
                                onClick = viewModel::openToSelector
                            )
                        }

                        FilledTonalButton(
                            onClick = viewModel::convert,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.convert).uppercase(),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                // Result card
                viewModel.result?.let { conversion ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "%.2f ${conversion.from}".format(conversion.amount),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "%.2f ${conversion.to}".format(conversion.convertedAmount),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "1 ${conversion.from} = %.4f ${conversion.to}".format(conversion.rate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Error / status
                viewModel.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (viewModel.isStale) {
                    Text(
                        text = stringResource(R.string.stale_warning),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                viewModel.lastUpdated?.let { timestamp ->
                    val updatedText = stringResource(R.string.last_updated) + ": " +
                        DateFormatters.dateTimeDefault().format(Date(timestamp))
                    Text(
                        text = updatedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavigationChip(
                        label = stringResource(R.string.trips),
                        icon = Icons.Outlined.Luggage,
                        onClick = onNavigateToTrips,
                        modifier = Modifier.weight(1f)
                    )
                    NavigationChip(
                        label = stringResource(R.string.charts),
                        icon = Icons.Outlined.BarChart,
                        onClick = { onNavigateToCharts(viewModel.fromCurrency, viewModel.toCurrency) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (viewModel.isFromSelectorOpen) {
        CurrencySelectorBottomSheet(
            title = stringResource(R.string.from),
            query = viewModel.fromQuery,
            results = viewModel.fromResults,
            selectedCode = viewModel.fromCurrency,
            onQueryChange = viewModel::onFromQueryChanged,
            onSelected = viewModel::selectFromCurrency,
            onDismiss = viewModel::closeSelectors
        )
    }

    if (viewModel.isToSelectorOpen) {
        CurrencySelectorBottomSheet(
            title = stringResource(R.string.to),
            query = viewModel.toQuery,
            results = viewModel.toResults,
            selectedCode = viewModel.toCurrency,
            onQueryChange = viewModel::onToQueryChanged,
            onSelected = viewModel::selectToCurrency,
            onDismiss = viewModel::closeSelectors
        )
    }
}

@Composable
private fun CurrencyChip(
    label: String,
    code: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = code,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NavigationChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


