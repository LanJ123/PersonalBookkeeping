package com.personalbookkeeping.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFilterTest {
    @Test
    fun defaultFilterIsInactive() {
        assertFalse(TransactionFilter().isActive)
    }

    @Test
    fun noteMakesFilterActive() {
        assertTrue(TransactionFilter(noteQuery = "午饭").isActive)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsReversedDateRange() {
        TransactionFilter(fromEpochDay = 20, toEpochDay = 10)
    }
}
