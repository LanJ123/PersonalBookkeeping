package com.personalbookkeeping.app

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.personalbookkeeping.security.AppLockCoordinator
import com.personalbookkeeping.domain.model.ThemeMode
import com.personalbookkeeping.ui.navigation.BookkeepingApp
import com.personalbookkeeping.ui.privacy.AmountPrivacyProvider
import com.personalbookkeeping.ui.security.LockedScreen
import com.personalbookkeeping.ui.theme.BookkeepingTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val container get() = (application as PersonalBookkeepingApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val lockState = container.appLockCoordinator.state.collectAsStateWithLifecycle().value
            val amountsHidden = container.portabilityService.hideAmounts
                .collectAsStateWithLifecycle(initialValue = false).value
            val themeMode = container.portabilityService.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM).value
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            ReportDrawnWhen { lockState.ready }
            LaunchedEffect(lockState.enabled) {
                if (lockState.enabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            BookkeepingTheme(darkTheme = darkTheme) {
                when {
                    !lockState.ready -> LockedScreen(ready = false, message = null, onUnlock = {})
                    lockState.locked -> LockedScreen(
                        ready = true,
                        message = lockState.message,
                        onUnlock = ::authenticateForUnlock,
                    )
                    else -> AmountPrivacyProvider(amountsHidden) {
                        BookkeepingApp(
                            container = container,
                            appLockEnabled = lockState.enabled,
                            appLockMessage = lockState.message,
                            amountsHidden = amountsHidden,
                            themeMode = themeMode,
                            onAppLockChanged = ::requestAppLockChange,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.appLockCoordinator.onForeground()
    }

    override fun onStop() {
        container.appLockCoordinator.onBackground()
        super.onStop()
    }

    private fun requestAppLockChange(enabled: Boolean) {
        if (!enabled) {
            lifecycleScope.launch { container.appLockCoordinator.setEnabled(false) }
            return
        }
        authenticate(
            title = "启用应用锁",
            onSuccess = { lifecycleScope.launch { container.appLockCoordinator.setEnabled(true) } },
        )
    }

    private fun authenticateForUnlock() {
        authenticate(
            title = "解锁个人记账",
            onSuccess = container.appLockCoordinator::unlock,
        )
    }

    private fun authenticate(title: String, onSuccess: () -> Unit) {
        val coordinator = container.appLockCoordinator
        if (!hasSystemAuthentication()) {
            coordinator.authenticationUnavailable()
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    coordinator.authenticationFailed()
                }

                override fun onAuthenticationFailed() {
                    coordinator.authenticationFailed()
                }
            },
        )
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("使用设备屏幕锁或生物识别")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        prompt.authenticate(builder.build())
    }

    private fun hasSystemAuthentication(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return BiometricManager.from(this).canAuthenticate(ALLOWED_AUTHENTICATORS) ==
                BiometricManager.BIOMETRIC_SUCCESS
        }
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguard.isDeviceSecure ||
            BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    companion object {
        private const val ALLOWED_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
