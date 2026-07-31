package com.personalbookkeeping.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.paging.compose.collectAsLazyPagingItems
import com.personalbookkeeping.app.AppContainer
import com.personalbookkeeping.domain.model.ThemeMode
import com.personalbookkeeping.ui.ledger.LedgerScreen
import com.personalbookkeeping.ui.ledger.LedgerViewModel
import com.personalbookkeeping.ui.ledger.LedgerViewModelFactory
import com.personalbookkeeping.ui.ledger.TransactionDetailScreen
import com.personalbookkeeping.ui.ledger.TransactionDetailViewModel
import com.personalbookkeeping.ui.ledger.TransactionDetailViewModelFactory
import com.personalbookkeeping.ui.insights.BudgetsScreen
import com.personalbookkeeping.ui.insights.HomeScreen
import com.personalbookkeeping.ui.insights.InsightsViewModel
import com.personalbookkeeping.ui.insights.InsightsViewModelFactory
import com.personalbookkeeping.ui.insights.StatisticsScreen
import com.personalbookkeeping.ui.management.AccountsScreen
import com.personalbookkeeping.ui.management.CategoriesScreen
import com.personalbookkeeping.ui.management.ManagementViewModel
import com.personalbookkeeping.ui.management.ManagementViewModelFactory
import com.personalbookkeeping.ui.settings.DataTransferScreen
import com.personalbookkeeping.ui.settings.PrivacySettingsScreen
import com.personalbookkeeping.ui.settings.SettingsViewModel
import com.personalbookkeeping.ui.settings.SettingsViewModelFactory
import com.personalbookkeeping.ui.transaction.TransactionEditorScreen
import com.personalbookkeeping.ui.transaction.TransactionEditorViewModel
import com.personalbookkeeping.ui.transaction.TransactionEditorViewModelFactory
import com.personalbookkeeping.ui.theme.IosSettingsGroup
import com.personalbookkeeping.ui.theme.IosSettingsRow
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppNavKey : NavKey

@Serializable data object HomeKey : AppNavKey
@Serializable data object LedgerKey : AppNavKey
@Serializable data object StatisticsKey : AppNavKey
@Serializable data object SettingsKey : AppNavKey
@Serializable data class TransactionEditorKey(val transactionId: String?, val instanceId: String) : AppNavKey
@Serializable data class TransactionDetailKey(val transactionId: String) : AppNavKey
@Serializable data object AccountsKey : AppNavKey
@Serializable data object CategoriesKey : AppNavKey
@Serializable data object BudgetsKey : AppNavKey
@Serializable data object DataTransferKey : AppNavKey
@Serializable data object PrivacyKey : AppNavKey

private data class RootTab(
    val key: AppNavKey,
    val icon: RootTabIcon,
    val label: String,
    val testTag: String,
)

private enum class RootTabIcon { HOME, LEDGER, STATISTICS, SETTINGS }

private val rootTabs = listOf(
    RootTab(HomeKey, RootTabIcon.HOME, "首页", "root-tab-home"),
    RootTab(LedgerKey, RootTabIcon.LEDGER, "流水", "root-tab-ledger"),
    RootTab(StatisticsKey, RootTabIcon.STATISTICS, "统计", "root-tab-statistics"),
    RootTab(SettingsKey, RootTabIcon.SETTINGS, "设置", "root-tab-settings"),
)

