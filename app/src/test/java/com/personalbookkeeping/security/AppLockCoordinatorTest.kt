package com.personalbookkeeping.security

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
}
