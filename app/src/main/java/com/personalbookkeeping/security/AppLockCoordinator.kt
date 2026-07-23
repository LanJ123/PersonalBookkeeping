package com.personalbookkeeping.security

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun interface MonotonicClock {
    fun elapsedRealtimeMs(): Long
}

object SystemMonotonicClock : MonotonicClock {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
}

data class AppLockState(
    val ready: Boolean = false,
    val enabled: Boolean = false,
    val locked: Boolean = false,
    val timeoutSeconds: Int = SecuritySettingsRepository.DEFAULT_TIMEOUT_SECONDS,
    val message: String? = null,
)

class AppLockCoordinator(
    private val repository: AppLockSettingsStore,
    private val clock: MonotonicClock = SystemMonotonicClock,
    scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(AppLockState())
    val state: StateFlow<AppLockState> = mutableState.asStateFlow()
    private var backgroundAtMs: Long? = null

    init {
        scope.launch(SupervisorJob()) {
            repository.settings.collect { settings ->
                mutableState.update { old ->
                    val firstLoad = !old.ready
                    AppLockState(
                        ready = true,
                        enabled = settings.appLockEnabled,
                        locked = settings.appLockEnabled && (firstLoad || old.locked),
                        timeoutSeconds = settings.backgroundTimeoutSeconds,
                        message = old.message,
                    )
                }
            }
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        repository.setAppLockEnabled(enabled)
        mutableState.update { it.copy(enabled = enabled, locked = false, message = null) }
    }

    fun onBackground() {
        if (mutableState.value.enabled) backgroundAtMs = clock.elapsedRealtimeMs()
    }

    fun onForeground() {
        val backgroundAt = backgroundAtMs ?: return
        backgroundAtMs = null
        val elapsed = (clock.elapsedRealtimeMs() - backgroundAt).coerceAtLeast(0L)
        val timeoutMs = mutableState.value.timeoutSeconds * 1_000L
        if (shouldLock(mutableState.value.enabled, elapsed, timeoutMs)) {
            mutableState.update { it.copy(locked = true, message = null) }
        }
    }

    fun unlock() {
        mutableState.update { it.copy(locked = false, message = null) }
    }

    fun authenticationUnavailable() {
        mutableState.update { it.copy(message = "设备尚未配置可用的屏幕锁或生物识别") }
    }

    fun authenticationFailed() {
        mutableState.update { it.copy(message = "未通过系统认证，请重试") }
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null) }
    }

    companion object {
        fun shouldLock(enabled: Boolean, elapsedMs: Long, timeoutMs: Long): Boolean =
            enabled && elapsedMs >= timeoutMs
    }
}
