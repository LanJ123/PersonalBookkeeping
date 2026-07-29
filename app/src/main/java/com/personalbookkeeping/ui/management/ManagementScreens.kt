package com.personalbookkeeping.ui.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personalbookkeeping.domain.model.AccountType
import com.personalbookkeeping.domain.model.CategoryKind
import com.personalbookkeeping.domain.model.ItemStatus
import com.personalbookkeeping.domain.model.ManagedAccount
import com.personalbookkeeping.domain.model.ManagedCategory
import com.personalbookkeeping.domain.model.MoveDirection
import com.personalbookkeeping.ui.privacy.displayCny
import com.personalbookkeeping.ui.theme.IosBackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    state: ManagementUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: (String?, String, AccountType, String, Boolean) -> Unit,
    onDeactivate: (String) -> Unit,
    onMove: (String, MoveDirection) -> Unit,
    onMessageConsumed: () -> Unit,
) {
    var editing by remember { mutableStateOf<ManagedAccount?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    MessageEffect(state, snackbarHostState, onMessageConsumed)
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("账户管理") },
            navigationIcon = { IosBackButton(onBack) },
            expandedHeight = 52.dp,
            windowInsets = WindowInsets(0.dp),
            actions = { TextButton(onClick = { editing = null; showDialog = true }) { Text("新增") } },
        )
        if (state.isLoading) CenterLoading() else LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val active = state.accounts.filter { it.status == ItemStatus.ACTIVE }
            val inactive = state.accounts.filter { it.status == ItemStatus.INACTIVE }
            items(active, key = { it.id }) { account ->
                AccountCard(account, active.indexOf(account), active.lastIndex, { editing = account; showDialog = true }, onDeactivate, onMove)
            }
            if (inactive.isNotEmpty()) {
                item { Text("已停用", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
                items(inactive, key = { it.id }) { account -> AccountCard(account, -1, -1, {}, {}, { _, _ -> }) }
            }
        }
    }
    if (showDialog) AccountDialog(editing, { showDialog = false }) { id, name, type, opening, include ->
        onSave(id, name, type, opening, include); showDialog = false
    }
}

@Composable
private fun AccountCard(
    account: ManagedAccount,
    index: Int,
    lastIndex: Int,
    onEdit: () -> Unit,
    onDeactivate: (String) -> Unit,
    onMove: (String, MoveDirection) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(account.name, fontWeight = FontWeight.SemiBold)
                Text(account.balance.displayCny(), fontWeight = FontWeight.SemiBold)
            }
            Text("${account.type.label()} · 期初 ${account.openingBalance.displayCny()} · ${account.transactionCount} 笔流水", style = MaterialTheme.typography.bodySmall)
            if (account.status == ItemStatus.INACTIVE) {
                Text("已停用（历史流水保留）", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onEdit) { Text("编辑") }
                    TextButton(onClick = { onMove(account.id, MoveDirection.UP) }, enabled = index > 0) { Text("上移") }
                    TextButton(onClick = { onMove(account.id, MoveDirection.DOWN) }, enabled = index < lastIndex) { Text("下移") }
                    TextButton(onClick = { onDeactivate(account.id) }) { Text("停用") }
                }
            }
        }
    }
}

