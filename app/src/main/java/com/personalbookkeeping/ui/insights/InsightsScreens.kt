package com.personalbookkeeping.ui.insights

import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalbookkeeping.benchmark.BenchmarkUiSignals
import com.personalbookkeeping.common.Money
import com.personalbookkeeping.domain.model.BudgetProgress
import com.personalbookkeeping.domain.model.BudgetStatus
import com.personalbookkeeping.domain.model.CategorySpending
import com.personalbookkeeping.domain.model.MonthPeriod
import com.personalbookkeeping.domain.model.MonthlySummary
import com.personalbookkeeping.domain.model.PeriodExpenseComparison
import com.personalbookkeeping.domain.model.RecentTransaction
import com.personalbookkeeping.domain.model.StatisticsGranularity
import com.personalbookkeeping.domain.model.StatisticsPeriod
import com.personalbookkeeping.domain.model.StatisticsTrendPoint
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.ui.privacy.displayCny
import com.personalbookkeeping.ui.privacy.LocalAmountsHidden
import com.personalbookkeeping.ui.theme.IosSegmentOption
import com.personalbookkeeping.ui.theme.IosSegmentedControl
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onTransactionClick: (String) -> Unit = {},
) {
    if (state.isLoading) {
        CenterLoading(PaddingValues(0.dp))
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home-list")
                .onGloballyPositioned {
                    BenchmarkUiSignals.mark(BenchmarkUiSignals.HOME_READY)
                },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "个人记账",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            item { HomeOverviewCard(state) }
            state.message?.let { message -> item { EmptyCard(message) } }
            item {
                Text(
                    "本月流水（${state.transactions.size}笔）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (state.transactions.isEmpty()) {
                item { EmptyCard("本月还没有流水，点击“记一笔”开始") }
            } else {
                val dailyGroups = state.transactions.toHomeDailyGroups()
                items(dailyGroups, key = { it.epochDay }) { group ->
                    HomeDailyGroupCard(group, onTransactionClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onGranularitySelected: (StatisticsGranularity) -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCategoryClick: (StatisticsPeriod, TransactionType, String) -> Unit,
) {
    if (state.isLoading) {
        CenterLoading(PaddingValues(0.dp))
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("statistics-list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "统计",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                StatisticsGranularitySelector(
                    selected = state.period.granularity,
                    onSelected = onGranularitySelected,
                )
            }
            item {
                StatisticsPeriodSwitcher(
                    period = state.period,
                    onPrevious = onPreviousPeriod,
                    onNext = onNextPeriod,
                )
            }
            item {
                StatisticsTypeSelector(
                    selected = state.type,
                    onSelected = onTypeSelected,
                )
            }
            state.message?.let { message -> item { EmptyCard(message) } }
            item { StatisticsSummaryCard(state) }
            item {
                Text(
                    "${state.period.granularity.periodLabel()}${state.type.compositionLabel()}趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item { StatisticsTrendChart(state.insights.trend, state.type) }
            item {
                CategoryCompositionCard(
                    type = state.type,
                    categories = if (state.type == TransactionType.EXPENSE) {
                        state.insights.categories
                    } else {
                        state.insights.incomeCategories
                    },
                    total = if (state.type == TransactionType.EXPENSE) {
                        state.insights.summary.expense
                    } else {
                        state.insights.summary.income
                    },
                    onCategoryClick = { categoryId ->
                        onCategoryClick(state.period, state.type, categoryId)
                    },
                )
            }
            item {
                Text(
                    "${state.period.granularity.comparisonLabel()}${state.type.compositionLabel()}对比",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item { PeriodComparisonChart(state) }
        }
    }
}

@Composable
private fun HomeOverviewCard(state: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("今日支出", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    state.todayExpense.displayCny(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                SummaryValue("本月支出", state.summary.expense, modifier = Modifier.weight(1f))
                SummaryValue("本月收入", state.summary.income, modifier = Modifier.weight(1f))
            }
        }
    }
}

private data class HomeDailyGroup(
    val epochDay: Long,
    val transactions: List<RecentTransaction>,
    val expense: Money,
    val income: Money,
)

private fun List<RecentTransaction>.toHomeDailyGroups(): List<HomeDailyGroup> =
    groupBy { it.localDateEpochDay }
        .toSortedMap(compareByDescending { it })
        .map { (epochDay, transactions) ->
            HomeDailyGroup(
                epochDay = epochDay,
                transactions = transactions,
                expense = Money.fromMinor(
                    transactions.filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amount.minorUnits },
                ),
                income = Money.fromMinor(
                    transactions.filter { it.type == TransactionType.INCOME }
                        .sumOf { it.amount.minorUnits },
                ),
            )
        }

@Composable
private fun HomeDailyGroupCard(
    group: HomeDailyGroup,
    onTransactionClick: (String) -> Unit,
) {
    val date = LocalDate.ofEpochDay(group.epochDay)
    Card(Modifier.fillMaxWidth().testTag("home-day-${group.epochDay}")) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    date.format(HOME_DAY_FORMAT),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "支 ${group.expense.displayCny()}  收 ${group.income.displayCny()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            group.transactions.forEachIndexed { index, transaction ->
                HomeTransactionRow(transaction, onTransactionClick)
                if (index < group.transactions.lastIndex) {
                    HorizontalDivider(Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeTransactionRow(
    transaction: RecentTransaction,
    onClick: (String) -> Unit,
) {
    val localDateTime = transaction.occurredAt.atZone(transaction.zoneId)
    val flow = when (transaction.type) {
        TransactionType.TRANSFER ->
            "${transaction.accountName} → ${transaction.targetAccountName.orEmpty()}"
        else -> transaction.accountName
    }
    val amount = when (transaction.type) {
        TransactionType.EXPENSE -> "-${transaction.amount.displayCny()}"
        TransactionType.INCOME -> transaction.amount.displayCny(showPositiveSign = true)
        TransactionType.TRANSFER -> transaction.amount.displayCny()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick(transaction.id) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                transaction.categoryName ?: transaction.type.compositionLabel(),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${localDateTime.format(HOME_TRANSACTION_TIME_FORMAT)} · $flow",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(amount, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CategoryCompositionCard(
    type: TransactionType,
    categories: List<CategorySpending>,
    total: Money,
    onCategoryClick: (String) -> Unit,
) {
    val palette = CATEGORY_CHART_COLORS
    val amountsHidden = LocalAmountsHidden.current
    val description = categories.joinToString("；") { category ->
        val share = category.shareOf(total)
        if (amountsHidden) {
            "${category.categoryName}${category.transactionCount}笔，占比${share.percentLabel()}"
        } else {
            "${category.categoryName}${category.transactionCount}笔，${category.amount.formatCny()}，占比${share.percentLabel()}"
        }
    }
    Card(Modifier.fillMaxWidth().testTag("category-composition-card")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${type.compositionLabel()}分类构成",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (categories.isEmpty() || total.minorUnits <= 0L) {
                Text(
                    "当前周期暂无${type.compositionLabel()}，分类构成将在记账后显示",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeWidth = 34.dp.toPx()
                        val radius = minOf(size.width, size.height) * 0.23f
                        val diameter = radius * 2f
                        val arcTopLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f,
                        )
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val labels = mutableListOf<PieChartLabel>()
                        var startAngle = -90f
                        categories.forEachIndexed { index, category ->
                            val sweep = category.shareOf(total) * 360f
                            drawArc(
                                color = palette[index % palette.size],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth),
                            )
                            val middleRadians = (startAngle + sweep / 2f) * PI.toFloat() / 180f
                            val directionX = cos(middleRadians)
                            val directionY = sin(middleRadians)
                            labels += PieChartLabel(
                                text = "${category.categoryName} ${category.shareOf(total).percentLabel()}",
                                color = palette[index % palette.size],
                                start = Offset(
                                    center.x + directionX * (radius + strokeWidth / 2f),
                                    center.y + directionY * (radius + strokeWidth / 2f),
                                ),
                                elbow = Offset(
                                    center.x + directionX * (radius + strokeWidth / 2f + 16.dp.toPx()),
                                    center.y + directionY * (radius + strokeWidth / 2f + 16.dp.toPx()),
                                ),
                                rightSide = directionX >= 0f,
                            )
                            startAngle += sweep
                        }
                        val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                            color = labelColor.toArgb()
                            textSize = 11.sp.toPx()
                        }
                        labels.groupBy { it.rightSide }.forEach { (rightSide, sideLabels) ->
                            var previousY = 8.dp.toPx()
                            sideLabels.sortedBy { it.elbow.y }.forEach { label ->
                                val y = label.elbow.y
                                    .coerceAtLeast(previousY + 16.dp.toPx())
                                    .coerceAtMost(size.height - 8.dp.toPx())
                                previousY = y
                                val lineEndX = if (rightSide) size.width - 8.dp.toPx() else 8.dp.toPx()
                                drawLine(label.color, label.start, label.elbow, strokeWidth = 2.dp.toPx())
                                drawLine(
                                    label.color,
                                    label.elbow,
                                    Offset(lineEndX, y),
                                    strokeWidth = 2.dp.toPx(),
                                )
                                textPaint.textAlign = if (rightSide) {
                                    AndroidPaint.Align.RIGHT
                                } else {
                                    AndroidPaint.Align.LEFT
                                }
                                drawContext.canvas.nativeCanvas.drawText(
                                    label.text,
                                    lineEndX,
                                    y - 3.dp.toPx(),
                                    textPaint,
                                )
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(total.displayCny(), fontWeight = FontWeight.Bold)
                        Text(
                            "共${type.compositionLabel()}（元）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                categories.forEachIndexed { index, category ->
                    val share = category.shareOf(total)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryClick(category.categoryId) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${index + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("●", color = palette[index % palette.size])
                        Text(category.categoryName, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${category.transactionCount}笔",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            share.percentLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            category.amount.displayCny(),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                        )
                        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsGranularitySelector(
    selected: StatisticsGranularity,
    onSelected: (StatisticsGranularity) -> Unit,
) {
    val entries = StatisticsGranularity.entries
    IosSegmentedControl(
        options = entries.map { IosSegmentOption(it.tabLabel()) },
        selectedIndex = entries.indexOf(selected),
        onSelected = { onSelected(entries[it]) },
    )
}

@Composable
private fun StatisticsTypeSelector(
    selected: TransactionType,
    onSelected: (TransactionType) -> Unit,
) {
    val entries = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
    IosSegmentedControl(
        options = entries.map { type ->
            IosSegmentOption(
                label = type.compositionLabel(),
                testTag = "statistics-type-${type.name.lowercase()}",
            )
        },
        selectedIndex = entries.indexOf(selected),
        onSelected = { onSelected(entries[it]) },
    )
}

@Composable
private fun StatisticsPeriodSwitcher(
    period: StatisticsPeriod,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onPrevious,
            modifier = Modifier.testTag("statistics-previous-period"),
        ) { Text("上一${period.granularity.unitLabel()}") }
        Text(
            period.label,
            modifier = Modifier
                .testTag("statistics-period")
                .onGloballyPositioned {
                    BenchmarkUiSignals.mark(BenchmarkUiSignals.MONTH_READY)
                },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedButton(
            onClick = onNext,
            modifier = Modifier.testTag("statistics-next-period"),
        ) { Text("下一${period.granularity.unitLabel()}") }
    }
}

@Composable
private fun StatisticsSummaryCard(state: StatisticsUiState) {
    val summary = state.insights.summary
    val selectedTotal = summary.amountFor(state.type)
    val average = if (state.type == TransactionType.EXPENSE) {
        state.insights.averageDailyExpense
    } else {
        state.insights.averageDailyIncome
    }
    val previousTotal = state.insights.comparisons
        .dropLast(1)
        .lastOrNull()
        ?.amountFor(state.type)
        ?: Money.fromMinor(0)
    val previousDifference = Money.fromMinor(selectedTotal.minorUnits - previousTotal.minorUnits)
    val typeLabel = state.type.compositionLabel()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SummaryValue(
                    "${state.period.granularity.periodLabel()}$typeLabel",
                    selectedTotal,
                    modifier = Modifier.weight(1f),
                )
                SummaryValue(
                    "日均$typeLabel",
                    average,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SummaryValue(
                    "比上${state.period.granularity.periodLabel()}$typeLabel",
                    previousDifference,
                    modifier = Modifier.weight(1f),
                )
                SummaryValue(
                    "收支结余",
                    summary.balance,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatisticsTrendChart(
    trend: List<StatisticsTrendPoint>,
    type: TransactionType,
) {
    if (trend.isEmpty()) {
        EmptyCard("当前周期暂无${type.compositionLabel()}趋势")
        return
    }
    val lineColor = if (type == TransactionType.EXPENSE) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val values = trend.map { it.amountFor(type).minorUnits.coerceAtLeast(0L) }
    val scale = dynamicChartScale(values)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val amountsHidden = LocalAmountsHidden.current
    var selectedIndex by remember(trend, type) { mutableIntStateOf(trend.lastIndex) }
    val description = trend.joinToString("；") {
        if (amountsHidden) "${it.label}金额已隐藏" else
            "${it.label}${type.compositionLabel()}${it.amountFor(type).formatCny()}"
    }
    val axisLabelIndices = trend.chartAxisLabelIndices()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = description },
            ) {
                Column(
                    Modifier
                        .width(58.dp)
                        .height(220.dp)
                        .padding(top = 58.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    (scale.tickCount downTo 1).forEach { tick ->
                        Text(
                            if (amountsHidden) "•••" else formatAxisAmount(scale.valueAt(tick)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("", style = MaterialTheme.typography.labelSmall)
                }
                Canvas(
                    Modifier
                        .weight(1f)
                        .height(220.dp)
                        .padding(start = 8.dp)
                        .pointerInput(trend) {
                            detectTapGestures { tap ->
                                if (trend.size > 1) {
                                    val horizontalPadding = 4.dp.toPx()
                                    val chartWidth = (size.width - horizontalPadding * 2f)
                                        .coerceAtLeast(1f)
                                    selectedIndex = (
                                        (tap.x - horizontalPadding) /
                                            chartWidth * (trend.size - 1)
                                        ).roundToInt().coerceIn(trend.indices)
                                }
                            }
                        },
                ) {
                    val plotTop = 58.dp.toPx()
                    val plotBottom = size.height - 4.dp.toPx()
                    val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)
                    val horizontalPadding = 4.dp.toPx()
                    val plotWidth = (size.width - horizontalPadding * 2f).coerceAtLeast(1f)
                    val step = if (trend.size <= 1) 0f else plotWidth / (trend.size - 1)
                    fun x(index: Int) =
                        if (trend.size <= 1) size.width / 2f else horizontalPadding + step * index
                    fun y(value: Long) =
                        plotBottom - plotHeight * value / scale.axisMax.toFloat()

                    axisLabelIndices.forEach { index ->
                        val x = x(index)
                        drawLine(
                            gridColor,
                            Offset(x, plotTop),
                            Offset(x, plotBottom),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    val areaPath = Path().apply {
                        moveTo(x(0), plotBottom)
                        lineTo(x(0), y(values.first()))
                        values.drop(1).forEachIndexed { index, value ->
                            lineTo(x(index + 1), y(value))
                        }
                        lineTo(x(values.lastIndex), plotBottom)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.30f), Color.Transparent),
                            startY = plotTop,
                            endY = plotBottom,
                        ),
                    )
                    values.zipWithNext().forEachIndexed { index, (first, second) ->
                        drawLine(
                            lineColor,
                            Offset(x(index), y(first)),
                            Offset(x(index + 1), y(second)),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }

                    val selected = selectedIndex.coerceIn(trend.indices)
                    val point = Offset(x(selected), y(values[selected]))
                    drawLine(
                        color = lineColor.copy(alpha = 0.75f),
                        start = Offset(point.x, plotTop),
                        end = Offset(point.x, plotBottom),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                        ),
                    )
                    drawCircle(lineColor.copy(alpha = 0.18f), 9.dp.toPx(), point)
                    drawCircle(lineColor, 4.dp.toPx(), point)

                    val bubbleWidth = 92.dp.toPx()
                    val bubbleHeight = 48.dp.toPx()
                    val bubbleLeft = (point.x - bubbleWidth / 2f)
                        .coerceIn(0f, size.width - bubbleWidth)
                    val bubbleTop = 2.dp.toPx()
                    drawRoundRect(
                        color = lineColor,
                        topLeft = Offset(bubbleLeft, bubbleTop),
                        size = Size(bubbleWidth, bubbleHeight),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                    )
                    val pointer = Path().apply {
                        moveTo(point.x - 6.dp.toPx(), bubbleTop + bubbleHeight)
                        lineTo(point.x + 6.dp.toPx(), bubbleTop + bubbleHeight)
                        lineTo(point.x, bubbleTop + bubbleHeight + 7.dp.toPx())
                        close()
                    }
                    drawPath(pointer, lineColor)
                    val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textAlign = AndroidPaint.Align.CENTER
                        textSize = 12.sp.toPx()
                    }
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "${trend[selected].label}${type.compositionLabel()}",
                            bubbleLeft + bubbleWidth / 2f,
                            bubbleTop + 18.dp.toPx(),
                            textPaint,
                        )
                        drawText(
                            if (amountsHidden) "•••" else formatChartAmount(values[selected]),
                            bubbleLeft + bubbleWidth / 2f,
                            bubbleTop + 37.dp.toPx(),
                            textPaint,
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 66.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                axisLabelIndices.forEach { index ->
                    Text(trend[index].label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun PeriodComparisonChart(state: StatisticsUiState) {
    val comparisons = state.insights.comparisons
    if (comparisons.isEmpty()) {
        EmptyCard("暂无可对比周期")
        return
    }
    val color = if (state.type == TransactionType.EXPENSE) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val values = comparisons.map {
        it.amountFor(state.type).minorUnits.coerceAtLeast(0L)
    }
    val scale = dynamicChartScale(values, preferredTickCount = 5)
    val amountsHidden = LocalAmountsHidden.current
    val description = comparisons.joinToString("；") {
        if (amountsHidden) "${it.label}${state.type.compositionLabel()}金额已隐藏" else
            "${it.label}${state.type.compositionLabel()}${it.amountFor(state.type).formatCny()}"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .semantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                comparisons.forEachIndexed { index, comparison ->
                    val value = values[index]
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            if (amountsHidden) "•••" else formatChartAmount(value),
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            maxLines = 1,
                        )
                        Box(
                            Modifier
                                .height(138.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(
                                        if (value == 0L) 0.01f else
                                            (value.toFloat() / scale.axisMax.toFloat())
                                                .coerceIn(0.03f, 1f),
                                    )
                                    .background(color),
                            )
                        }
                        Text(
                            comparison.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(
                "最近 ${comparisons.size} 个周期",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun StatisticsGranularity.tabLabel(): String = when (this) {
    StatisticsGranularity.WEEK -> "周"
    StatisticsGranularity.MONTH -> "月"
    StatisticsGranularity.YEAR -> "年"
}

private fun StatisticsGranularity.unitLabel(): String = tabLabel()

private fun StatisticsGranularity.periodLabel(): String = tabLabel()

private fun StatisticsGranularity.comparisonLabel(): String = when (this) {
    StatisticsGranularity.WEEK -> "周"
    StatisticsGranularity.MONTH -> "月"
    StatisticsGranularity.YEAR -> "年"
}

private fun TransactionType.compositionLabel(): String = when (this) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "转账"
}

private fun MonthlySummary.amountFor(type: TransactionType): Money = when (type) {
    TransactionType.EXPENSE -> expense
    TransactionType.INCOME -> income
    TransactionType.TRANSFER -> Money.fromMinor(0)
}

private fun StatisticsTrendPoint.amountFor(type: TransactionType): Money = when (type) {
    TransactionType.EXPENSE -> expense
    TransactionType.INCOME -> income
    TransactionType.TRANSFER -> Money.fromMinor(0)
}

private fun PeriodExpenseComparison.amountFor(type: TransactionType): Money = when (type) {
    TransactionType.EXPENSE -> expense
    TransactionType.INCOME -> income
    TransactionType.TRANSFER -> Money.fromMinor(0)
}

private fun formatChartAmount(minorUnits: Long): String =
    if (minorUnits >= 1_000_000L) {
        String.format(Locale.ROOT, "¥%.2f万", minorUnits / 1_000_000.0)
    } else {
        Money.fromMinor(minorUnits).formatCny()
    }

private fun formatAxisAmount(minorUnits: Long): String = when {
    minorUnits >= 1_000_000L ->
        String.format(Locale.ROOT, "¥%.1f万", minorUnits / 1_000_000.0)
    minorUnits % 100L == 0L -> "¥${minorUnits / 100L}"
    else -> String.format(Locale.ROOT, "¥%.2f", minorUnits / 100.0)
}

private fun List<StatisticsTrendPoint>.chartAxisLabelIndices(): List<Int> {
    if (isEmpty()) return emptyList()
    if (size <= 8) return indices.toList()
    val stride = ((lastIndex.toFloat() / 7f).roundToInt()).coerceAtLeast(1)
    return (0..lastIndex step stride).take(8)
}

private fun CategorySpending.shareOf(total: Money): Float =
    if (total.minorUnits <= 0L) 0f else
        (amount.minorUnits.toFloat() / total.minorUnits.toFloat()).coerceIn(0f, 1f)

private fun Float.percentLabel(): String = "${(this * 100).roundToInt()}%"

private val HOME_TRANSACTION_TIME_FORMAT =
    DateTimeFormatter.ofPattern("HH:mm", Locale.SIMPLIFIED_CHINESE)

private val HOME_DAY_FORMAT =
    DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)

private data class PieChartLabel(
    val text: String,
    val color: Color,
    val start: Offset,
    val elbow: Offset,
    val rightSide: Boolean,
)

private val CATEGORY_CHART_COLORS = listOf(
    Color(0xFF4F7DF3),
    Color(0xFF59A9F8),
    Color(0xFF12BDE2),
    Color(0xFF12C9BC),
    Color(0xFFFFC928),
    Color(0xFFFF8A3D),
    Color(0xFF8D79F6),
    Color(0xFF7A90A8),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    state: InsightsUiState,
    snackbarHostState: SnackbarHostState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSave: (String?, String) -> Boolean,
    onClear: (String?) -> Unit,
    onMessageConsumed: () -> Unit,
) {
    var editing by remember { mutableStateOf<BudgetEditTarget?>(null) }
    MessageEffect(state, snackbarHostState, onMessageConsumed)
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = BudgetEditTarget(
                        categoryId = null,
                        name = "本月总预算",
                        current = state.insights.totalBudget?.limit,
                    )
                },
                shape = CircleShape,
                modifier = Modifier.testTag("budget-add-fab"),
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) CenterLoading(padding) else LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "预算管理",
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
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
        OutlinedButton(
            onClick = onPrevious,
            modifier = Modifier.testTag("home-previous-month"),
        ) { Text("上月") }
        Text(
            period.label,
            modifier = Modifier
                .testTag("home-period")
                .onGloballyPositioned {
                    BenchmarkUiSignals.mark(BenchmarkUiSignals.MONTH_READY)
                },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedButton(
            onClick = onNext,
            modifier = Modifier.testTag("home-next-month"),
        ) { Text("下月") }
    }
}

@Composable
private fun SummaryValue(
    label: String,
    money: Money?,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text ?: money!!.displayCny(), fontWeight = FontWeight.SemiBold)
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
            Text("已用 ${budget.used.displayCny()} / ${budget.limit.displayCny()}")
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
