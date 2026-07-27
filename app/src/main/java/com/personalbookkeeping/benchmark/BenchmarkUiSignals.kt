package com.personalbookkeeping.benchmark

import android.content.Context
import android.os.Process
import com.personalbookkeeping.BuildConfig
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-process readiness signals consumed only by the benchmark-only provider.
 * Release builds do not expose a provider that can read these values.
 */
object BenchmarkUiSignals {
    private val signals = ConcurrentHashMap<String, AtomicBoolean>()
    private val generations = ConcurrentHashMap<String, AtomicInteger>()
    @Volatile
    private var signalDirectory: File? = null

    fun configure(context: Context) {
        if (BuildConfig.BUILD_TYPE != "benchmark") return
        signalDirectory = context.externalMediaDirs
            .firstOrNull()
            ?.resolve(SIGNAL_DIRECTORY)
            ?.apply { mkdirs() }
    }

    fun mark(name: String) {
        requireValidName(name)
        signals.getOrPut(name) { AtomicBoolean() }.set(true)
        writeState(name = name, marked = true, generation = generation(name))
    }

    fun reset(name: String): Int {
        requireValidName(name)
        signals.getOrPut(name) { AtomicBoolean() }.set(false)
        val generation = generations.getOrPut(name) { AtomicInteger() }.incrementAndGet()
        writeState(name = name, marked = false, generation = generation)
        return generation
    }

    fun isMarked(name: String): Boolean = signals[name]?.get() == true
    fun generation(name: String): Int = generations[name]?.get() ?: 0
    fun signalPath(name: String): String? {
        requireValidName(name)
        return signalDirectory?.resolve("$name.signal")?.absolutePath
    }

    private fun writeState(name: String, marked: Boolean, generation: Int) {
        val file = signalDirectory?.resolve("$name.signal") ?: return
        runCatching {
            file.writeText(
                "pid=${Process.myPid()} signal=$name generation=$generation marked=$marked",
            )
        }
    }

    private fun requireValidName(name: String) {
        require(SIGNAL_NAME.matches(name)) { "invalid benchmark signal name" }
    }

    const val HOME_READY = "homeReady"
    const val EDITOR_READY = "editorReady"
    const val LEDGER_READY = "ledgerReady"
    const val MONTH_READY = "monthReady"
    const val SAVE_READY = "saveReady"
    private const val SIGNAL_DIRECTORY = "benchmark-signals"
    private val SIGNAL_NAME = Regex("[A-Za-z][A-Za-z0-9]*")
}
