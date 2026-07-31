package com.personalbookkeeping.ui.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartScaleTest {
    @Test
    fun `small values do not use a fixed one hundred yuan range`() {
        val scale = dynamicChartScale(listOf(139L))

        assertEquals(150L, scale.axisMax)
        assertEquals(25L, scale.tickStep)
        assertEquals(6, scale.tickCount)
    }

    @Test
    fun `hundreds of yuan produce readable dynamic ticks`() {
        val scale = dynamicChartScale(listOf(6_000L, 35_000L, 12_000L))

        assertEquals(36_000L, scale.axisMax)
        assertEquals(6_000L, scale.tickStep)
        assertEquals(6, scale.tickCount)
    }

    @Test
    fun `scale always contains the actual maximum`() {
        val scale = dynamicChartScale(listOf(99_999L))

        assertTrue(scale.axisMax >= 99_999L)
        assertEquals(scale.axisMax, scale.valueAt(scale.tickCount))
        assertEquals(0L, scale.valueAt(0))
    }

    @Test
    fun `all zero data still has a compact usable scale`() {
        val scale = dynamicChartScale(listOf(0L, 0L))

        assertEquals(600L, scale.axisMax)
        assertEquals(100L, scale.tickStep)
    }
}
