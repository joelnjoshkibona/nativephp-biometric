package com.blutrixx.plugins.biometric

import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.nativephp.mobile.bridge.BridgeFunction
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The phone's own fingerprint (or face), e.g. for an app lock. Strong or weak biometrics only: the phone's screen PIN is
 * not offered here, so an app using it keeps its own PIN or password as the second way in. Every failure answers "not
 * available" or an error, never an exception, so an app can simply fall back to its own PIN.
 */
object BiometricFunctions {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    private const val WAIT_SECONDS = 60L

    class Available(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> = try {
            mapOf("available" to (BiometricManager.from(activity).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS))
        } catch (e: Exception) {
            mapOf("available" to false)
        }
    }

    class Authenticate(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>): Map<String, Any> {
            // the bridge call waits for the person; it must not be the UI thread, which has to draw the prompt
            if (Looper.myLooper() == Looper.getMainLooper()) return mapOf("success" to false, "error" to "busy")
            if (BiometricManager.from(activity).canAuthenticate(AUTH) != BiometricManager.BIOMETRIC_SUCCESS) return mapOf("success" to false, "error" to "unavailable")

            val title = (parameters["title"] as? String).orEmpty().ifEmpty { "Unlock" }
            val subtitle = (parameters["subtitle"] as? String).orEmpty()
            val cancel = (parameters["cancel"] as? String).orEmpty().ifEmpty { "Use PIN" }
            val done = CountDownLatch(1)
            val answer = AtomicReference<Map<String, Any>>(mapOf("success" to false, "error" to "timeout"))

            activity.runOnUiThread {
                try {
                    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            answer.set(mapOf("success" to true))
                            done.countDown()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED
                            answer.set(if (cancelled) mapOf("success" to false, "cancelled" to true) else mapOf("success" to false, "error" to errString.toString()))
                            done.countDown()
                        }
                    })
                    prompt.authenticate(
                        BiometricPrompt.PromptInfo.Builder().setTitle(title).setSubtitle(subtitle).setNegativeButtonText(cancel).setAllowedAuthenticators(AUTH).build(),
                    )
                } catch (e: Exception) {
                    answer.set(mapOf("success" to false, "error" to (e.message ?: "failed")))
                    done.countDown()
                }
            }
            done.await(WAIT_SECONDS, TimeUnit.SECONDS)
            return answer.get()
        }
    }
}
