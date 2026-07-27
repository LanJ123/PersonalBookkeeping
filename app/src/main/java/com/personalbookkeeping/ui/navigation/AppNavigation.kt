package com.personalbookkeeping.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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
    val glyph: String,
    val label: String,
    val testTag: String,
)

private val rootTabs = listOf(
    RootTab(HomeKey, "⌂", "首页", "root-tab-home"),
    RootTab(LedgerKey, "≡", "流水", "root-tab-ledger"),
    RootTab(StatisticsKey, "◔", "统计", "root-tab-statistics"),
    RootTab(SettingsKey, "⚙", "设置", "root-tab-settings"),
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
            if (isRoot) NavigationBar {
                rootTabs.forEach { tab ->
                    NavigationBarItem(
                        modifier = Modifier.testTag(tab.testTag),
                        selected = current == tab.key,
                        onClick = { switchRoot(tab.key) },
                        icon = { Text(tab.glyph, Modifier.clearAndSetSemantics { }) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (isRoot) {
                FloatingActionButton(
                    onClick = { openEditor() },
                    modifier = Modifier.testTag("root-add-transaction"),
                ) { Text("＋记一笔") }
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
                            onBack = { backStack.removeLastOrNull() },
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
                            onBack = { backStack.removeLastOrNull() },
                            onAmountsHiddenChanged = settingsViewModel::setHideAmounts,
                            onThemeModeChanged = settingsViewModel::setThemeMode,
                            onAppLockChanged = onAppLockChanged,
                        )
                    }
                    BudgetsKey -> NavEntry(key) {
                        BudgetsScreen(
                            state = currentInsightsState.value,
                            snackbarHostState = budgetSnackbar,
                            onBack = { backStack.removeLastOrNull() },
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
                            onBack = { backStack.removeLastOrNull() },
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
                            onBack = { backStack.removeLastOrNull() },
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
                            onBack = { backStack.removeLastOrNull() },
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
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    else -> error("Unknown navigation key: $key")
                }
            },
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onBudgets: () -> Unit,
    onDataTransfer: () -> Unit,
    onPrivacy: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("设置") })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onAccounts, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("账户管理") }
            Button(onClick = onCategories, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("分类管理") }
            Button(onClick = onBudgets, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("预算管理") }
            Button(onClick = onDataTransfer, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("数据与备份") }
            Button(onClick = onPrivacy, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("隐私与安全") }
            Text("数据仅保存在本机，不会自动上传。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
