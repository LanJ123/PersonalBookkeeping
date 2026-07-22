package com.personalbookkeeping.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.paging.compose.collectAsLazyPagingItems
import com.personalbookkeeping.app.AppContainer
import com.personalbookkeeping.ui.ledger.LedgerScreen
import com.personalbookkeeping.ui.ledger.LedgerViewModel
import com.personalbookkeeping.ui.ledger.LedgerViewModelFactory
import com.personalbookkeeping.ui.ledger.TransactionDetailScreen
import com.personalbookkeeping.ui.ledger.TransactionDetailViewModel
import com.personalbookkeeping.ui.ledger.TransactionDetailViewModelFactory
import com.personalbookkeeping.ui.management.AccountsScreen
import com.personalbookkeeping.ui.management.CategoriesScreen
import com.personalbookkeeping.ui.management.ManagementViewModel
import com.personalbookkeeping.ui.management.ManagementViewModelFactory
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

private data class RootTab(val key: AppNavKey, val glyph: String, val label: String)

private val rootTabs = listOf(
    RootTab(HomeKey, "⌂", "首页"),
    RootTab(LedgerKey, "≡", "流水"),
    RootTab(StatisticsKey, "◔", "统计"),
    RootTab(SettingsKey, "⚙", "设置"),
)

@Composable
fun BookkeepingApp(container: AppContainer) {
    val backStack = rememberNavBackStack(LedgerKey)
    val ledgerViewModel: LedgerViewModel = viewModel(
        factory = LedgerViewModelFactory(container.ledgerRepository, container.transactionRepository),
    )
    val managementViewModel: ManagementViewModel = viewModel(
        factory = ManagementViewModelFactory(container.managementRepository, container.transactionRepository),
    )
    val ledgerState by ledgerViewModel.state.collectAsStateWithLifecycle()
    val managementState by managementViewModel.state.collectAsStateWithLifecycle()
    val current = backStack.lastOrNull()
    val isRoot = rootTabs.any { it.key == current }
    val ledgerSnackbar = remember { SnackbarHostState() }
    val accountSnackbar = remember { SnackbarHostState() }
    val categorySnackbar = remember { SnackbarHostState() }

    fun openEditor(id: String? = null) {
        backStack.add(TransactionEditorKey(id, UUID.randomUUID().toString()))
    }

    fun switchRoot(key: AppNavKey) {
        backStack.clear()
        backStack.add(key)
    }

    Scaffold(
        bottomBar = {
            if (isRoot) NavigationBar {
                rootTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.key,
                        onClick = { switchRoot(tab.key) },
                        icon = { Text(tab.glyph) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (isRoot) FloatingActionButton(onClick = { openEditor() }) { Text("＋记一笔") }
        },
    ) { outerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(outerPadding),
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    HomeKey -> NavEntry(key) { PlaceholderScreen("首页", "净资产与月度摘要将在 I3 接入。") }
                    LedgerKey -> NavEntry(key) {
                        val transactions = ledgerViewModel.transactions.collectAsLazyPagingItems()
                        LedgerScreen(
                            state = ledgerState,
                            transactions = transactions,
                            snackbarHostState = ledgerSnackbar,
                            onQueryChanged = ledgerViewModel::setNoteQuery,
                            onTypeChanged = ledgerViewModel::setType,
                            onAccountChanged = ledgerViewModel::setAccount,
                            onCategoryChanged = ledgerViewModel::setCategory,
                            onDateChanged = ledgerViewModel::setDateRange,
                            onClearFilters = ledgerViewModel::clearFilters,
                            onTransactionClick = { backStack.add(TransactionDetailKey(it)) },
                            onRestore = ledgerViewModel::restoreLastDeleted,
                            onDeletedConsumed = ledgerViewModel::consumeDeleted,
                            onMessageConsumed = ledgerViewModel::consumeMessage,
                        )
                    }
                    StatisticsKey -> NavEntry(key) { PlaceholderScreen("统计", "月度趋势与分类占比将在 I3 接入。") }
                    SettingsKey -> NavEntry(key) {
                        SettingsScreen(
                            onAccounts = { backStack.add(AccountsKey) },
                            onCategories = { backStack.add(CategoriesKey) },
                        )
                    }
                    AccountsKey -> NavEntry(key) {
                        AccountsScreen(
                            state = managementState,
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
                            state = managementState,
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
private fun SettingsScreen(onAccounts: () -> Unit, onCategories: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("设置") })
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onAccounts, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text("账户管理") }
            Button(onClick = onCategories, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text("分类管理") }
            Text("数据仅保存在本机；备份与恢复将在 I4 接入。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(title: String, message: String) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(title) })
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
