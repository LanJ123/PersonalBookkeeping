package com.personalbookkeeping.export

import com.personalbookkeeping.database.dao.CsvTransactionRow
import java.io.OutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

data class CsvExportResult(val rows: Int, val bytes: Long)

object CsvExporter {
    private val header = listOf(
        "type",
        "amount",
        "currency",
        "category",
        "account",
        "target_account",
        "occurred_at",
        "note",
        "created_at",
        "updated_at",
    )

    fun write(rows: List<CsvTransactionRow>, output: OutputStream): CsvExportResult {
        var bytes = 0L
        fun writeBytes(value: ByteArray) {
            output.write(value)
            bytes += value.size
        }
        writeBytes(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        writeBytes((header.joinToString(",") + "\r\n").toByteArray(Charsets.UTF_8))
        rows.forEach { row ->
            val zone = ZoneId.of(row.zoneId)
            val values = listOf(
                row.type,
                BigDecimal.valueOf(row.amountMinor, 2).toPlainString(),
                "CNY",
                row.categoryName.orEmpty(),
                row.accountName,
                row.targetAccountName.orEmpty(),
                Instant.ofEpochMilli(row.occurredAtMs).atZone(zone).toOffsetDateTime().toString(),
                row.note.orEmpty(),
                Instant.ofEpochMilli(row.createdAtMs).toString(),
                Instant.ofEpochMilli(row.updatedAtMs).toString(),
            )
            writeBytes((values.joinToString(",") { escape(it) } + "\r\n").toByteArray(Charsets.UTF_8))
        }
        output.flush()
        return CsvExportResult(rows.size, bytes)
    }

    fun escape(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\r' || it == '\n' }) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
