package com.personalbookkeeping.benchmark

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.io.File
import java.util.Locale
import java.util.regex.Pattern
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerBenchmark {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    @Before
    fun seedTenThousandTransactions() {
        val output = device.executeShellCommand(
            "content call --uri content://$TARGET_PACKAGE.benchmark-data --method seed",
        )
        assertTrue(output, output.contains("count=10000"))
    }

    @Test
    fun ledgerScrollWithTenThousandTransactions() {
        val samples = List(ITERATIONS) {
            launchTarget()
            device.bottomTab("流水").click()
            check(device.wait(Until.hasObject(By.text("搜索备注")), UI_TIMEOUT_MS))
            check(device.wait(Until.hasObject(By.textContains("基准备注")), UI_TIMEOUT_MS))

            measureMs {
                repeat(3) {
                    device.swipe(
                        device.displayWidth / 2,
                        device.displayHeight * 3 / 4,
                        device.displayWidth / 2,
                        device.displayHeight / 4,
                        60,
                    )
                }
                device.waitForIdle()
            }
        }

        report("ledger-scroll-10k", samples)
    }

    @Test
    fun previousMonthSwitch() {
        val periodPattern = Pattern.compile("\\d{4}年\\d{1,2}月")
        val samples = List(ITERATIONS) {
            launchTarget()
            device.bottomTab("首页").click()
            val period = device.wait(Until.findObject(By.text(periodPattern)), UI_TIMEOUT_MS)
                ?: error("未找到当前月份")
            val periodText = period.text
            val previousMonth = device.wait(Until.findObject(By.text("上月")), UI_TIMEOUT_MS)
                ?: error("未找到上月操作")

            measureMs {
                previousMonth.click()
                check(device.wait(Until.gone(By.text(periodText)), UI_TIMEOUT_MS))
                device.waitForIdle()
            }
        }

        report("previous-month-switch", samples)
    }

    private fun launchTarget() {
        device.executeShellCommand("am force-stop $TARGET_PACKAGE")
        val result = device.executeShellCommand(
            "am start -W -n $TARGET_PACKAGE/.MainActivity",
        )
        check(result.contains("Status: ok")) { result }
        check(device.wait(Until.hasObject(By.text("＋记一笔")), UI_TIMEOUT_MS))
    }

    private fun measureMs(block: () -> Unit): Double {
        val startedAt = SystemClock.elapsedRealtimeNanos()
        block()
        return (SystemClock.elapsedRealtimeNanos() - startedAt) / 1_000_000.0
    }

    private fun report(name: String, samples: List<Double>) {
        val sorted = samples.sorted()
        val median = sorted[sorted.size / 2]
        val payload = buildString {
            appendLine("{")
            appendLine("""  "metric": "$name",""")
            appendLine("""  "deviceRole": "emulator-trend-only",""")
            appendLine("""  "transactionCount": 10000,""")
            appendLine("""  "iterations": ${samples.size},""")
            appendLine("""  "medianMs": ${median.format()},""")
            appendLine("""  "samplesMs": [${samples.joinToString { it.format() }}]""")
            appendLine("}")
        }
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
    }

    private fun Double.format(): String = String.format(Locale.ROOT, "%.3f", this)

    companion object {
        private const val ITERATIONS = 5
        private const val UI_TIMEOUT_MS = 15_000L
        private const val LOG_TAG = "I5Benchmark"
    }
}

private fun UiDevice.bottomTab(label: String): UiObject2 {
    check(wait(Until.hasObject(By.text(label)), 15_000))
    return findObjects(By.text(label))
        .maxByOrNull { it.visibleBounds.centerY() }
        ?: error("未找到底部导航：$label")
}
