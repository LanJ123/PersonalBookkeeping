package com.personalbookkeeping.ui.ledger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.LedgerTransaction
import com.personalbookkeeping.domain.model.TransactionFilter
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.ui.privacy.displayCny
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    state: LedgerUiState,
    transactions: LazyPagingItems<LedgerTransaction>,
    snackbarHostState: SnackbarHostState,
    onQueryChanged: (String) -> Unit,
    onTypeChanged: (TransactionType?) -> Unit,
    onAccountChanged: (String?) -> Unit,
    onCategoryChanged: (String?) -> Unit,
    onDateChanged: (String, String) -> Boolean,
    onClearFilters: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onRestore: () -> Unit,
    onDeletedConsumed: () -> Unit,
    onMessageConsumed: () -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }
    LaunchedEffect(state.lastDeleted?.id) {
        if (state.lastDeleted != null) {
            val result = snackbarHostState.showSnackbar("流水已删除", "撤销")
            if (result == SnackbarResult.ActionPerformed) onRestore() else onDeletedConsumed()
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageConsumed()
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("流水") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.filter.noteQuery,
                onValueChange = onQueryChanged,
                label = { Text("搜索备注") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterChip(selected = state.filter.type == null, onClick = { onTypeChanged(null) }, label = { Text("全部") }) }
                items(TransactionType.entries) { type ->
                    FilterChip(
                        selected = state.filter.type == type,
                        onClick = { onTypeChanged(type) },
                        label = { Text(type.label()) },
                    )
                }
                item {
                    OutlinedButton(onClick = { showFilters = true }) {
                        Text(if (state.filter.isActive) "更多筛选 · 已启用" else "更多筛选")
                    }
                }
            }
            when {
                state.isInitializing ->
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { CircularProgressIndicator() }
                transactions.itemCount == 0 && state.filter.isActive -> EmptyLedger(
                    "没有符合条件的流水",
                    onClearFilters,
                )
                transactions.loadState.refresh is LoadState.Loading ->
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { CircularProgressIndicator() }
                transactions.loadState.refresh is LoadState.Error -> EmptyLedger("加载失败，请稍后重试")
                transactions.itemCount == 0 -> EmptyLedger(
                    if (state.filter.isActive) "没有符合条件的流水" else "还没有流水，点击“记一笔”开始",
                    if (state.filter.isActive) onClearFilters else null,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(transactions.itemCount) { index ->
                        val item = transactions[index] ?: return@items
                        val previousDay = if (index > 0) transactions.peek(index - 1)?.localDateEpochDay else null
                        if (previousDay != item.localDateEpochDay) DailyHeader(item)
                        TransactionCard(item, onTransactionClick)
                    }
                    if (transactions.loadState.append is LoadState.Loading) {
                        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
                    }
                }
            }
        }
    }
    if (showFilters) {
        FilterDialog(
            filter = state.filter,
            state = state,
            onAccountChanged = onAccountChanged,
            onCategoryChanged = onCategoryChanged,
            onDateChanged = onDateChanged,
            onClear = { onClearFilters(); showFilters = false },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun EmptyLedger(message: String, onClear: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onClear != null) TextButton(onClick = onClear) { Text("清除筛选") }
    }
}

@Composable
private fun DailyHeader(item: LedgerTransaction) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(LocalDate.ofEpochDay(item.localDateEpochDay).format(DateTimeFormatter.ofPattern("M月d日 EEEE")), fontWeight = FontWeight.SemiBold)
        Text("支 ${item.dailyExpense.displayCny()} · 收 ${item.dailyIncome.displayCny()}", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TransactionCard(item: LedgerTransaction, onClick: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick(item.id) }) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.categoryName ?: item.type.label(), fontWeight = FontWeight.SemiBold)
                val flow = if (item.type == TransactionType.TRANSFER) {
                    "${item.accountName} → ${item.targetAccountName.orEmpty()}"
                } else item.accountName
                Text(flow, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                item.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
            }
            val amount = when (item.type) {
                TransactionType.EXPENSE -> "-${item.amount.displayCny()}"
                TransactionType.INCOME -> item.amount.displayCny(showPositiveSign = true)
                TransactionType.TRANSFER -> item.amount.displayCny()
            }
            Text(amount, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FilterDialog(
    filter: TransactionFilter,
    state: LedgerUiState,
    onAccountChanged: (String?) -> Unit,
    onCategoryChanged: (String?) -> Unit,
    onDateChanged: (String, String) -> Boolean,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var from by remember(filter.fromEpochDay) { mutableStateOf(filter.fromEpochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()) }
    var to by remember(filter.toEpochDay) { mutableStateOf(filter.toEpochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("组合筛选") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("账户", fontWeight = FontWeight.SemiBold) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(filter.accountId == null, { onAccountChanged(null) }, { Text("全部") }) }
                        items(state.options.accounts) { account ->
                            FilterChip(filter.accountId == account.id, { onAccountChanged(account.id) }, { Text(account.name) })
                        }
                    }
                }
                item { Text("分类", fontWeight = FontWeight.SemiBold) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(filter.categoryId == null, { onCategoryChanged(null) }, { Text("全部") }) }
                        items(state.options.categories.filter { option ->
                            filter.type == null || filter.type == TransactionType.TRANSFER ||
                                (filter.type == TransactionType.EXPENSE && option.kind == CategoryKind.EXPENSE) ||
                                (filter.type == TransactionType.INCOME && option.kind == CategoryKind.INCOME)
                        }) { category ->
                            FilterChip(filter.categoryId == category.id, { onCategoryChanged(category.id) }, { Text(category.name) })
                        }
                    }
                }
                item { OutlinedTextField(from, { from = it }, label = { Text("开始日期 YYYY-MM-DD") }, singleLine = true) }
                item { OutlinedTextField(to, { to = it }, label = { Text("结束日期 YYYY-MM-DD") }, singleLine = true) }
            }
        },
        confirmButton = { Button(onClick = { if (onDateChanged(from, to)) onDismiss() }) { Text("应用") } },
        dismissButton = { TextButton(onClick = onClear) { Text("清除全部") } },
    )
}

internal fun TransactionType.label(): String = when (this) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "转账"
}
