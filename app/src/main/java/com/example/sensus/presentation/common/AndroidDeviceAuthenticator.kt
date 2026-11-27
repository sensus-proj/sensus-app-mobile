package com.example.sensus.presentation.common

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.sensus.domain.security.DeviceAuthOutcome
import com.example.sensus.domain.security.DeviceAuthPrompt
import com.example.sensus.domain.security.DeviceAuthenticator
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

class AndroidDeviceAuthenticator(
    private val activity: FragmentActivity
) : DeviceAuthenticator {

    override suspend fun authenticate(prompt: DeviceAuthPrompt): DeviceAuthOutcome {
        val availability = BiometricManager.from(activity).canAuthenticate(ALLOWED_AUTHENTICATORS)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            return DeviceAuthOutcome.Unavailable
        }
        return suspendCancellableCoroutine { continuation ->
            val resumed = AtomicBoolean(false)
            fun finish(outcome: DeviceAuthOutcome) {
                if (resumed.compareAndSet(false, true) && continuation.isActive) {
                    continuation.resume(outcome)
                }
            }

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    finish(DeviceAuthOutcome.Authenticated)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val isCancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    val outcome = when {
                        isCancelled -> DeviceAuthOutcome.Cancelled
                        errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                            errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ||
                            errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                            errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE -> DeviceAuthOutcome.Unavailable
                        else -> DeviceAuthOutcome.Error(errString.toString())
                    }
                    finish(outcome)
                }
            }

            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(prompt.title)
                .setSubtitle(prompt.subtitle)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()

            continuation.invokeOnCancellation {
                executor.execute { biometricPrompt.cancelAuthentication() }
            }
            executor.execute { biometricPrompt.authenticate(promptInfo) }
        }
    }
}

@Composable
fun rememberDeviceAuthenticator(): DeviceAuthenticator? {
    val activity = LocalActivity.current as? FragmentActivity
    return remember(activity) { activity?.let(::AndroidDeviceAuthenticator) }
}
