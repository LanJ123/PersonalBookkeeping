package com.personalbookkeeping.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyParserTest {
    @Test
    fun `parses integer and two decimal inputs into minor units`() {
        assertSuccess("25.50", 2_550)
        assertSuccess("25.5", 2_550)
        assertSuccess("25", 2_500)
        assertSuccess("0.01", 1)
    }

    @Test
    fun `rejects empty zero negative excess precision and above maximum`() {
        assertFailure("", MoneyParseFailure.EMPTY)
        assertFailure("0", MoneyParseFailure.NON_POSITIVE)
        assertFailure("-1", MoneyParseFailure.NON_POSITIVE)
        assertFailure("1.001", MoneyParseFailure.TOO_MANY_FRACTION_DIGITS)
        assertFailure("1000000000.00", MoneyParseFailure.ABOVE_MAXIMUM)
    }

    @Test
    fun `formats minor units without floating point arithmetic`() {
        assertEquals("¥1,234.50", Money.fromMinor(123_450).formatCny())
        assertEquals("-¥25.05", Money.fromMinor(-2_505).formatCny())
        assertEquals("+¥25.05", Money.fromMinor(2_505).formatCny(showPositiveSign = true))
    }

    private fun assertSuccess(raw: String, expectedMinor: Long) {
        val result = MoneyParser.parsePositive(raw)
        assertTrue(result is MoneyParseResult.Success)
        assertEquals(expectedMinor, (result as MoneyParseResult.Success).money.minorUnits)
    }

    private fun assertFailure(raw: String, expected: MoneyParseFailure) {
        assertEquals(MoneyParseResult.Failure(expected), MoneyParser.parsePositive(raw))
    }
}
