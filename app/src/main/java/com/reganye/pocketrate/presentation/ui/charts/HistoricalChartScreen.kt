package com.reganye.pocketrate.presentation.ui.charts

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.MarkerCorneredShape
import com.reganye.pocketrate.R
import com.reganye.pocketrate.presentation.ui.common.CurrencySelectorBottomSheet
import com.reganye.pocketrate.util.DateFormatters
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalChartScreen(
    fromCurrency: String,
    toCurrency: String,
    onNavigateBack: () -> Unit,
    viewModel: HistoricalChartViewModel = hiltViewModel()
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    var isFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.rates) {
        if (viewModel.rates.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(viewModel.rates.map { it.rateAgainstUsd.toFloat() })
                }
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.charts),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CurrencyPairSelector(viewModel = viewModel)

                    if (viewModel.rates.isNotEmpty()) {
                        SummaryCards(viewModel = viewModel)
                    }

                    TimeRangeCard(viewModel = viewModel)
                }

                ChartCard(
                    viewModel = viewModel,
                    modelProducer = modelProducer,
                    onFullscreen = { isFullscreen = true },
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CurrencyPairSelector(viewModel = viewModel)

                if (viewModel.rates.isNotEmpty()) {
                    SummaryCards(viewModel = viewModel)
                }

                TimeRangeCard(viewModel = viewModel)

                ChartCard(
                    viewModel = viewModel,
                    modelProducer = modelProducer,
                    onFullscreen = { isFullscreen = true },
                    modifier = Modifier.height(360.dp)
                )
            }
        }
    }

    if (isFullscreen) {
        FullscreenChartDialog(
            fromCurrency = viewModel.fromCurrency,
            toCurrency = viewModel.toCurrency,
            daysBack = viewModel.daysBack,
            rates = viewModel.rates,
            onDismiss = { isFullscreen = false }
        )
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
private fun CurrencyPairSelector(
    viewModel: HistoricalChartViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyChip(
                label = stringResource(R.string.from),
                code = viewModel.fromCurrency,
                placeholder = "—",
                onClick = viewModel::openFromSelector,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = viewModel::swap,
                modifier = Modifier.size(44.dp),
                enabled = viewModel.fromCurrency.isNotBlank() && viewModel.toCurrency.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = stringResource(R.string.swap),
                    tint = if (viewModel.fromCurrency.isNotBlank() && viewModel.toCurrency.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }

            CurrencyChip(
                label = stringResource(R.string.to),
                code = viewModel.toCurrency,
                placeholder = "Select",
                onClick = viewModel::openToSelector,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CurrencyChip(
    label: String,
    code: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = code.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyLarge,
                color = if (code.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun SummaryCards(viewModel: HistoricalChartViewModel) {
    val current = viewModel.currentRate
    val high = viewModel.highRate
    val low = viewModel.lowRate
    val change = viewModel.rateChange

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Current",
            value = current?.let { "%.4f".format(it) } ?: "—",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "High",
            value = high?.let { "%.4f".format(it) } ?: "—",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Low",
            value = low?.let { "%.4f".format(it) } ?: "—",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Change",
            value = change?.let { "%.2f%%".format(it) } ?: "—",
            valueColor = when {
                change == null -> MaterialTheme.colorScheme.onSurface
                change > 0 -> MaterialTheme.colorScheme.primary
                change < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor
            )
        }
    }
}

@Composable
private fun TimeRangeCard(viewModel: HistoricalChartViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "TIME RANGE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    7 to "1W",
                    30 to "1M",
                    365 to "1Y",
                    1095 to "3Y",
                    1825 to "5Y"
                ).forEach { (days, label) ->
                    PeriodButton(
                        label = label,
                        selected = viewModel.daysBack == days,
                        onClick = {
                            viewModel.daysBack = days
                            viewModel.load()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    viewModel: HistoricalChartViewModel,
    modelProducer: CartesianChartModelProducer,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                viewModel.toCurrency.isBlank() -> {
                    EmptyChartMessage(
                        text = "Select a second currency to view the chart."
                    )
                }

                viewModel.isLoading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                viewModel.rates.isNotEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, end = 8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            IconButton(onClick = onFullscreen) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Full screen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        ChartContent(
                            rates = viewModel.rates,
                            daysBack = viewModel.daysBack,
                            modelProducer = modelProducer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        )
                    }
                }

                else -> {
                    EmptyChartMessage(
                        text = viewModel.error ?: "Historical data not available."
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChartMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    )
}

@Composable
private fun FullscreenChartDialog(
    fromCurrency: String,
    toCurrency: String,
    daysBack: Int,
    rates: List<com.reganye.pocketrate.domain.model.HistoricalRate>,
    onDismiss: () -> Unit
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(rates) {
        if (rates.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(rates.map { it.rateAgainstUsd.toFloat() })
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HISTORICAL RATES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$fromCurrency / $toCurrency",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    if (rates.isNotEmpty()) {
                        ChartContent(
                            rates = rates,
                            daysBack = daysBack,
                            modelProducer = modelProducer,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartContent(
    rates: List<com.reganye.pocketrate.domain.model.HistoricalRate>,
    daysBack: Int,
    modelProducer: CartesianChartModelProducer,
    modifier: Modifier = Modifier
) {
    val values = remember(rates) { rates.map { it.rateAgainstUsd.toFloat() } }
    val minRate = values.minOrNull() ?: 0f
    val maxRate = values.maxOrNull() ?: 0f
    val padding = ((maxRate - minRate) * 0.1f).coerceAtLeast(0.001f)

    val dates = remember(rates) { rates.map { it.date } }
    val dateParser = remember { DateFormatters.isoDateUs() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val xLabelSpacing = remember(daysBack) { calculateXAxisLabelSpacing(daysBack) }

    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            color = onPrimaryContainerColor,
            textSize = 14.sp,
            padding = insets(horizontal = 12.dp, vertical = 6.dp),
            background = rememberShapeComponent(
                fill = fill(primaryContainerColor),
                shape = MarkerCorneredShape(CorneredShape.Pill)
            ),
            lineCount = 2
        ),
        labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
        valueFormatter = remember(dates, daysBack) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                val target = targets.firstOrNull() as? LineCartesianLayerMarkerTarget
                val point = target?.points?.firstOrNull()
                val index = point?.entry?.x?.toInt() ?: 0
                val dateString = if (index in dates.indices) dates[index] else ""
                val rate = point?.entry?.y ?: 0.0
                formatMarkerLabel(dateString, rate, daysBack)
            }
        },
        indicator = { color ->
            shapeComponent(
                fill = fill(color),
                shape = CorneredShape.Pill,
                margins = insets(4.dp)
            )
        },
        guideline = rememberLineComponent(
            fill = fill(outlineColor.copy(alpha = 0.5f)),
            thickness = 1.dp
        )
    )

    val scrollState = rememberVicoScrollState(scrollEnabled = false)

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(primaryColor)),
                        stroke = LineCartesianLayer.LineStroke.Continuous(thicknessDp = 3f),
                        areaFill = LineCartesianLayer.AreaFill.single(
                            fill(primaryColor.copy(alpha = 0.12f))
                        ),
                        pointConnector = LineCartesianLayer.PointConnector.cubic()
                    )
                ),
                rangeProvider = remember(minRate, maxRate, padding) {
                    CartesianLayerRangeProvider.fixed(
                        minY = (minRate - padding).toDouble(),
                        maxY = (maxRate + padding).toDouble()
                    )
                }
            ),
            startAxis = VerticalAxis.rememberStart(
                label = rememberTextComponent(
                    color = onSurfaceVariantColor,
                    textSize = 12.sp
                ),
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    "%.3f".format(value)
                },
                guideline = rememberLineComponent(
                    fill = fill(outlineColor.copy(alpha = 0.2f))
                ),
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Outside,
                size = com.patrykandpatrick.vico.core.cartesian.axis.BaseAxis.Size.Fixed(48f)
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = rememberTextComponent(
                    color = onSurfaceVariantColor,
                    textSize = 12.sp
                ),
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    val index = value.toInt()
                    if (index in dates.indices) {
                        formatXAxisLabel(dates[index], daysBack)
                    } else {
                        // Vico queries out-of-range x values during rotation,
                        // zoom and resize — and throws on empty labels.
                        // A blank-but-nonempty label keeps that from crashing.
                        " "
                    }
                },
                guideline = null,
                itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { xLabelSpacing }) },
                size = com.patrykandpatrick.vico.core.cartesian.axis.BaseAxis.Size.Fixed(32f)
            ),
            marker = marker
        ),
        modelProducer = modelProducer,
        scrollState = scrollState,
        modifier = modifier
    )
}

@Composable
private fun PeriodButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = {},
            enabled = false,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
