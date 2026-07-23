package com.personalbookkeeping.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalbookkeeping.backup.BackupFormatException
import com.personalbookkeeping.backup.PortabilityService
import com.personalbookkeeping.backup.RestoreReview
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val restoreReview: RestoreReview? = null,
)

class SettingsViewModel(private val service: PortabilityService) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    fun createBackup(uri: Uri) = runOperation {
        val result = service.createBackup(uri)
        val name = result.fileName ?: "所选文件"
        val size = humanBytes(result.bytes)
        "$name 备份成功 · $size · ${result.counts.transactions} 笔流水"
    }

    fun inspectBackup(uri: Uri) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null, restoreReview = null) }
            try {
                val review = service.inspectBackup(uri)
                mutableState.update { it.copy(busy = false, restoreReview = review) }
            } catch (error: Exception) {
                mutableState.update { it.copy(busy = false, message = error.localMessage()) }
            }
        }
    }

    fun confirmRestore() {
        val review = mutableState.value.restoreReview ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            try {
                service.restore(review.token)
                mutableState.update {
                    it.copy(busy = false, restoreReview = null, message = "恢复成功，数据已通过完整性校验")
                }
            } catch (error: Exception) {
                mutableState.update { it.copy(busy = false, message = error.localMessage()) }
            }
        }
    }

    fun cancelRestoreReview() {
        mutableState.value.restoreReview?.let { service.discardPendingRestore(it.token) }
        mutableState.update { it.copy(restoreReview = null) }
    }

    fun exportCsv(uri: Uri, startDate: LocalDate, endDate: LocalDate) = runOperation {
        val result = service.exportCsv(uri, startDate, endDate)
        "CSV 导出成功 · ${result.rows} 行 · ${humanBytes(result.bytes)}"
    }

    fun setHideAmounts(hidden: Boolean) {
        viewModelScope.launch {
            try {
                service.setHideAmounts(hidden)
            } catch (_: Exception) {
                mutableState.update { it.copy(message = "金额隐私设置保存失败") }
            }
        }
    }

    fun showMessage(message: String) {
        mutableState.update { it.copy(message = message) }
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null) }
    }

    private fun runOperation(operation: suspend () -> String) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            try {
                mutableState.update { it.copy(busy = false, message = operation()) }
            } catch (error: Exception) {
                mutableState.update { it.copy(busy = false, message = error.localMessage()) }
            }
        }
    }

    private fun Exception.localMessage(): String = when ((this as? BackupFormatException)?.reason) {
        BackupFormatException.Reason.UNSUPPORTED_FORMAT -> "不是个人记账备份文件"
        BackupFormatException.Reason.UNSUPPORTED_VERSION -> "备份版本高于当前应用支持范围"
        BackupFormatException.Reason.CORRUPT_ARCHIVE -> "备份归档损坏或包含不安全条目"
        BackupFormatException.Reason.INTEGRITY_MISMATCH -> "备份完整性校验失败"
        BackupFormatException.Reason.INVALID_DATA -> "备份数据不一致，未修改现有数据"
        BackupFormatException.Reason.TOO_LARGE -> "备份文件超过安全上限"
        null -> "本地文件操作失败，请检查空间或重新选择文件"
    }

    private fun humanBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KiB"
        else -> "${bytes / (1024 * 1024)} MiB"
    }
}

class SettingsViewModelFactory(private val service: PortabilityService) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(service) as T
}