@Composable
private fun AccountDialog(
    account: ManagedAccount?,
    onDismiss: () -> Unit,
    onSave: (String?, String, AccountType, String, Boolean) -> Unit,
) {
    var name by remember(account?.id) { mutableStateOf(account?.name.orEmpty()) }
    var type by remember(account?.id) { mutableStateOf(account?.type ?: AccountType.CASH) }
    var opening by remember(account?.id) { mutableStateOf(account?.openingBalance?.toPlainDecimal() ?: "0") }
    var include by remember(account?.id) { mutableStateOf(account?.includeInAssets ?: true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "新增账户" else "编辑账户") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("账户名称") }, singleLine = true)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AccountType.entries) { option -> FilterChip(type == option, { type = option }, { Text(option.label()) }) }
                }
                OutlinedTextField(opening, { opening = it.take(18) }, label = { Text("期初余额（可为负数）") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(include, { include = it }); Text("计入总资产")
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(account?.id, name, type, opening, include) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    state: ManagementUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: (String?, CategoryKind, String) -> Unit,
    onDeactivate: (String) -> Unit,
    onMove: (String, MoveDirection) -> Unit,
    onMessageConsumed: () -> Unit,
) {
    var kind by remember { mutableStateOf(CategoryKind.EXPENSE) }
    var editing by remember { mutableStateOf<ManagedCategory?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    MessageEffect(state, snackbarHostState, onMessageConsumed)
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("分类管理") },
            navigationIcon = { IosBackButton(onBack) },
            expandedHeight = 52.dp,
            windowInsets = WindowInsets(0.dp),
            actions = { TextButton(onClick = { editing = null; showDialog = true }) { Text("新增") } },
        )
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(kind == CategoryKind.EXPENSE, { kind = CategoryKind.EXPENSE }, { Text("支出分类") })
            FilterChip(kind == CategoryKind.INCOME, { kind = CategoryKind.INCOME }, { Text("收入分类") })
        }
        val categories = if (kind == CategoryKind.EXPENSE) state.expenseCategories else state.incomeCategories
        if (state.isLoading) CenterLoading() else LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val active = categories.filter { it.status == ItemStatus.ACTIVE }
            val inactive = categories.filter { it.status == ItemStatus.INACTIVE }
            items(active, key = { it.id }) { category ->
                CategoryCard(category, active.indexOf(category), active.lastIndex, { editing = category; showDialog = true }, onDeactivate, onMove)
            }
            if (inactive.isNotEmpty()) {
                item { Text("已停用", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
                items(inactive, key = { it.id }) { category -> CategoryCard(category, -1, -1, {}, {}, { _, _ -> }) }
            }
        }
    }
    if (showDialog) {
        var name by remember(editing?.id) { mutableStateOf(editing?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "新增分类" else "重命名分类") },
            text = { OutlinedTextField(name, { name = it.take(40) }, label = { Text("分类名称") }, singleLine = true) },
            confirmButton = { Button(onClick = { onSave(editing?.id, editing?.kind ?: kind, name); showDialog = false }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun CategoryCard(
    category: ManagedCategory,
    index: Int,
    lastIndex: Int,
    onEdit: () -> Unit,
    onDeactivate: (String) -> Unit,
    onMove: (String, MoveDirection) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(category.name, fontWeight = FontWeight.SemiBold)
            Text("${category.transactionCount} 笔历史流水", style = MaterialTheme.typography.bodySmall)
            if (category.status == ItemStatus.INACTIVE) {
                Text("已停用（历史流水保留）", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onEdit) { Text("重命名") }
                    TextButton(onClick = { onMove(category.id, MoveDirection.UP) }, enabled = index > 0) { Text("上移") }
                    TextButton(onClick = { onMove(category.id, MoveDirection.DOWN) }, enabled = index < lastIndex) { Text("下移") }
                    TextButton(onClick = { onDeactivate(category.id) }) { Text("停用") }
                }
            }
        }
    }
}

@Composable
private fun CenterLoading() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageEffect(state: ManagementUiState, host: SnackbarHostState, onConsumed: () -> Unit) {
    LaunchedEffect(state.message) {
        state.message?.let { host.showSnackbar(it); onConsumed() }
    }
    SnackbarHost(host)
}

private fun AccountType.label(): String = when (this) {
    AccountType.CASH -> "现金"
    AccountType.BANK -> "银行"
    AccountType.E_WALLET -> "电子钱包"
    AccountType.STORED_VALUE -> "储值卡"
    AccountType.CREDIT_CARD -> "信用卡"
    AccountType.OTHER -> "其他"
}
