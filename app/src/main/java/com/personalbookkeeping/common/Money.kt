package com.personalbookkeeping.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

@JvmInline
value class Money private constructor(val minorUnits: Long) {
    fun formatCny(showPositiveSign: Boolean = false): String {
        val sign = when {
            minorUnits < 0 -> "-"
            showPositiveSign && minorUnits > 0 -> "+"
            else -> ""
        }
        val absoluteMinor = BigDecimal.valueOf(minorUnits).abs()
        val whole = absoluteMinor.divideToIntegralValue(HUNDRED).longValueExact()
        val fraction = absoluteMinor.remainder(HUNDRED).intValueExact()
        val groupedWhole = NumberFormat.getIntegerInstance(Locale.CHINA).format(whole)
        return "$sign¥$groupedWhole.${fraction.toString().padStart(2, '0')}"
    }

    fun toPlainDecimal(): String = BigDecimal.valueOf(minorUnits, 2)
        .stripTrailingZeros()
        .toPlainString()

    companion object {
        fun fromMinor(minorUnits: Long): Money = Money(minorUnits)

        private val HUNDRED = BigDecimal.valueOf(100)
    }
}

sealed interface MoneyParseResult {
    data class Success(val money: Money) : MoneyParseResult
    data class Failure(val reason: MoneyParseFailure) : MoneyParseResult
}

enum class MoneyParseFailure {
    EMPTY,
    INVALID_FORMAT,
    NON_POSITIVE,
    TOO_MANY_FRACTION_DIGITS,
    ABOVE_MAXIMUM,
}

object MoneyParser {
    const val MAX_MINOR_UNITS: Long = 99_999_999_999L

    private val validPattern = Regex("^\\d+(?:\\.\\d{1,2})?$")
    private val excessFractionPattern = Regex("^\\d+\\.\\d{3,}$")
    private val signedPattern = Regex("^[+-]?\\d+(?:\\.\\d{1,2})?$")
    private val signedExcessFractionPattern = Regex("^[+-]?\\d+\\.\\d{3,}$")

    fun parsePositive(raw: String): MoneyParseResult {
        val input = raw.trim()
        if (input.isEmpty()) return MoneyParseResult.Failure(MoneyParseFailure.EMPTY)
        if (input.startsWith('-')) {
            return MoneyParseResult.Failure(MoneyParseFailure.NON_POSITIVE)
        }
        if (excessFractionPattern.matches(input)) {
            return MoneyParseResult.Failure(MoneyParseFailure.TOO_MANY_FRACTION_DIGITS)
        }
        if (!validPattern.matches(input)) {
            return MoneyParseResult.Failure(MoneyParseFailure.INVALID_FORMAT)
        }

        val decimal = input.toBigDecimalOrNull()
            ?: return MoneyParseResult.Failure(MoneyParseFailure.INVALID_FORMAT)
        if (decimal.signum() <= 0) {
            return MoneyParseResult.Failure(MoneyParseFailure.NON_POSITIVE)
        }

        val minorUnits = try {
            decimal
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        } catch (_: ArithmeticException) {
            return MoneyParseResult.Failure(MoneyParseFailure.ABOVE_MAXIMUM)
        }
        if (minorUnits > MAX_MINOR_UNITS) {
            return MoneyParseResult.Failure(MoneyParseFailure.ABOVE_MAXIMUM)
        }
        return MoneyParseResult.Success(Money.fromMinor(minorUnits))
    }

    fun parseSigned(raw: String): MoneyParseResult {
        val input = raw.trim()
        if (input.isEmpty()) return MoneyParseResult.Failure(MoneyParseFailure.EMPTY)
        if (signedExcessFractionPattern.matches(input)) {
            return MoneyParseResult.Failure(MoneyParseFailure.TOO_MANY_FRACTION_DIGITS)
        }
        if (!signedPattern.matches(input)) {
            return MoneyParseResult.Failure(MoneyParseFailure.INVALID_FORMAT)
        }
        val minorUnits = try {
            input.toBigDecimal()
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        } catch (_: ArithmeticException) {
            return MoneyParseResult.Failure(MoneyParseFailure.ABOVE_MAXIMUM)
        }
        if (kotlin.math.abs(minorUnits) > MAX_MINOR_UNITS) {
            return MoneyParseResult.Failure(MoneyParseFailure.ABOVE_MAXIMUM)
        }
        return MoneyParseResult.Success(Money.fromMinor(minorUnits))
    }
}
