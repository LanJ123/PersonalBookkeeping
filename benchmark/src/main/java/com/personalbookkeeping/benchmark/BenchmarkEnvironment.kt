package com.personalbookkeeping.benchmark

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry

object BenchmarkEnvironment {
    private val iterationOverride: Int? by lazy {
        InstrumentationRegistry.getArguments()
            .getString("benchmarkIterations")
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
    }

    val isEmulator: Boolean by lazy {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator", ignoreCase = true) ||
            model.contains("Emulator", ignoreCase = true) ||
            model.contains("sdk_gphone", ignoreCase = true) ||
            hardware.contains("goldfish", ignoreCase = true) ||
            hardware.contains("ranchu", ignoreCase = true) ||
            product.contains("sdk_gphone", ignoreCase = true)
    }

    val deviceRole: String
        get() = if (isEmulator) "emulator-trend-only" else "target-physical-device"

    val releaseGateIterations: Int
        get() = iterationOverride
            ?: if (isEmulator) EMULATOR_ITERATIONS else PHYSICAL_DEVICE_ITERATIONS

    val startupIterations: Int
        get() = iterationOverride
            ?: if (isEmulator) STARTUP_EMULATOR_ITERATIONS else PHYSICAL_DEVICE_ITERATIONS

    const val EMULATOR_ITERATIONS = 5
    const val STARTUP_EMULATOR_ITERATIONS = 10
    const val PHYSICAL_DEVICE_ITERATIONS = 30
}
