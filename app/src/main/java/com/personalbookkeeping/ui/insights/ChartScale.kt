package com.personalbookkeeping.ui.insights

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal data class ChartScale(
    val axisMax: Long,
    val tickStep: Long,
    val tickCount: Int,
) {
    fun valueAt(tick: Int): Long = tickStep * tick.coerceIn(0, tickCount)
}

/**
 * Builds a zero-based scale from the values currently visible in the chart.
 *
 * The additional 1/1.2/1.5/2/2.5/3/4/5/6/8 steps keep mobile chart labels
 * readable without letting a small transaction inherit a large fixed range.
 */
internal fun dynamicChartScale(
    values: Iterable<Long>,
    preferredTickCount: Int = 6,
): ChartScale {
    require(preferredTickCount > 0)
    val maximum = values.maxOrNull()?.coerceAtLeast(0L) ?: 0L
    if (maximum == 0L) {
        return ChartScale(
            axisMax = preferredTickCount * 100L,
            tickStep = 100L,
            tickCount = preferredTickCount,
        )
    }

    val rawStep = maximum.toDouble() / preferredTickCount
    val tickStep = niceStepAtLeast(rawStep).coerceAtLeast(1L)
    val requiredTicks = ceil(maximum.toDouble() / tickStep).toInt()
    val tickCount = maxOf(preferredTickCount, requiredTicks)
    return ChartScale(
        axisMax = tickStep * tickCount,
        tickStep = tickStep,
        tickCount = tickCount,
    )
}

private fun niceStepAtLeast(value: Double): Long {
    if (!value.isFinite() || value <= 1.0) return 1L
    val magnitude = 10.0.pow(floor(log10(value)))
    val normalized = value / magnitude
    val factor = NICE_STEP_FACTORS.firstOrNull { it >= normalized } ?: 10.0
    return ceil(factor * magnitude).toLong()
}

private val NICE_STEP_FACTORS =
    doubleArrayOf(1.0, 1.2, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0)
