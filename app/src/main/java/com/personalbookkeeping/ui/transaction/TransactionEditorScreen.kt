package com.personalbookkeeping.ui.transaction

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personalbookkeeping.R
import com.personalbookkeeping.common.MoneyParseFailure
import com.personalbookkeeping.domain.model.AccountOption
import com.personalbookkeeping.domain.model.CategoryOption
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.ui.privacy.displayCny
import com.personalbookkeeping.domain.validation.TransactionValidationError
import java.text.DateFormat
import java.util.Date

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
    onSave: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Scaffold(
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
                    modifier = Modifier.fillMaxWidth(),
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
                        stringResourceSafe(R.string.save_success),
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
                        .height(52.dp),
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

            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResourceSafe(R.string.recent_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (!state.isLoading && state.recentTransactions.isEmpty()) {
                item {
                    Text(
                        text = stringResourceSafe(R.string.recent_transactions_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.recentTransactions, key = { it.id }) { transaction ->
                    RecentTransactionCard(transaction)
                }
            }
        }
    }
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
private fun RecentTransactionCard(transaction: RecentTransaction) {
    val descriptor = when (transaction.type) {
        TransactionType.TRANSFER -> stringResourceSafe(
            R.string.transaction_flow,
            transaction.accountName,
            transaction.targetAccountName.orEmpty(),
        )

        else -> stringResourceSafe(
            R.string.transaction_category_account,
            transaction.categoryName.orEmpty(),
            transaction.accountName,
        )
    }
    val amount = when (transaction.type) {
        TransactionType.EXPENSE -> "-${transaction.amount.displayCny()}"
        TransactionType.INCOME -> transaction.amount.displayCny(showPositiveSign = true)
        TransactionType.TRANSFER -> transaction.amount.displayCny()
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResourceSafe(transaction.type.labelResource()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = descriptor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date.from(transaction.occurredAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
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
