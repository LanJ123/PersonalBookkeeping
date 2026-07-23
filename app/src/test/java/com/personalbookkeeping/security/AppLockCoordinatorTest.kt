package com.personalbookkeeping.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockCoordinatorTest {
    @Test
    fun timeoutBoundaryUsesMonotonicElapsedDuration() {
        assertFalse(AppLockCoordinator.shouldLock(enabled = true, elapsedMs = 29_999, timeoutMs = 30_000))
        assertTrue(AppLockCoordinator.shouldLock(enabled = true, elapsedMs = 30_000, timeoutMs = 30_000))
        assertFalse(AppLockCoordinator.shouldLock(enabled = false, elapsedMs = 60_000, timeoutMs = 30_000))
    }

    @Test
    fun enabledColdStartLocksAndSuccessfulAuthenticationUnlocks() {
        val fixture = fixture(enabled = true)

        assertTrue(fixture.coordinator.state.value.ready)
        assertTrue(fixture.coordinator.state.value.enabled)
        assertTrue(fixture.coordinator.state.value.locked)

        fixture.coordinator.authenticationFailed()
        assertEquals("未通过系统认证，请重试", fixture.coordinator.state.value.message)
        fixture.coordinator.unlock()
        assertFalse(fixture.coordinator.state.value.locked)
        assertEquals(null, fixture.coordinator.state.value.message)
    }

    @Test
    fun backgroundTimeoutUsesInjectedMonotonicClock() {
        val fixture = fixture(enabled = true)
        fixture.coordinator.unlock()
        fixture.nowMs = 1_000
        fixture.coordinator.onBackground()
        fixture.nowMs = 30_999
        fixture.coordinator.onForeground()
        assertFalse(fixture.coordinator.state.value.locked)

        fixture.nowMs = 40_000
        fixture.coordinator.onBackground()
        fixture.nowMs = 70_000
        fixture.coordinator.onForeground()
        assertTrue(fixture.coordinator.state.value.locked)
    }

    @Test
    fun enableDisableAndMessagesAreReflectedInState() = runBlocking {
        val fixture = fixture(enabled = false)

        fixture.coordinator.setEnabled(true)
        assertTrue(fixture.store.settings.value.appLockEnabled)
        assertTrue(fixture.coordinator.state.value.enabled)
        assertFalse(fixture.coordinator.state.value.locked)

        fixture.coordinator.authenticationUnavailable()
        assertEquals("设备尚未配置可用的屏幕锁或生物识别", fixture.coordinator.state.value.message)
        fixture.coordinator.consumeMessage()
        assertEquals(null, fixture.coordinator.state.value.message)

        fixture.coordinator.setEnabled(false)
        assertFalse(fixture.store.settings.value.appLockEnabled)
        assertFalse(fixture.coordinator.state.value.enabled)
    }

    private fun fixture(enabled: Boolean): Fixture {
        val store = FakeStore(enabled)
        var now = 0L
        val coordinator = AppLockCoordinator(
            repository = store,
            clock = MonotonicClock { now },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
        return Fixture(
            coordinator = coordinator,
            store = store,
            getNow = { now },
            setNow = { now = it },
        )
    }

    private class FakeStore(enabled: Boolean) : AppLockSettingsStore {
        override val settings = MutableStateFlow(SecuritySettings(enabled, 30))

        override suspend fun setAppLockEnabled(enabled: Boolean) {
            settings.value = SecuritySettings(enabled, 30)
        }
    }

    private class Fixture(
        val coordinator: AppLockCoordinator,
        val store: FakeStore,
        private val getNow: () -> Long,
        private val setNow: (Long) -> Unit,
    ) {
        var nowMs: Long
            get() = getNow()
            set(value) = setNow(value)
    }
}
