package com.personalbookkeeping.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupArchive {
    const val MIME_TYPE = "application/vnd.personalbookkeeping.backup"
    const val MANIFEST_ENTRY = "manifest.json"
    const val DATA_ENTRY = "data.json"
    const val MAX_ARCHIVE_BYTES = 100L * 1024 * 1024
    const val MAX_DATA_BYTES = 100L * 1024 * 1024
    private const val MAX_MANIFEST_BYTES = 64L * 1024

    val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
        isLenient = false
        allowSpecialFloatingPointValues = false
        allowStructuredMapKeys = false
    }

    fun write(
        data: BackupDataV1,
        appVersionName: String,
        appVersionCode: Int,
        createdAt: String,
        output: OutputStream,
    ): BackupResult {
        BackupValidator.validateData(data)
        val dataBytes = json.encodeToString(data).toByteArray(Charsets.UTF_8)
        if (dataBytes.size.toLong() > MAX_DATA_BYTES) {
            throw BackupFormatException(BackupFormatException.Reason.TOO_LARGE)
        }
        val counts = data.counts()
        val manifest = BackupManifestV1(
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            databaseSchemaVersion = 1,
            createdAt = createdAt,
            counts = counts,
            dataFile = BackupDataFile(
                bytes = dataBytes.size.toLong(),
                sha256 = sha256(dataBytes),
            ),
        )
        val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)
        val countingOutput = CountingOutputStream(output, MAX_ARCHIVE_BYTES)
        val zip = ZipOutputStream(countingOutput, Charsets.UTF_8)
        zip.setLevel(6)
        zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
        zip.write(manifestBytes)
        zip.closeEntry()
        zip.putNextEntry(ZipEntry(DATA_ENTRY))
        zip.write(dataBytes)
        zip.closeEntry()
        zip.finish()
        zip.flush()
        return BackupResult(countingOutput.count, createdAt, counts)
    }

    fun writeToBytes(
        data: BackupDataV1,
        appVersionName: String = "test",
        appVersionCode: Int = 1,
        createdAt: String,
    ): ByteArray = ByteArrayOutputStream().also {
        write(data, appVersionName, appVersionCode, createdAt, it)
    }.toByteArray()

    fun read(input: InputStream): ValidatedBackup = read(
        input = input,
        maxArchiveBytes = MAX_ARCHIVE_BYTES,
        maxDataBytes = MAX_DATA_BYTES,
        maxManifestBytes = MAX_MANIFEST_BYTES,
    )

    internal fun read(
        input: InputStream,
        maxArchiveBytes: Long,
        maxDataBytes: Long,
        maxManifestBytes: Long,
    ): ValidatedBackup {
        val countingInput = CountingInputStream(input, maxArchiveBytes)
        val found = linkedMapOf<String, ByteArray>()
        try {
            ZipInputStream(countingInput, Charsets.UTF_8).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    if (entry.isDirectory || name !in setOf(MANIFEST_ENTRY, DATA_ENTRY) || found.containsKey(name)) {
                        throw BackupFormatException(BackupFormatException.Reason.CORRUPT_ARCHIVE)
                    }
                    val limit = if (name == MANIFEST_ENTRY) maxManifestBytes else maxDataBytes
                    found[name] = zip.readLimited(limit)
                    zip.closeEntry()
                }
            }
        } catch (error: BackupFormatException) {
            throw error
        } catch (_: Exception) {
            throw BackupFormatException(BackupFormatException.Reason.CORRUPT_ARCHIVE)
        }
        if (found.keys != setOf(MANIFEST_ENTRY, DATA_ENTRY)) {
            throw BackupFormatException(BackupFormatException.Reason.CORRUPT_ARCHIVE)
        }
        val manifestBytes = found.getValue(MANIFEST_ENTRY)
        val dataBytes = found.getValue(DATA_ENTRY)
        val manifest = decodeManifest(manifestBytes)
        if (manifest.format != BackupManifestV1.FORMAT || manifest.currencyCode != BackupManifestV1.CURRENCY) {
            throw BackupFormatException(BackupFormatException.Reason.UNSUPPORTED_FORMAT)
        }
        if (manifest.formatVersion != BackupManifestV1.VERSION || manifest.databaseSchemaVersion > 1) {
            throw BackupFormatException(BackupFormatException.Reason.UNSUPPORTED_VERSION)
        }
        if (manifest.dataFile.path != DATA_ENTRY ||
            manifest.dataFile.bytes != dataBytes.size.toLong() ||
            !MessageDigest.isEqual(
                manifest.dataFile.sha256.toByteArray(Charsets.US_ASCII),
                sha256(dataBytes).toByteArray(Charsets.US_ASCII),
            )
        ) {
            throw BackupFormatException(BackupFormatException.Reason.INTEGRITY_MISMATCH)
        }
        val data = try {
            json.decodeFromString<BackupDataV1>(dataBytes.toString(Charsets.UTF_8))
        } catch (_: SerializationException) {
            throw BackupFormatException(BackupFormatException.Reason.INVALID_DATA)
        } catch (_: IllegalArgumentException) {
            throw BackupFormatException(BackupFormatException.Reason.INVALID_DATA)
        }
        BackupValidator.validateManifest(manifest, data)
        BackupValidator.validateData(data)
        return ValidatedBackup(manifest, data)
    }

    fun read(bytes: ByteArray): ValidatedBackup = read(ByteArrayInputStream(bytes))

    private fun decodeManifest(bytes: ByteArray): BackupManifestV1 = try {
        json.decodeFromString<BackupManifestV1>(bytes.toString(Charsets.UTF_8))
    } catch (_: Exception) {
        throw BackupFormatException(BackupFormatException.Reason.CORRUPT_ARCHIVE)
    }

    internal fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun InputStream.readLimited(limit: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw BackupFormatException(BackupFormatException.Reason.TOO_LARGE)
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private class CountingInputStream(input: InputStream, private val limit: Long) : FilterInputStream(input) {
        private var count = 0L

        override fun read(): Int = super.read().also { if (it >= 0) add(1) }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            super.read(buffer, offset, length).also { if (it > 0) add(it.toLong()) }

        private fun add(value: Long) {
            count += value
            if (count > limit) throw BackupFormatException(BackupFormatException.Reason.TOO_LARGE)
        }
    }

    private class CountingOutputStream(
        private val output: OutputStream,
        private val limit: Long,
    ) : OutputStream() {
        var count: Long = 0
            private set

        override fun write(value: Int) {
            checkLimit(1)
            output.write(value)
            count++
        }

        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            checkLimit(length.toLong())
            output.write(buffer, offset, length)
            count += length
        }

        override fun flush() = output.flush()

        private fun checkLimit(additionalBytes: Long) {
            if (count + additionalBytes > limit) {
                throw BackupFormatException(BackupFormatException.Reason.TOO_LARGE)
            }
        }
    }
}

fun BackupDataV1.counts(): BackupCounts = BackupCounts(
    accounts = accounts.size,
    categories = categories.size,
    transactions = transactions.size,
    budgets = budgets.size,
)