@Composable
fun BookkeepingApp(
    container: AppContainer,
    appLockEnabled: Boolean,
    appLockMessage: String?,
    amountsHidden: Boolean,
    themeMode: ThemeMode,
    onAppLockChanged: (Boolean) -> Unit,
) {
    // NavDisplay keeps NavEntry content alive while it remains on the back stack.
    // Keep mutable settings behind State objects so cached entries never render
    // the values that were captured when the destination was first created.
    val currentAppLockEnabled = rememberUpdatedState(appLockEnabled)
    val currentAppLockMessage = rememberUpdatedState(appLockMessage)
    val currentAmountsHidden = rememberUpdatedState(amountsHidden)
    val currentThemeMode = rememberUpdatedState(themeMode)
    val backStack = rememberNavBackStack(HomeKey)
    val ledgerViewModel: LedgerViewModel = viewModel(
        factory = LedgerViewModelFactory(container.ledgerRepository, container.transactionRepository),
    )
    val managementViewModel: ManagementViewModel = viewModel(
        factory = ManagementViewModelFactory(container.managementRepository, container.transactionRepository),
    )
    val insightsViewModel: InsightsViewModel = viewModel(
        factory = InsightsViewModelFactory(container.insightsRepository),
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(container.portabilityService),
    )
    val ledgerState by ledgerViewModel.state.collectAsStateWithLifecycle()
    val managementState by managementViewModel.state.collectAsStateWithLifecycle()
    val insightsState by insightsViewModel.state.collectAsStateWithLifecycle()
    val homeState by insightsViewModel.homeState.collectAsStateWithLifecycle()
    val statisticsState by insightsViewModel.statisticsState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
    val currentLedgerState = rememberUpdatedState(ledgerState)
    val currentManagementState = rememberUpdatedState(managementState)
    val currentInsightsState = rememberUpdatedState(insightsState)
    val currentHomeState = rememberUpdatedState(homeState)
    val currentStatisticsState = rememberUpdatedState(statisticsState)
    val currentSettingsState = rememberUpdatedState(settingsState)
    val current = backStack.lastOrNull()
    val isRoot = rootTabs.any { it.key == current }
    val ledgerSnackbar = remember { SnackbarHostState() }
    val accountSnackbar = remember { SnackbarHostState() }
    val categorySnackbar = remember { SnackbarHostState() }
    val budgetSnackbar = remember { SnackbarHostState() }

    fun openEditor(id: String? = null) {
        backStack.add(TransactionEditorKey(id, UUID.randomUUID().toString()))
    }

    fun switchRoot(key: AppNavKey) {
        backStack.clear()
        backStack.add(key)
    }

    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        bottomBar = {
            if (isRoot) {
                IosBottomBar(
                    current = current,
                    onTabSelected = ::switchRoot,
                    onAdd = { openEditor() },
                )
            }
        },
    ) { outerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(outerPadding),
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    HomeKey -> NavEntry(key) {
                        HomeScreen(
                            state = currentHomeState.value,
                            onTransactionClick = { backStack.add(TransactionDetailKey(it)) },
                        )
                    }
                    LedgerKey -> NavEntry(key) {
                        val transactions = ledgerViewModel.transactions.collectAsLazyPagingItems()
                        LedgerScreen(
                            state = currentLedgerState.value,
                            transactions = transactions,
                            snackbarHostState = ledgerSnackbar,
                            onQueryChanged = ledgerViewModel::setNoteQuery,
                            onTypeChanged = ledgerViewModel::setType,
                            onAccountChanged = ledgerViewModel::setAccount,
                            onCategoryChanged = ledgerViewModel::setCategory,
                            onDateChanged = ledgerViewModel::setDateRange,
                            onMonthSelected = ledgerViewModel::selectMonth,
                            onPreviousMonth = ledgerViewModel::previousMonth,
                            onNextMonth = ledgerViewModel::nextMonth,
                            onClearMonth = ledgerViewModel::clearMonth,
                            onClearFilters = ledgerViewModel::clearFilters,
                            onTransactionClick = { backStack.add(TransactionDetailKey(it)) },
                            onRestore = ledgerViewModel::restoreLastDeleted,
                            onDeletedConsumed = ledgerViewModel::consumeDeleted,
                            onMessageConsumed = ledgerViewModel::consumeMessage,
                        )
                    }
                    StatisticsKey -> NavEntry(key) {
                        StatisticsScreen(
                            state = currentStatisticsState.value,
                            onGranularitySelected = insightsViewModel::selectStatisticsGranularity,
                            onTypeSelected = insightsViewModel::selectStatisticsType,
                            onPreviousPeriod = insightsViewModel::previousStatisticsPeriod,
                            onNextPeriod = insightsViewModel::nextStatisticsPeriod,
                            onCategoryClick = { period, type, categoryId ->
                                ledgerViewModel.showStatistics(period, type, categoryId)
                                switchRoot(LedgerKey)
                            },
                        )
                    }
                    SettingsKey -> NavEntry(key) {
                        SettingsScreen(
                            onAccounts = { backStack.add(AccountsKey) },
                            onCategories = { backStack.add(CategoriesKey) },
                            onBudgets = { backStack.add(BudgetsKey) },
                            onDataTransfer = { backStack.add(DataTransferKey) },
                            onPrivacy = { backStack.add(PrivacyKey) },
                        )
                    }
                    DataTransferKey -> NavEntry(key) {
                        DataTransferScreen(
                            state = currentSettingsState.value,
                            onBackup = settingsViewModel::createBackup,
                            onRestoreSelected = settingsViewModel::inspectBackup,
                            onConfirmRestore = settingsViewModel::confirmRestore,
                            onCancelRestore = settingsViewModel::cancelRestoreReview,
                            onExportCsv = settingsViewModel::exportCsv,
                            onInputError = settingsViewModel::showMessage,
                        )
                    }
                    PrivacyKey -> NavEntry(key) {
                        PrivacySettingsScreen(
                            amountsHidden = currentAmountsHidden.value,
                            themeMode = currentThemeMode.value,
                            appLockEnabled = currentAppLockEnabled.value,
                            appLockMessage = currentAppLockMessage.value,
                            onAmountsHiddenChanged = settingsViewModel::setHideAmounts,
                            onThemeModeChanged = settingsViewModel::setThemeMode,
                            onAppLockChanged = onAppLockChanged,
                        )
                    }
                    BudgetsKey -> NavEntry(key) {
                        BudgetsScreen(
                            state = currentInsightsState.value,
                            snackbarHostState = budgetSnackbar,
                            onPreviousMonth = insightsViewModel::previousMonth,
                            onNextMonth = insightsViewModel::nextMonth,
                            onSave = insightsViewModel::saveBudget,
                            onClear = insightsViewModel::clearBudget,
                            onMessageConsumed = insightsViewModel::consumeMessage,
                        )
                    }
                    AccountsKey -> NavEntry(key) {
                        AccountsScreen(
                            state = currentManagementState.value,
                            snackbarHostState = accountSnackbar,
                            onSave = managementViewModel::saveAccount,
                            onDeactivate = managementViewModel::deactivateAccount,
                            onMove = managementViewModel::moveAccount,
                            onMessageConsumed = managementViewModel::consumeMessage,
                        )
                    }
                    CategoriesKey -> NavEntry(key) {
                        CategoriesScreen(
                            state = currentManagementState.value,
                            snackbarHostState = categorySnackbar,
                            onSave = managementViewModel::saveCategory,
                            onDeactivate = managementViewModel::deactivateCategory,
                            onMove = managementViewModel::moveCategory,
                            onMessageConsumed = managementViewModel::consumeMessage,
                        )
                    }
                    is TransactionDetailKey -> NavEntry(key) {
                        val detailViewModel: TransactionDetailViewModel = viewModel(
                            key = "detail-${key.transactionId}",
                            factory = TransactionDetailViewModelFactory(container.ledgerRepository, key.transactionId),
                        )
                        val detailState by detailViewModel.state.collectAsStateWithLifecycle()
                        TransactionDetailScreen(
                            state = detailState,
                            onEdit = { openEditor(it) },
                            onDelete = {
                                ledgerViewModel.delete(it)
                                switchRoot(LedgerKey)
                            },
                        )
                    }
                    is TransactionEditorKey -> NavEntry(key) {
                        val editorViewModel: TransactionEditorViewModel = viewModel(
                            key = "editor-${key.instanceId}",
                            factory = TransactionEditorViewModelFactory(
                                createTransaction = container.createTransactionUseCase,
                                updateTransaction = container.updateTransactionUseCase,
                                repository = container.transactionRepository,
                                ledgerRepository = container.ledgerRepository,
                                transactionId = key.transactionId,
                            ),
                        )
                        val editorState by editorViewModel.state.collectAsStateWithLifecycle()
                        TransactionEditorScreen(
                            state = editorState,
                            onAmountChanged = editorViewModel::onAmountChanged,
                            onNoteChanged = editorViewModel::onNoteChanged,
                            onTypeSelected = editorViewModel::onTypeSelected,
                            onAccountSelected = editorViewModel::onAccountSelected,
                            onTargetAccountSelected = editorViewModel::onTargetAccountSelected,
                            onCategorySelected = editorViewModel::onCategorySelected,
                            onDateSelected = editorViewModel::onDateSelected,
                            onTimeSelected = editorViewModel::onTimeSelected,
                            onSave = editorViewModel::save,
                            onSaved = { backStack.removeLastOrNull() },
                        )
                    }
                    else -> error("Unknown navigation key: $key")
                }
            },
        )
    }
}

