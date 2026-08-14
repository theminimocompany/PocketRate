package com.reganye.pocketrate.presentation.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Date
import androidx.hilt.navigation.compose.hiltViewModel
import com.reganye.pocketrate.R
import com.reganye.pocketrate.presentation.ui.common.CurrencySelectorBottomSheet
import com.reganye.pocketrate.presentation.ui.common.CurrencySelectorField
import com.reganye.pocketrate.util.DateFormatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    tripId: String,
    onExpenseSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val titleRes = if (viewModel.isEditing) R.string.edit_expense else R.string.add_expense

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = viewModel.amount,
                    onValueChange = viewModel::onAmountChanged,
                    label = { Text(stringResource(R.string.amount)) },
                    isError = viewModel.amountError,
                    supportingText = {
                        if (viewModel.amountError) {
                            Text(
                                text = stringResource(R.string.invalid_amount),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(2f)
                )
                CurrencySelectorField(
                    label = stringResource(R.string.currency),
                    code = viewModel.currency,
                    onClick = viewModel::openCurrencySelector,
                    modifier = Modifier.weight(1f)
                )
            }

            DateField(
                date = viewModel.date,
                onDateSelected = { viewModel.date = it }
            )

            if (viewModel.companions.isNotEmpty()) {
                FieldLabel(stringResource(R.string.payer))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.companions.forEach { companion ->
                        NordicFilterChip(
                            selected = viewModel.payerId == companion.id,
                            onClick = { viewModel.selectPayer(companion.id) },
                            label = companion.name
                        )
                    }
                }

                FieldLabel(stringResource(R.string.split))
                Text(
                    text = stringResource(R.string.split_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.companions.forEach { companion ->
                        NordicFilterChip(
                            selected = viewModel.selectedCompanionIds.contains(companion.id),
                            onClick = { viewModel.toggleCompanionSelection(companion.id) },
                            label = companion.name
                        )
                    }
                }
            }

            FieldLabel(stringResource(R.string.category))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.categories.chunked(3).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { category ->
                            NordicFilterChip(
                                selected = viewModel.category == category,
                                onClick = { viewModel.category = category },
                                label = category
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text(stringResource(R.string.description)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            LaunchedEffect(viewModel.amount, viewModel.currency, viewModel.date, viewModel.applyBuffer) {
                viewModel.recalculateSettlementPreview()
            }

            SettlementPreviewCard(
                preview = viewModel.settlementPreview,
                applyBuffer = viewModel.applyBuffer,
                onBufferChanged = viewModel::toggleBuffer
            )

            Button(
                onClick = { viewModel.saveExpense(onExpenseSaved) },
                enabled = viewModel.isLoaded,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.save).uppercase(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (viewModel.isCurrencySelectorOpen) {
        CurrencySelectorBottomSheet(
            title = stringResource(R.string.currency),
            query = viewModel.currencyQuery,
            results = viewModel.currencyResults,
            selectedCode = viewModel.currency,
            onQueryChange = viewModel::onCurrencyQueryChanged,
            onSelected = viewModel::selectCurrency,
            onDismiss = viewModel::closeCurrencySelector
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    date: Long,
    onDateSelected: (Long) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "${stringResource(R.string.date)}: ${DateFormatters.isoDateDefault().format(Date(date))}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                        showPicker = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SettlementPreviewCard(
    preview: AddExpenseViewModel.SettlementPreview?,
    applyBuffer: Boolean,
    onBufferChanged: (Boolean) -> Unit
) {
    if (preview == null) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FieldLabel(stringResource(R.string.settlement_preview))

        Text(
            text = "${stringResource(R.string.rate)}: ${"%.6f".format(preview.rate)} (${preview.rateDate})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "${stringResource(R.string.base_settle)}: ${"%.2f".format(preview.baseAmount)} ${preview.currency}",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(checked = applyBuffer, onCheckedChange = onBufferChanged)
            Text(
                text = stringResource(R.string.buffer_5_percent),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (applyBuffer) {
            Text(
                text = "${stringResource(R.string.final_settle)}: ${"%.2f".format(preview.bufferedAmount)} ${preview.currency}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NordicFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}
