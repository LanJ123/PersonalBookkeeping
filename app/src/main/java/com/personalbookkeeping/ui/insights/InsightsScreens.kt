package com.personalbookkeeping.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.BudgetProgress
import com.personalbookkeeping.domain.model.BudgetStatus
import com.personalbookkeeping.domain.model.DailyTrend
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: InsightsUiState,
    snackbarHostState: SnackbarHostState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewAll: (MonthPeriod) -> Unit,
    onTransactionClick: (String) -> Unit,
    onBudgets: () -> Unit,
    onMessageConsumed: () -> Unit,
) {
    MessageEffect(state, snackbarHostState, onMessageConsumed)
    Scaffold(
        topBar = { TopAppBar(title = { Text("个人记账") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) CenterLoading(padding) else LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthSwitcher(state.period, onPreviousMonth, onNextMonth) }
            item { SummaryCard(state) }
            item {
                val budget = state.insights.totalBudget
                if (budget == null) {
                    Card(Modifier.fillMaxWidth().clickable(onClick = onBudgets)) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column { Text("本月预算", fontWeight = FontWeight.SemiBold); Text("尚未设置", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text("去设置")
                        }
                    }
                } else BudgetCard("本月预算", budget, onBudgets)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("最近流水", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onViewAll(state.period) }) { Text("查看全部") }
                }
            }
            if (state.insights.recentTransactions.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("这个月还没有流水")
                        Text("点击右下角，记下第一笔", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(state.insights.recentTransactions, key = { it.id }) { transaction ->
                    RecentTransactionCard(transaction, onTransactionClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: InsightsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCategoryClick: (MonthPeriod, String) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("统计") }) }) { padding ->
        if (state.isLoading) CenterLoading(padding) else LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthSwitcher(state.period, onPreviousMonth, onNextMonth) }
            item { SummaryCard(state) }
            item { Text("支出分类排行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (state.insights.categories.isEmpty()) {
                item { EmptyCard("本月暂无支出，分类排行将在记账后显示") }
            } else {
                items(state.insights.categories, key = { it.categoryId }) { category ->
                    val total = state.insights.summary.expense.minorUnits
                    val share = if (total == 0L) 0f else category.amount.minorUnits.toFloat() / total.toFloat()
                    Card(Modifier.fillMaxWidth().clickable { onCategoryClick(state.period, category.categoryId) }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(category.categoryName, fontWeight = FontWeight.SemiBold)
                                Text(category.amount.formatCny())
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(progress = { share.coerceIn(0f, 1f) }, modifier = Modifier.weight(1f))
                                Text("${(share * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                            }
                            Text("查看对应流水", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { Text("每日收支趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (state.insights.dailyTrend.isEmpty()) item { EmptyCard("本月暂无收支趋势") }
            else item { DailyTrendChart(state.insights.dailyTrend) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    state: InsightsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSave: (String?, String) -> Boolean,
    onClear: (String?) -> Unit,
    onMessageConsumed: () -> Unit,
) {
    var editing by remember { mutableStateOf<BudgetEditTarget?>(null) }
    MessageEffect(state, snackbarHostState, onMessageConsumed)
    Scaffold(
        topBar = { TopAppBar(title = { Text("预算管理") }, navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) CenterLoading(padding) else LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthSwitcher(state.period, onPreviousMonth, onNextMonth) }
            item { Text("总支出预算", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            item {
                val total = state.insights.totalBudget
                if (total == null) BudgetUnsetCard("本月总预算") { editing = BudgetEditTarget(null, "本月总预算", null) }
                else BudgetManagementCard("本月总预算", total, { editing = BudgetEditTarget(null, "本月总预算", total.limit) }) { onClear(null) }
            }
            item { Text("分类预算（可选）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
            items(state.insights.expenseCategories, key = { it.id }) { category ->
                val budget = state.insights.categoryBudgets.firstOrNull { it.categoryId == category.id }
                if (budget == null) BudgetUnsetCard(category.name) { editing = BudgetEditTarget(category.id, category.name, null) }
                else BudgetManagementCard(category.name, budget, { editing = BudgetEditTarget(category.id, category.name, budget.limit) }) { onClear(category.id) }
            }
            item { Text("分类预算相互独立，总和可以超过总预算。预算提示不会阻止继续记账。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    editing?.let { target ->
        BudgetDialog(target, { editing = null }) { amount -> if (onSave(target.categoryId, amount)) editing = null }
    }
}

@Composable
private fun MonthSwitcher(period: MonthPeriod, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onPrevious) { Text("上月") }
        Text(period.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = onNext) { Text("下月") }
    }
}

@Composable
private fun SummaryCard(state: InsightsUiState) {
    val summary = state.insights.summary
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("本月支出", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(summary.expense.formatCny(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryValue("收入", summary.income)
                SummaryValue("结余", summary.balance)
                SummaryValue("流水", null, "${summary.transactionCount} 笔")
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, money: Money?, text: String? = null) {
    Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(text ?: money!!.formatCny(), fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun BudgetCard(title: String, budget: BudgetProgress, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.SemiBold); Text(budget.status.label(), color = budget.status.color()) }
            Text("已用 ${budget.used.formatCny()} / ${budget.limit.formatCny()}")
            LinearProgressIndicator(progress = { budget.progressFraction }, modifier = Modifier.fillMaxWidth())
            Text("剩余 ${budget.remaining.formatCny()}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RecentTransactionCard(transaction: RecentTransaction, onClick: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick(transaction.id) }) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(transaction.categoryName ?: transaction.type.label(), fontWeight = FontWeight.SemiBold)
                val date = transaction.occurredAt.atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("M月d日"))
                Text("$date · ${transaction.accountName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(transaction.signedAmount(), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DailyTrendChart(trend: List<DailyTrend>) {
    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor = MaterialTheme.colorScheme.primary
    val max = trend.maxOf { maxOf(it.expense.minorUnits, it.income.minorUnits) }.coerceAtLeast(1L)
    val description = trend.joinToString("；") {
        val day = LocalDate.ofEpochDay(it.epochDay).dayOfMonth
        "${day}日收入${it.income.formatCny()}，支出${it.expense.formatCny()}"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) { Text("收入", color = incomeColor); Text("支出", color = expenseColor) }
            Canvas(Modifier.fillMaxWidth().height(160.dp).semantics { contentDescription = description }) {
                val step = size.width / trend.size.coerceAtLeast(1)
                val barWidth = (step * 0.28f).coerceAtLeast(2f)
                trend.forEachIndexed { index, point ->
                    val center = step * (index + 0.5f)
                    val incomeHeight = size.height * point.income.minorUnits / max.toFloat()
                    val expenseHeight = size.height * point.expense.minorUnits / max.toFloat()
                    drawLine(incomeColor, Offset(center - barWidth, size.height), Offset(center - barWidth, size.height - incomeHeight), strokeWidth = barWidth)
                    drawLine(expenseColor, Offset(center + barWidth, size.height), Offset(center + barWidth, size.height - expenseHeight), strokeWidth = barWidth)
                }
            }
            Text("共 ${trend.size} 个有收支记录的日期；图形仅作趋势辅助，准确金额以汇总和流水为准。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BudgetUnsetCard(name: String, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text(name, fontWeight = FontWeight.SemiBold); Text("未设置", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = onEdit) { Text("设置") }
        }
    }
}

@Composable
private fun BudgetManagementCard(name: String, budget: BudgetProgress, onEdit: () -> Unit, onClear: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, fontWeight = FontWeight.SemiBold); Text(budget.status.label(), color = budget.status.color()) }
            Text("已用 ${budget.used.formatCny()} / ${budget.limit.formatCny()}")
            LinearProgressIndicator(progress = { budget.progressFraction }, modifier = Modifier.fillMaxWidth())
            Row { TextButton(onClick = onEdit) { Text("编辑") }; TextButton(onClick = onClear) { Text("清除") } }
        }
    }
}

private data class BudgetEditTarget(val categoryId: String?, val name: String, val current: Money?)

@Composable
private fun BudgetDialog(target: BudgetEditTarget, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var amount by remember(target.categoryId, target.name) { mutableStateOf(target.current?.toPlainDecimal().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置${target.name}") },
        text = { OutlinedTextField(amount, { amount = it.take(18) }, label = { Text("预算金额") }, prefix = { Text("¥") }, singleLine = true) },
        confirmButton = { Button(onClick = { onSave(amount) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun EmptyCard(message: String) {
    Card(Modifier.fillMaxWidth()) { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}

@Composable
private fun CenterLoading(padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun MessageEffect(state: InsightsUiState, snackbarHostState: SnackbarHostState, onConsumed: () -> Unit) {
    LaunchedEffect(state.message) { state.message?.let { snackbarHostState.showSnackbar(it); onConsumed() } }
}

private fun BudgetStatus.label() = when (this) {
    BudgetStatus.NORMAL -> "预算正常"
    BudgetStatus.NEAR_LIMIT -> "接近预算"
    BudgetStatus.EXCEEDED -> "已超支"
}

@Composable
private fun BudgetStatus.color(): Color = when (this) {
    BudgetStatus.NORMAL -> MaterialTheme.colorScheme.primary
    BudgetStatus.NEAR_LIMIT -> MaterialTheme.colorScheme.tertiary
    BudgetStatus.EXCEEDED -> MaterialTheme.colorScheme.error
}

private fun TransactionType.label() = when (this) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "转账"
}

private fun RecentTransaction.signedAmount() = when (type) {
    TransactionType.EXPENSE -> "-${amount.formatCny()}"
    TransactionType.INCOME -> amount.formatCny(showPositiveSign = true)
    TransactionType.TRANSFER -> amount.formatCny()
}