@Composable
private fun IosBottomBar(
    current: NavKey?,
    onTabSelected: (AppNavKey) -> Unit,
    onAdd: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                IosTabItem(rootTabs[0], current == rootTabs[0].key, onTabSelected)
                IosTabItem(rootTabs[1], current == rootTabs[1].key, onTabSelected)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("root-add-transaction")
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .semantics {
                                text = AnnotatedString("＋记一笔")
                                contentDescription = "新增流水"
                            }
                            .clickable(onClick = onAdd),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(25.dp)) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawLine(
                                Color.White,
                                Offset(center.x, 2f),
                                Offset(center.x, size.height - 2f),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                Color.White,
                                Offset(2f, center.y),
                                Offset(size.width - 2f, center.y),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
                IosTabItem(rootTabs[2], current == rootTabs[2].key, onTabSelected)
                IosTabItem(rootTabs[3], current == rootTabs[3].key, onTabSelected)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.IosTabItem(
    tab: RootTab,
    selected: Boolean,
    onSelected: (AppNavKey) -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .testTag(tab.testTag)
            .clickable { onSelected(tab.key) }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IosTabIcon(tab.icon, color)
        Text(
            tab.label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun IosTabIcon(icon: RootTabIcon, color: Color) {
    Canvas(Modifier.size(25.dp)) {
        val stroke = 2.dp.toPx()
        when (icon) {
            RootTabIcon.HOME -> {
                val roof = Path().apply {
                    moveTo(size.width * .12f, size.height * .48f)
                    lineTo(size.width * .5f, size.height * .15f)
                    lineTo(size.width * .88f, size.height * .48f)
                }
                drawPath(roof, color, style = Stroke(stroke, cap = StrokeCap.Round))
                drawRoundRect(
                    color,
                    topLeft = Offset(size.width * .22f, size.height * .43f),
                    size = androidx.compose.ui.geometry.Size(size.width * .56f, size.height * .43f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    style = Stroke(stroke),
                )
            }
            RootTabIcon.LEDGER -> {
                listOf(.28f, .5f, .72f).forEach { y ->
                    drawCircle(color, radius = stroke * .65f, center = Offset(size.width * .17f, size.height * y))
                    drawLine(
                        color,
                        Offset(size.width * .31f, size.height * y),
                        Offset(size.width * .84f, size.height * y),
                        stroke,
                        StrokeCap.Round,
                    )
                }
            }
            RootTabIcon.STATISTICS -> {
                val barWidth = size.width * .16f
                listOf(.62f, .38f, .16f).forEachIndexed { index, top ->
                    drawRoundRect(
                        color,
                        topLeft = Offset(size.width * (.18f + index * .25f), size.height * top),
                        size = androidx.compose.ui.geometry.Size(barWidth, size.height * (.84f - top)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
                    )
                }
            }
            RootTabIcon.SETTINGS -> {
                drawCircle(color, radius = size.minDimension * .33f, style = Stroke(stroke))
                drawCircle(color, radius = size.minDimension * .10f, style = Stroke(stroke))
                repeat(8) { index ->
                    val angle = Math.toRadians((index * 45).toDouble())
                    val start = size.minDimension * .36f
                    val end = size.minDimension * .47f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawLine(
                        color,
                        Offset(center.x + kotlin.math.cos(angle).toFloat() * start, center.y + kotlin.math.sin(angle).toFloat() * start),
                        Offset(center.x + kotlin.math.cos(angle).toFloat() * end, center.y + kotlin.math.sin(angle).toFloat() * end),
                        stroke,
                        StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onBudgets: () -> Unit,
    onDataTransfer: () -> Unit,
    onPrivacy: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "设置",
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text("财务", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IosSettingsGroup {
            IosSettingsRow("账户管理", "管理现金、银行卡和其他资金账户", "¥", Color(0xFF007AFF), onAccounts)
            IosSettingsRow("分类管理", "整理支出与收入分类", "#", Color(0xFFFF9500), onCategories)
            IosSettingsRow("预算管理", "设置月度分类预算", "◎", Color(0xFF34C759), onBudgets, showDivider = false)
        }
        Text("数据与安全", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IosSettingsGroup {
            IosSettingsRow("数据与备份", "导入、导出和恢复本地数据", "⇅", Color(0xFF5856D6), onDataTransfer)
            IosSettingsRow("隐私与安全", "金额隐藏、主题与应用锁", "✓", Color(0xFF8E8E93), onPrivacy, showDivider = false)
        }
        Text(
            "数据仅保存在本机，不会自动上传。",
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
