package com.example.loveapp.utils

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Wraps AndroidX BiometricPrompt in a clean coroutine-friendly API.
 *
 * Usage (from a Fragment/Activity):
 *   val result = biometricManager.authenticate(requireActivity())
 *   if (result == BiometricResult.Success) { … }
 */
@Singleton
class BiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    sealed class BiometricResult {
        object Success : BiometricResult()
        data class Error(val code: Int, val message: String) : BiometricResult()
        object Cancelled : BiometricResult()
        object NotEnrolled : BiometricResult()
        object HardwareUnavailable : BiometricResult()
    }

    /** Returns true if biometric authentication is available on this device. */
    fun isAvailable(): Boolean {
        val mgr = AndroidBiometricManager.from(context)
        val result = mgr.canAuthenticate(
            AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
                    AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == AndroidBiometricManager.BIOMETRIC_SUCCESS
    }

    /** Returns true if any biometrics are enrolled. */
    fun isEnrolled(): Boolean {
        val mgr = AndroidBiometricManager.from(context)
        return mgr.canAuthenticate(
            AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == AndroidBiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the biometric prompt and suspends until the user authenticates
     * or cancels. Must be called from a FragmentActivity context.
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Login",
        subtitle: String = "Use your fingerprint or face to continue",
        negativeText: String = "Use Password"
    ): BiometricResult = suspendCancellableCoroutine { cont ->
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cont.isActive) cont.resume(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (cont.isActive) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED -> cont.resume(BiometricResult.Cancelled)
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> cont.resume(BiometricResult.HardwareUnavailable)
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> cont.resume(BiometricResult.NotEnrolled)
                        else -> cont.resume(BiometricResult.Error(errorCode, errString.toString()))
                    }
                }
            }

            override fun onAuthenticationFailed() {
                // single attempt failed — don't close prompt, let system retry
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
                    AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG
        }

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .apply {
                // DEVICE_CREDENTIAL alone replaces the negative button on API 30+
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    setNegativeButtonText(negativeText)
                }
            }
            .build()

        prompt.authenticate(info)

        cont.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}
