package com.personalbookkeeping.common

import java.text.Normalizer
import java.util.Locale

object NameNormalizer {
    private val whitespace = Regex("\\s+")

    fun displayName(raw: String): String = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)

    fun activeKey(raw: String): String = displayName(raw)
        .replace(whitespace, "")
        .lowercase(Locale.ROOT)
}
