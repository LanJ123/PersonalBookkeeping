package com.personalbookkeeping.common

import org.junit.Assert.assertEquals
import org.junit.Test

class NameNormalizerTest {
    @Test
    fun activeKeyUsesNfkcRemovesWhitespaceAndIgnoresCase() {
        assertEquals("abc现金", NameNormalizer.activeKey("  Ａ b C 现 金 "))
    }

    @Test
    fun displayNameTrimsAndNormalizesCompatibilityCharacters() {
        assertEquals("Cash 账户", NameNormalizer.displayName("  Ｃａｓｈ 账户  "))
    }
}
