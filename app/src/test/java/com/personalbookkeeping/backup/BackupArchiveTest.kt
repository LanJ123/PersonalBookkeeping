package com.personalbookkeeping.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupArchiveTest {
    private val createdAt = "2026-07-23T08:00:00Z"

    @Test
    fun goldenData_roundTripsThroughPbk() {
        val source = goldenData()
        val archive = BackupArchive.writeToBytes(source, createdAt = createdAt)

        val restored = BackupArchive.read(archive)

        assertEquals(source, restored.data)
        assertEquals(source.counts(), restored.manifest.counts)
        assertEquals(createdAt, restored.manifest.createdAt)
        assertEquals(64, restored.manifest.dataFile.sha256.length)
    }

    @Test
    fun integrityMismatch_isRejectedBeforeDecode() {
        val source = goldenData()
        val dataBytes = BackupArchive.json.encodeToString(source).toByteArray()
        val manifest = manifest(source, dataBytes).copy(
            dataFile = BackupDataFile(bytes = dataBytes.size.toLong(), sha256 = "0".repeat(64)),
        )
        val error = assertThrows(BackupFormatException::class.java) {
            BackupArchive.read(rawArchive(BackupArchive.json.encodeToString(manifest).toByteArray(), dataBytes))
        }
        assertEquals(BackupFormatException.Reason.INTEGRITY_MISMATCH, error.reason)
    }

    @Test
    fun unknownField_isRejectedByStrictJson() {
        val source = goldenData()
        val original = BackupArchive.json.encodeToString(source)
        val dataBytes = (original.dropLast(1) + ",\"unexpected\":true}").toByteArray()
        val manifest = manifest(source, dataBytes)
        val error = assertThrows(BackupFormatException::class.java) {
            BackupArchive.read(rawArchive(BackupArchive.json.encodeToString(manifest).toByteArray(), dataBytes))
        }
        assertEquals(BackupFormatException.Reason.INVALID_DATA, error.reason)
    }

    @Test
    fun futureFormatVersion_isRejected() {
        val source = goldenData()
        val dataBytes = BackupArchive.json.encodeToString(source).toByteArray()
        val manifest = manifest(source, dataBytes).copy(formatVersion = 2)
        val error = assertThrows(BackupFormatException::class.java) {
            BackupArchive.read(rawArchive(BackupArchive.json.encodeToString(manifest).toByteArray(), dataBytes))
        }
        assertEquals(BackupFormatException.Reason.UNSUPPORTED_VERSION, error.reason)
    }

    @Test
    fun pathTraversalEntry_isRejected() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("../manifest.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
        }
        val error = assertThrows(BackupFormatException::class.java) { BackupArchive.read(output.toByteArray()) }
        assertEquals(BackupFormatException.Reason.CORRUPT_ARCHIVE, error.reason)
    }

    @Test
    fun directoryAndMissingRequiredEntry_areRejected() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("nested/"))
            zip.closeEntry()
        }
        val directoryError = assertThrows(BackupFormatException::class.java) {
            BackupArchive.read(output.toByteArray())
        }
        assertEquals(BackupFormatException.Reason.CORRUPT_ARCHIVE, directoryError.reason)

        val missingError = assertThrows(BackupFormatException::class.java) {
            BackupArchive.read(rawArchiveEntry(BackupArchive.MANIFEST_ENTRY, "{}".toByteArray()))
        }
        assertEquals(BackupFormatException.Reason.CORRUPT_ARCHIVE, missingError.reason)
    }

    @Test
    fun duplicateEntry_isRejected() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            listOf(BackupArchive.MANIFEST_ENTRY, "manifesz.json").forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }
        }
        val forgedDuplicate = output.toByteArray().replaceAscii("manifesz.json", BackupArchive.MANIFEST_ENTRY)
        val error = assertThrows(BackupFormatException::class.java) {
            BackupArchive.read(forgedDuplicate)
        }
        assertEquals(BackupFormatException.Reason.CORRUPT_ARCHIVE, error.reason)
    }

    @Test
    fun extraEntry_isRejected() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            listOf(BackupArchive.MANIFEST_ENTRY, BackupArchive.DATA_ENTRY, "extra.txt").forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }
        }
        val error = assertThrows(BackupFormatException::class.java) { BackupArchive.read(output.toByteArray()) }
        assertEquals(BackupFormatException.Reason.CORRUPT_ARCHIVE, error.reason)
    }

    @Test
    fun compressedAndExpandedSizeLimits_areEnforced() {
        val archive = BackupArchive.writeToBytes(goldenData(), createdAt = createdAt)

        val archiveError = assertThrows(BackupFormatException::class.java) {
            BackupArchive.read(
                input = ByteArrayInputStream(archive),
                maxArchiveBytes = 32,
                maxDataBytes = BackupArchive.MAX_DATA_BYTES,
                maxManifestBytes = 64 * 1024,
            )
        }
        assertEquals(BackupFormatException.Reason.TOO_LARGE, archiveError.reason)

        val expandedError = assertThrows(BackupFormatException::class.java) {
            BackupArchive.read(
                input = ByteArrayInputStream(archive),
                maxArchiveBytes = BackupArchive.MAX_ARCHIVE_BYTES,
                maxDataBytes = 32,
                maxManifestBytes = 64 * 1024,
            )
        }
        assertEquals(BackupFormatException.Reason.TOO_LARGE, expandedError.reason)
    }

    @Test
    fun accountCountAboveSchemaLimit_isRejected() {
        val base = goldenData()
        val oversized = base.copy(
            accounts = List(10_001) { index ->
                base.accounts.single().copy(
                    id = "account-$index",
                    name = "账户$index",
                    status = "INACTIVE",
                )
            },
        )
        val error = assertThrows(BackupFormatException::class.java) {
            BackupArchive.writeToBytes(oversized, createdAt = createdAt)
        }
        assertEquals(BackupFormatException.Reason.INVALID_DATA, error.reason)
    }

    @Test
    fun danglingReference_isRejected() {
        val source = goldenData().copy(
            transactions = goldenData().transactions.map { it.copy(accountId = "missing-account") },
        )
        val error = assertThrows(BackupFormatException::class.java) {
            BackupArchive.writeToBytes(source, createdAt = createdAt)
        }
        assertEquals(BackupFormatException.Reason.INVALID_DATA, error.reason)
    }

    private fun goldenData(): BackupDataV1 {
        val resource = requireNotNull(javaClass.getResourceAsStream("/fixtures/backup-v1-golden-data.json"))
        return resource.use { BackupArchive.json.decodeFromString(it.readBytes().toString(Charsets.UTF_8)) }
    }

    private fun manifest(data: BackupDataV1, dataBytes: ByteArray) = BackupManifestV1(
        appVersionName = "test",
        appVersionCode = 1,
        databaseSchemaVersion = 1,
        createdAt = createdAt,
        counts = data.counts(),
        dataFile = BackupDataFile(
            bytes = dataBytes.size.toLong(),
            sha256 = BackupArchive.sha256(dataBytes),
        ),
    )

    private fun rawArchive(manifest: ByteArray, data: ByteArray): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(BackupArchive.MANIFEST_ENTRY))
            zip.write(manifest)
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(BackupArchive.DATA_ENTRY))
            zip.write(data)
            zip.closeEntry()
        }
    }.toByteArray()

    private fun rawArchiveEntry(name: String, bytes: ByteArray): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }.toByteArray()

    private fun ByteArray.replaceAscii(source: String, replacement: String): ByteArray {
        require(source.length == replacement.length)
        val sourceBytes = source.toByteArray(Charsets.US_ASCII)
        val replacementBytes = replacement.toByteArray(Charsets.US_ASCII)
        val result = copyOf()
        for (index in 0..result.size - sourceBytes.size) {
            if (result.copyOfRange(index, index + sourceBytes.size).contentEquals(sourceBytes)) {
                replacementBytes.copyInto(result, destinationOffset = index)
            }
        }
        return result
    }
}
