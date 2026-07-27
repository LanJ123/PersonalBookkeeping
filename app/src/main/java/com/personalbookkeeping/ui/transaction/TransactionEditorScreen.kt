package com.personalbookkeeping.ui.transaction

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personalbookkeeping.R
import com.personalbookkeeping.benchmark.BenchmarkUiSignals
import com.personalbookkeeping.common.MoneyParseFailure
import com.personalbookkeeping.domain.model.AccountOption
import com.personalbookkeeping.domain.model.CategoryOption
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.domain.validation.TransactionValidationError
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorScreen(
    state: TransactionEditorUiState,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onAccountSelected: (String) -> Unit,
    onTargetAccountSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .onGloballyPositioned {
                BenchmarkUiSignals.mark(BenchmarkUiSignals.EDITOR_READY)
            },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (state.isEditing) "编辑流水" else stringResourceSafe(R.string.transaction_editor_title))
                        Text(
                            text = stringResourceSafe(R.string.iteration_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) TextButton(onClick = onBack) { Text("返回") }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TransactionTypeSelector(
                    selected = state.type,
                    onSelected = onTypeSelected,
                )
            }

            item {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = onAmountChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction-amount"),
                    label = { Text(stringResourceSafe(R.string.amount_label)) },
                    placeholder = { Text(stringResourceSafe(R.string.amount_placeholder)) },
                    prefix = { Text("¥") },
                    singleLine = true,
                    isError = state.amountError != null,
                    supportingText = state.amountError?.let { error ->
                        { Text(stringResourceSafe(error.messageResource())) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                )
            }

            if (state.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Text(stringResourceSafe(R.string.initializing_data))
                    }
                }
            } else {
                if (state.type == TransactionType.TRANSFER) {
                    item {
                        AccountSelector(
                            labelResource = R.string.source_account_label,
                            accounts = state.accounts,
                            selectedId = state.selectedAccountId,
                            onSelected = onAccountSelected,
                        )
                    }
                    item {
                        AccountSelector(
                            labelResource = R.string.target_account_label,
                            accounts = state.accounts,
                            selectedId = state.selectedTargetAccountId,
                            onSelected = onTargetAccountSelected,
                        )
                    }
                } else {
                    item {
                        CategorySelector(
                            categories = state.visibleCategories,
                            selectedId = state.selectedCategoryId,
                            onSelected = onCategorySelected,
                        )
                    }
                    item {
                        AccountSelector(
                            labelResource = R.string.account_label,
                            accounts = state.accounts,
                            selectedId = state.selectedAccountId,
                            onSelected = onAccountSelected,
                        )
                    }
                }
            }

            item {
                TransactionDateTimeSelector(
                    state = state,
                    onDateSelected = onDateSelected,
                    onTimeSelected = onTimeSelected,
                )
            }

            item {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResourceSafe(R.string.note_label)) },
                    minLines = 2,
                    maxLines = 4,
                )
            }

            state.validationErrors.firstOrNull()?.let { error ->
                item {
                    Text(
                        text = stringResourceSafe(error.messageResource()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            when (state.saveStatus) {
                SaveStatus.SAVED -> item {
                    Text(
                        text = stringResourceSafe(R.string.save_success),
                        modifier = Modifier
                            .testTag("transaction-save-success")
                            .onGloballyPositioned {
                                BenchmarkUiSignals.mark(BenchmarkUiSignals.SAVE_READY)
                            },
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                SaveStatus.FAILED -> item {
                    Text(
                        stringResourceSafe(R.string.save_failure),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                SaveStatus.IDLE -> Unit
            }

            item {
                val hasRequiredOptions = state.accounts.isNotEmpty() && when (state.type) {
                    TransactionType.TRANSFER -> state.accounts.size >= 2
                    else -> state.visibleCategories.isNotEmpty()
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("transaction-save"),
                    enabled = !state.isLoading && !state.isSaving && hasRequiredOptions,
                ) {
                    Text(
                        if (state.isSaving) {
                            stringResourceSafe(R.string.saving_transaction)
                        } else {
                            if (state.isEditing) "保存修改" else stringResourceSafe(R.string.save_transaction)
                        },
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDateTimeSelector(
    state: TransactionEditorUiState,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
) {
    val localDateTime = state.occurredAt.atZone(state.zoneId)
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("日期", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().testTag("transaction-date"),
                ) {
                    Text(
                        localDateTime.toLocalDate().format(
                            DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.SIMPLIFIED_CHINESE),
                        ),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("时间", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth().testTag("transaction-time"),
                ) {
                    Text(localDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                }
            }
        }
    }

    if (showDatePicker) {
        TransactionDatePickerDialog(
            initialEpochDay = localDateTime.toLocalDate().toEpochDay(),
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateSelected(it)
                showDatePicker = false
            },
        )
    }
    if (showTimePicker) {
        TransactionTimePickerDialog(
            initialHour = localDateTime.hour,
            initialMinute = localDateTime.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                onTimeSelected(hour, minute)
                showTimePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePickerDialog(
    initialEpochDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val initialMillis = java.time.LocalDate.ofEpochDay(initialEpochDay)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { selectedMillis ->
                        val epochDay = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toEpochDay()
                        onConfirm(epochDay)
                    }
                },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text("确定")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TransactionTypeSelector(
    selected: TransactionType,
    onSelected: (TransactionType) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TransactionType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelected(type) },
                label = { Text(stringResourceSafe(type.labelResource())) },
            )
        }
    }
}

@Composable
private fun AccountSelector(
    @StringRes labelResource: Int,
    accounts: List<AccountOption>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    OptionSelector(
        labelResource = labelResource,
        options = accounts.map { it.id to it.name },
        selectedId = selectedId,
        onSelected = onSelected,
    )
}

@Composable
private fun CategorySelector(
    categories: List<CategoryOption>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    OptionSelector(
        labelResource = R.string.category_label,
        options = categories.map { it.id to it.name },
        selectedId = selectedId,
        onSelected = onSelected,
    )
}

@Composable
private fun OptionSelector(
    @StringRes labelResource: Int,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResourceSafe(labelResource),
            style = MaterialTheme.typography.labelLarge,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options, key = { it.first }) { (id, name) ->
                FilterChip(
                    selected = id == selectedId,
                    onClick = { onSelected(id) },
                    label = { Text(name) },
                )
            }
        }
    }
}

@Composable
private fun stringResourceSafe(@StringRes id: Int, vararg formatArgs: Any): String =
    androidx.compose.ui.res.stringResource(id, *formatArgs)

@StringRes
private fun TransactionType.labelResource(): Int = when (this) {
    TransactionType.EXPENSE -> R.string.transaction_type_expense
    TransactionType.INCOME -> R.string.transaction_type_income
    TransactionType.TRANSFER -> R.string.transaction_type_transfer
}

@StringRes
private fun MoneyParseFailure.messageResource(): Int = when (this) {
    MoneyParseFailure.EMPTY -> R.string.amount_error_empty
    MoneyParseFailure.INVALID_FORMAT -> R.string.amount_error_invalid
    MoneyParseFailure.NON_POSITIVE -> R.string.amount_error_non_positive
    MoneyParseFailure.TOO_MANY_FRACTION_DIGITS -> R.string.amount_error_precision
    MoneyParseFailure.ABOVE_MAXIMUM -> R.string.amount_error_above_max
}

@StringRes
private fun TransactionValidationError.messageResource(): Int = when (this) {
    TransactionValidationError.ACCOUNT_REQUIRED -> R.string.validation_account_required
    TransactionValidationError.CATEGORY_REQUIRED -> R.string.validation_category_required
    TransactionValidationError.TARGET_ACCOUNT_REQUIRED -> R.string.validation_target_required
    TransactionValidationError.ACCOUNTS_MUST_DIFFER -> R.string.validation_accounts_differ
    TransactionValidationError.NOTE_TOO_LONG -> R.string.validation_note_too_long
    else -> R.string.validation_generic
}
