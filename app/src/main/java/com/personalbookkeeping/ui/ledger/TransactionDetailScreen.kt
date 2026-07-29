package com.personalbookkeeping.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personalbookkeeping.domain.model.TransactionType
import com.personalbookkeeping.ui.privacy.displayCny
import com.personalbookkeeping.ui.theme.IosBackButton
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    state: TransactionDetailUiState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("流水详情") },
                navigationIcon = { IosBackButton(onBack) },
                expandedHeight = 52.dp,
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            state.transaction == null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { Text("这笔流水不存在或已删除") }
            else -> {
                val transaction = state.transaction
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(transaction.type.label(), style = MaterialTheme.typography.titleMedium)
                    val amount = when (transaction.type) {
                        TransactionType.EXPENSE -> "-${transaction.amount.displayCny()}"
                        TransactionType.INCOME -> transaction.amount.displayCny(showPositiveSign = true)
                        TransactionType.TRANSFER -> transaction.amount.displayCny()
                    }
                    Text(amount, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    FactRow("分类", transaction.categoryName ?: "—")
                    FactRow("账户", transaction.accountName)
                    transaction.targetAccountName?.let { FactRow("转入账户", it) }
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .withZone(ZoneId.systemDefault())
                    FactRow("发生时间", formatter.format(transaction.occurredAt))
                    FactRow("备注", transaction.note ?: "—")
                    FactRow("创建时间", formatter.format(transaction.createdAt))
                    FactRow("更新时间", formatter.format(transaction.updatedAt))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onEdit(transaction.id) }, modifier = Modifier.weight(1f)) { Text("编辑") }
                        OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) { Text("删除") }
                    }
                }
            }
        }
    }
    if (confirmDelete && state.transaction != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这笔流水？") },
            text = { Text("删除会立即影响账户余额，返回流水页后可短时撤销。") },
            confirmButton = {
                Button(onClick = { confirmDelete = false; onDelete(state.transaction.id) }) { Text("确认删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.padding(start = 20.dp), maxLines = 3)
    }
}
