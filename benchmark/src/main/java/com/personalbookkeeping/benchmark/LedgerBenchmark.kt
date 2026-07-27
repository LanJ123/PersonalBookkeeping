package com.personalbookkeeping.benchmark

import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.util.Locale
import kotlin.math.ceil
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONArray
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class LedgerBenchmark {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val displaySize by lazy {
        val output = device.executeShellCommand("wm size")
        val match = DISPLAY_SIZE_REGEX.findAll(output).lastOrNull()
            ?: error("无法读取屏幕尺寸：$output")
        match.groupValues[1].toInt() to match.groupValues[2].toInt()
    }

    @Before
    fun seedTenThousandTransactions() {
        device.setOrientationNatural()
        val output = device.executeShellCommand(
            "content call --uri content://$TARGET_PACKAGE.benchmark-data --method seed",
        )
        assertTrue(output, output.contains("count=10000"))
    }

    @After
    fun restoreRotation() {
        device.unfreezeRotation()
    }

    @Test
    fun ledgerFirstContentWithTenThousandTransactions() {
        val samples = List(BenchmarkEnvironment.releaseGateIterations) {
            launchTarget()
            resetUiSignal(LEDGER_READY_SIGNAL)

            measureMs {
                clickBottomTab("流水")
                check(waitForUiSignal(LEDGER_READY_SIGNAL))
            }
        }

        report(
            name = "ledger-first-content-10k",
            samples = samples,
            thresholdMs = LEDGER_FIRST_CONTENT_THRESHOLD_MS,
        )
    }

    @Test
    fun ledgerScrollWithTenThousandTransactions() {
        val samples = List(BenchmarkEnvironment.releaseGateIterations) {
            launchTarget()
            resetUiSignal(LEDGER_READY_SIGNAL)
            clickBottomTab("流水")
            check(waitForUiSignal(LEDGER_READY_SIGNAL))

            measureMs {
                repeat(3) {
                    shellSwipe(
                        displaySize.first / 2,
                        displaySize.second * 3 / 4,
                        displaySize.first / 2,
                        displaySize.second / 4,
                    )
                }
                SystemClock.sleep(SCROLL_SETTLE_MS)
            }
        }

        report(name = "ledger-scroll-10k", samples = samples, thresholdMs = null)
    }

    @Test
    fun previousMonthSwitch() {
        val samples = List(BenchmarkEnvironment.releaseGateIterations) {
            launchTarget()
            clickBottomTab("统计")
            check(waitForUiSignal(MONTH_READY_SIGNAL))
            resetUiSignal(MONTH_READY_SIGNAL)

            measureMs {
                clickPreviousMonth()
                check(waitForUiSignal(MONTH_READY_SIGNAL))
            }
        }

        report(
            name = "previous-month-switch",
            samples = samples,
            thresholdMs = MONTH_SWITCH_THRESHOLD_MS,
        )
    }

    @Test
    fun saveFeedbackWithTenThousandTransactions() {
        val samples = List(BenchmarkEnvironment.releaseGateIterations) {
            launchTarget()
            resetUiSignal(EDITOR_READY_SIGNAL)
            clickAddTransaction()
            check(waitForUiSignal(EDITOR_READY_SIGNAL))
            clickAmountField()
            device.executeShellCommand("input text $SAVE_AMOUNT")
            device.executeShellCommand("input keyevent 4")

            resetUiSignal(SAVE_READY_SIGNAL)
            measureMs {
                clickSave()
                check(waitForUiSignal(SAVE_READY_SIGNAL))
            }
        }

        report(
            name = "save-feedback-10k",
            samples = samples,
            thresholdMs = SAVE_FEEDBACK_THRESHOLD_MS,
        )
    }

    private fun launchTarget() {
        device.executeShellCommand("am force-stop $TARGET_PACKAGE")
        val result = device.executeShellCommand(
            "am start -W -n $TARGET_PACKAGE/.MainActivity",
        )
        check(result.contains("Status: ok")) { result }
        check(waitForUiSignal(HOME_READY_SIGNAL))
    }

    private fun measureMs(block: () -> Unit): Double {
        val startedAt = SystemClock.elapsedRealtimeNanos()
        block()
        return (SystemClock.elapsedRealtimeNanos() - startedAt) / 1_000_000.0
    }

    private fun resetUiSignal(signal: String) {
        val output = device.executeShellCommand(
            "content call --uri content://$TARGET_PACKAGE.benchmark-data " +
                "--method reset-ui-signal --arg $signal",
        )
        check(output.contains("reset=true")) { output }
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

    private fun clickBottomTab(label: String) {
        val index = when (label) {
            "首页" -> 0
            "流水" -> 1
            "统计" -> 2
            "设置" -> 3
            else -> error("未知底部导航：$label")
        }
        shellTap(
            displaySize.first * (index * 2 + 1) / 8,
            displaySize.second * 13 / 14,
        )
    }

    private fun clickAddTransaction() {
        shellTap(displaySize.first * 7 / 8, displaySize.second * 6 / 7)
    }

    private fun clickPreviousMonth() {
        shellTap(displaySize.first * 3 / 20, displaySize.second * 2 / 7)
    }

    private fun clickAmountField() {
        shellTap(displaySize.first / 2, displaySize.second * 7 / 24)
    }

    private fun clickSave() {
        shellTap(displaySize.first / 2, displaySize.second * 4 / 5)
    }

    private fun shellTap(x: Int, y: Int) {
        device.executeShellCommand("input tap $x $y")
    }

    private fun shellSwipe(x1: Int, y1: Int, x2: Int, y2: Int) {
        device.executeShellCommand("input swipe $x1 $y1 $x2 $y2 $SWIPE_DURATION_MS")
    }

    private fun report(name: String, samples: List<Double>, thresholdMs: Double?) {
        require(samples.isNotEmpty())
        val sorted = samples.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2]
        }
        val p95Index = (ceil(sorted.size * 0.95).toInt() - 1).coerceIn(sorted.indices)
        val p95 = sorted[p95Index]
        val thresholdApplicable =
            !BenchmarkEnvironment.isEmulator &&
                samples.size >= BenchmarkEnvironment.PHYSICAL_DEVICE_ITERATIONS &&
                thresholdMs != null
        val passed = if (thresholdApplicable) p95 <= requireNotNull(thresholdMs) else null
        val payload = JSONObject().apply {
            put("metricVersion", 2)
            put("metric", name)
            put("measurement", "end-to-end-wall-clock")
            put("deviceRole", BenchmarkEnvironment.deviceRole)
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("androidRelease", Build.VERSION.RELEASE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("fingerprint", Build.FINGERPRINT)
            put("transactionCountAtSeed", 10_000)
            put("iterations", samples.size)
            put("medianMs", median.rounded())
            put("p95Ms", p95.rounded())
            put("thresholdMs", thresholdMs?.rounded() ?: JSONObject.NULL)
            put("thresholdApplicable", thresholdApplicable)
            put("passed", passed ?: JSONObject.NULL)
            put("samplesMs", JSONArray(samples.map { it.rounded() }))
        }.toString(2)
        val outputDirectory = File(
            "/sdcard/Android/media/${instrumentation.context.packageName}/additional_test_output",
        ).apply { mkdirs() }
        val output = File(outputDirectory, "i5-$name.json")
        output.writeText(payload)
        Log.i(LOG_TAG, payload)
        instrumentation.sendStatus(
            2,
            Bundle().apply {
                putString("additionalTestOutputFile_i5-$name.json", output.absolutePath)
            },
        )
        if (passed == false) {
            assertTrue(
                "$name P95 ${p95.format()} ms exceeds ${thresholdMs?.format()} ms",
                false,
            )
        }
    }

    private fun Double.format(): String = String.format(Locale.ROOT, "%.3f", this)
    private fun Double.rounded(): Double = format().toDouble()

    companion object {
        private const val UI_TIMEOUT_MS = 15_000L
        private const val LOG_TAG = "I5Benchmark"
        private const val SAVE_AMOUNT = "1.23"
        private const val HOME_READY_SIGNAL = "homeReady"
        private const val EDITOR_READY_SIGNAL = "editorReady"
        private const val LEDGER_READY_SIGNAL = "ledgerReady"
        private const val MONTH_READY_SIGNAL = "monthReady"
        private const val SAVE_READY_SIGNAL = "saveReady"
        private const val POLL_INTERVAL_MS = 25L
        private const val SCROLL_SETTLE_MS = 250L
        private const val SWIPE_DURATION_MS = 250L
        private const val LEDGER_FIRST_CONTENT_THRESHOLD_MS = 1_000.0
        private const val MONTH_SWITCH_THRESHOLD_MS = 1_000.0
        private const val SAVE_FEEDBACK_THRESHOLD_MS = 500.0
        private val DISPLAY_SIZE_REGEX = Regex("""(?:Physical|Override) size:\s*(\d+)x(\d+)""")
    }
}
