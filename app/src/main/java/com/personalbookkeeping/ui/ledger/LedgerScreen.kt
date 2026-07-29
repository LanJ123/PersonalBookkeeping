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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.personalbookkeeping.benchmark.BenchmarkUiSignals
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.LedgerTransaction
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.TransactionFilter
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.ui.privacy.displayCny
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    onMonthSelected: (String, Int) -> Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onClearMonth: () -> Unit,
    onClearFilters: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onRestore: () -> Unit,
    onDeletedConsumed: () -> Unit,
    onMessageConsumed: () -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("ledger-screen"),
        ) {
            OutlinedTextField(
                value = state.filter.noteQuery,
                onValueChange = onQueryChanged,
                label = { Text("搜索备注") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("ledger-search"),
            )
            MonthFilterBar(
                selectedMonth = state.selectedMonth,
                onOpenPicker = { showMonthPicker = true },
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onClear = onClearMonth,
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
                    modifier = Modifier.fillMaxSize().testTag("ledger-list"),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    items(transactions.itemCount) { index ->
                        val item = transactions[index] ?: return@items
                        val previousDay = if (index > 0) transactions.peek(index - 1)?.localDateEpochDay else null
                        val nextDay = if (index < transactions.itemCount - 1) {
                            transactions.peek(index + 1)?.localDateEpochDay
                        } else {
                            null
                        }
                        LedgerDaySegment(
                            item = item,
                            onClick = onTransactionClick,
                            isFirstOfDay = previousDay != item.localDateEpochDay,
                            isLastOfDay = nextDay != item.localDateEpochDay,
                            markReady = index == 0,
                        )
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
    if (showMonthPicker) {
        MonthPickerDialog(
            initial = state.selectedMonth ?: MonthPeriod.from(LocalDate.now()),
            onSelect = { year, month ->
                onMonthSelected(year, month).also { accepted ->
                    if (accepted) showMonthPicker = false
                }
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

@Composable
private fun MonthFilterBar(
    selectedMonth: MonthPeriod?,
    onOpenPicker: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClear: () -> Unit,
) {
    if (selectedMonth == null) {
        OutlinedButton(
            onClick = onOpenPicker,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("ledger-month-filter"),
        ) {
            Text("按月筛选")
        }
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onPrevious) { Text("上月") }
        OutlinedButton(
            onClick = onOpenPicker,
            modifier = Modifier.weight(1f).testTag("ledger-month-period"),
        ) {
            Text(selectedMonth.label)
        }
        OutlinedButton(onClick = onNext) { Text("下月") }
        TextButton(onClick = onClear) { Text("清除") }
    }
}

@Composable
private fun MonthPickerDialog(
    initial: MonthPeriod,
    onSelect: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var yearText by remember(initial.year) { mutableStateOf(initial.year.toString()) }
    var month by remember(initial.month) { mutableIntStateOf(initial.month) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择查询月份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.take(4) },
                    label = { Text("年份") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                (1..12).chunked(4).forEach { months ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        months.forEach { option ->
                            FilterChip(
                                selected = month == option,
                                onClick = { month = option },
                                label = { Text("${option}月") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(yearText, month) }) {
                Text("查询")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
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
private fun LedgerDaySegment(
    item: LedgerTransaction,
    onClick: (String) -> Unit,
    isFirstOfDay: Boolean,
    isLastOfDay: Boolean,
    markReady: Boolean,
) {
    val shape = when {
        isFirstOfDay && isLastOfDay -> RoundedCornerShape(16.dp)
        isFirstOfDay -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 0.dp,
            bottomStart = 0.dp,
        )
        isLastOfDay -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp,
        )
        else -> RoundedCornerShape(0.dp)
    }
    Surface(
        Modifier
            .padding(top = if (isFirstOfDay) 12.dp else 0.dp)
            .fillMaxWidth()
            .then(
                if (isFirstOfDay) {
                    Modifier.testTag("ledger-day-${item.localDateEpochDay}")
                } else {
                    Modifier
                },
            )
            .onGloballyPositioned {
                if (markReady) BenchmarkUiSignals.mark(BenchmarkUiSignals.LEDGER_READY)
            },
        shape = shape,
        tonalElevation = 1.dp,
    ) {
        Column {
            if (isFirstOfDay) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        LocalDate.ofEpochDay(item.localDateEpochDay).format(LEDGER_DAY_FORMAT),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "支 ${item.dailyExpense.displayCny()}  收 ${item.dailyIncome.displayCny()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
            LedgerTransactionRow(item, onClick)
            if (!isLastOfDay) {
                HorizontalDivider(Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun LedgerTransactionRow(
    item: LedgerTransaction,
    onClick: (String) -> Unit,
) {
    val flow = if (item.type == TransactionType.TRANSFER) {
        "${item.accountName} → ${item.targetAccountName.orEmpty()}"
    } else {
        item.accountName
    }
    val amount = when (item.type) {
        TransactionType.EXPENSE -> "-${item.amount.displayCny()}"
        TransactionType.INCOME -> item.amount.displayCny(showPositiveSign = true)
        TransactionType.TRANSFER -> item.amount.displayCny()
    }
    val localTime = item.occurredAt.atZone(item.zoneId).format(LEDGER_TIME_FORMAT)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("ledger-item")
            .clickable { onClick(item.id) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.categoryName ?: item.type.label(), fontWeight = FontWeight.SemiBold)
            Text(
                "$localTime · $flow",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
        Text(amount, fontWeight = FontWeight.SemiBold)
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

private val LEDGER_DAY_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
private val LEDGER_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.SIMPLIFIED_CHINESE)
