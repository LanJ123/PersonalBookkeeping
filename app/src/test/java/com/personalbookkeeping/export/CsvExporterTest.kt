package com.personalbookkeeping.export

import com.personalbookkeeping.database.dao.CsvTransactionRow
import java.io.ByteArrayOutputStream
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    @Test
    fun writesBomFixedColumnsCrlfAndRfc4180Escaping() {
        val time = Instant.parse("2026-07-23T04:30:00Z").toEpochMilli()
        val output = ByteArrayOutputStream()
        val result = CsvExporter.write(
            rows = listOf(
                CsvTransactionRow(
                    type = "EXPENSE",
                    amountMinor = 2350,
                    categoryName = "餐饮,日常",
                    accountName = "现金",
                    targetAccountName = null,
                    occurredAtMs = time,
                    zoneId = "Asia/Shanghai",
                    note = "午餐\n\"很好\"",
                    createdAtMs = time,
                    updatedAtMs = time,
                ),
            ),
            output = output,
        )

        val bytes = output.toByteArray()
        assertEquals(listOf(0xEF, 0xBB, 0xBF), bytes.take(3).map { it.toInt() and 0xFF })
        val text = bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)
        assertTrue(text.startsWith("type,amount,currency,category,account,target_account,occurred_at,note,created_at,updated_at\r\n"))
        assertTrue(text.contains("EXPENSE,23.50,CNY,\"餐饮,日常\",现金,"))
        assertTrue(text.contains("\"午餐\n\"\"很好\"\"\""))
        assertTrue(text.endsWith("\r\n"))
        assertEquals(1, result.rows)
        assertEquals(bytes.size.toLong(), result.bytes)
    }

    @Test
    fun escapeLeavesPlainTextAndDoublesQuotes() {
        assertEquals("plain", CsvExporter.escape("plain"))
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"))
        assertEquals("\"a\"\"b\"", CsvExporter.escape("a\"b"))
    }
}
