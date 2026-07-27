package com.personalbookkeeping.benchmark

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.util.Locale
import kotlin.math.ceil
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Vendor-compatible cold-start TTFD gate.
 *
 * vivo Android 16 delivers ProfileInstaller broadcasts but returns result=0,
 * which makes Macrobenchmark fail before sampling. This measures force-stop
 * through the first globally positioned Home content instead.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun coldStartup() {
        val samples = List(BenchmarkEnvironment.startupIterations) {
            device.executeShellCommand("am force-stop $TARGET_PACKAGE")
            val startedAt = SystemClock.elapsedRealtimeNanos()
            val result = device.executeShellCommand(
                "am start -W -n $TARGET_PACKAGE/.MainActivity",
            )
            check(result.contains("Status: ok")) { result }
            check(waitForUiSignal(HOME_READY_SIGNAL)) {
                "首页未在 ${UI_TIMEOUT_MS}ms 内报告布局完成"
            }
            (SystemClock.elapsedRealtimeNanos() - startedAt) / 1_000_000.0
        }
        report(samples)
    }

    private fun waitForUiSignal(signal: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            val output = device.executeShellCommand(
                "content call --uri content://$TARGET_PACKAGE.benchmark-data " +
                    "--method ui-signal-status --arg $signal",
            )
            if (output.contains("marked=true")) return true
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun report(samples: List<Double>) {
        val sorted = samples.sorted()
        val p95Index = (ceil(sorted.size * 0.95).toInt() - 1).coerceIn(sorted.indices)
        val p95 = sorted[p95Index]
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2]
        }
        val thresholdApplicable =
            !BenchmarkEnvironment.isEmulator &&
                samples.size >= BenchmarkEnvironment.PHYSICAL_DEVICE_ITERATIONS
        val passed = if (thresholdApplicable) p95 <= THRESHOLD_MS else null
        val payload = JSONObject().apply {
            put("metricVersion", 2)
            put("metric", "cold-start-ttfd")
            put("measurement", "force-stop-to-home-globally-positioned-wall-clock")
            put("deviceRole", BenchmarkEnvironment.deviceRole)
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("androidRelease", Build.VERSION.RELEASE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("fingerprint", Build.FINGERPRINT)
            put("iterations", samples.size)
            put("medianMs", median.rounded())
            put("p95Ms", p95.rounded())
            put("thresholdMs", THRESHOLD_MS)
            put("thresholdApplicable", thresholdApplicable)
            put("passed", passed ?: JSONObject.NULL)
            put("samplesMs", JSONArray(samples.map { it.rounded() }))
        }.toString(2)
        val outputDirectory = File(
            "/sdcard/Android/media/${instrumentation.context.packageName}/additional_test_output",
        ).apply { mkdirs() }
        val output = File(outputDirectory, OUTPUT_FILE)
        output.writeText(payload)
        Log.i(LOG_TAG, payload)
        instrumentation.sendStatus(
            2,
            Bundle().apply {
                putString("additionalTestOutputFile_$OUTPUT_FILE", output.absolutePath)
            },
        )
        if (passed == false) {
            assertTrue("cold-start-ttfd P95 ${p95.rounded()}ms exceeds ${THRESHOLD_MS}ms", false)
        }
    }

    private fun Double.rounded(): Double =
        String.format(Locale.ROOT, "%.3f", this).toDouble()

    private companion object {
        const val HOME_READY_SIGNAL = "homeReady"
        const val OUTPUT_FILE = "i5-cold-start-ttfd.json"
        const val LOG_TAG = "I5Benchmark"
        const val UI_TIMEOUT_MS = 15_000L
        const val POLL_INTERVAL_MS = 25L
        const val THRESHOLD_MS = 2_000.0
    }
}
