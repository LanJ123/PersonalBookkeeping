package com.personalbookkeeping.ui.settings

import android.net.Uri
import com.personalbookkeeping.backup.BackupArchive
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.personalbookkeeping.domain.model.ThemeMode
import com.personalbookkeeping.ui.theme.IosSegmentOption
import com.personalbookkeeping.ui.theme.IosSegmentedControl
import com.personalbookkeeping.ui.theme.IosBackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTransferScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onBackup: (Uri) -> Unit,
    onRestoreSelected: (Uri) -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    onExportCsv: (Uri, LocalDate, LocalDate) -> Unit,
    onInputError: (String) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var fromText by remember { mutableStateOf(today.withDayOfMonth(1).toString()) }
    var toText by remember { mutableStateOf(today.toString()) }
    var pendingCsvRange by remember { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var showBackupWarning by remember { mutableStateOf(false) }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupArchive.MIME_TYPE),
        onResult = { it?.let(onBackup) },
    )
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
        onResult = { it?.let(onRestoreSelected) },
    )
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            val range = pendingCsvRange
            if (uri != null && range != null) onExportCsv(uri, range.first, range.second)
            pendingCsvRange = null
        },
    )

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("数据与备份") },
            navigationIcon = { IosBackButton(onBack) },
            expandedHeight = 52.dp,
            windowInsets = WindowInsets(0.dp),
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("完整备份", style = MaterialTheme.typography.titleMedium)
            Text(".pbk 可用于恢复全部本地记账数据；文件未加密。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { showBackupWarning = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text("创建完整备份")
            }
            OutlinedButton(
                onClick = {
                    restoreLauncher.launch(
                        arrayOf(BackupArchive.MIME_TYPE, "application/zip", "application/octet-stream", "*/*"),
                    )
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("选择备份并恢复") }

            Text("CSV 导出", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text("CSV 仅用于查看和分析，不能用于完整恢复。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    fromText,
                    { fromText = it },
                    label = { Text("开始日期") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    toText,
                    { toText = it },
                    label = { Text("结束日期") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            Button(
                onClick = {
                    val start = runCatching { LocalDate.parse(fromText, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
                    val end = runCatching { LocalDate.parse(toText, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
                    if (start == null || end == null || end.isBefore(start)) {
                        onInputError("请输入有效的 YYYY-MM-DD 日期范围")
                    } else {
                        pendingCsvRange = start to end
                        csvLauncher.launch("个人记账-${start}-${end}.csv")
                    }
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导出 CSV") }
            if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }

    if (showBackupWarning) {
        AlertDialog(
            onDismissRequest = { showBackupWarning = false },
            title = { Text("创建完整备份") },
            text = { Text("备份包含个人财务数据且未加密，请保存到可信位置。") },
            confirmButton = {
                TextButton(onClick = {
                    showBackupWarning = false
                    backupLauncher.launch("个人记账-${today}.pbk")
                }) { Text("选择保存位置") }
            },
            dismissButton = { TextButton(onClick = { showBackupWarning = false }) { Text("取消") } },
        )
    }
    state.restoreReview?.let { review ->
        AlertDialog(
            onDismissRequest = onCancelRestore,
            title = { Text("确认恢复") },
            text = {
                Text(
                    "备份时间：${review.createdAt}\n" +
                        "应用版本：${review.appVersionName}\n" +
                        "账户 ${review.counts.accounts} · 分类 ${review.counts.categories}\n" +
                        "流水 ${review.counts.transactions} · 预算 ${review.counts.budgets}\n\n" +
                        "恢复将替换当前数据，并先在本机创建回滚快照。",
                )
            },
            confirmButton = { TextButton(onClick = onConfirmRestore) { Text("确认恢复") } },
            dismissButton = { TextButton(onClick = onCancelRestore) { Text("取消") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    amountsHidden: Boolean,
    themeMode: ThemeMode,
    appLockEnabled: Boolean,
    appLockMessage: String?,
    onBack: () -> Unit,
    onAmountsHiddenChanged: (Boolean) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAppLockChanged: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("隐私与安全") },
            navigationIcon = { IosBackButton(onBack) },
            expandedHeight = 52.dp,
            windowInsets = WindowInsets(0.dp),
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("主题", style = MaterialTheme.typography.titleMedium)
            IosSegmentedControl(
                options = ThemeMode.entries.map { mode ->
                    IosSegmentOption(
                        label = when (mode) {
                            ThemeMode.SYSTEM -> "跟随系统"
                            ThemeMode.LIGHT -> "浅色"
                            ThemeMode.DARK -> "深色"
                        },
                        testTag = "privacy-theme-${mode.name.lowercase()}",
                    )
                },
                selectedIndex = ThemeMode.entries.indexOf(themeMode),
                onSelected = { onThemeModeChanged(ThemeMode.entries[it]) },
            )
            SettingSwitchRow(
                title = "隐藏金额",
                description = "首页、流水、统计、预算和账户中的只读金额显示为 ••••",
                testTag = "privacy-hide-amounts",
                checked = amountsHidden,
                onCheckedChange = onAmountsHiddenChanged,
            )
            SettingSwitchRow(
                title = "应用锁",
                description = "离开应用 30 秒后，使用设备屏幕锁或生物识别解锁",
                testTag = "privacy-app-lock",
                checked = appLockEnabled,
                onCheckedChange = onAppLockChanged,
            )
            appLockMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("认证由 Android 系统完成，应用不保存 PIN 或生物特征。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    testTag: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}
