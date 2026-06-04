package dev.favourdevlabs.cleanthes.autofill

import android.content.Intent
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.WindowManager
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager

class AutofillAuthActivity:
AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "pkg"
        const val EXTRA_WEB_DOMAIN   = "domain"
        const val EXTRA_USERNAME_ID  = "uid"
        const val EXTRA_PASSWORD_ID  = "pid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autofill_auth)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        if (!SessionManager.isUnlocked()) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        prompt()
    }

    private fun prompt() {
        BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    deliver()
                }
                override fun onAuthenticationFailed() = Unit
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Cleanthes").setSubtitle("Authenticate to fill").setNegativeButtonText("Cancel").build())

    }

    private fun deliver() {
        val secretKey = SessionManager.getSessionKey() ?: run {
            setResult(RESULT_CANCELED); finish(); return
        }

        val usernameId  = intent.getParcelableExtra<AutofillId>(EXTRA_USERNAME_ID)
        val passwordId  = intent.getParcelableExtra<AutofillId>(EXTRA_PASSWORD_ID)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val webDomain   = intent.getStringExtra(EXTRA_WEB_DOMAIN)
        val lookupKey   = webDomain ?: packageName

        try {
            val all     = VaultRepository.getInstance(this).getAllEntries(secretKey)
            val matches = filter(all, lookupKey)

            if (matches.isEmpty()) {
                setResult(RESULT_CANCELED); finish(); return
            }

            val response = FillResponse.Builder()
            for (entry in matches) {
                val view = RemoteViews(packageName, R.layout.autofill_item).apply {
                    setTextViewText(R.id.autofill_label, entry.username)
                }
                // After getAllEntries(), encryptedPassword holds the decrypted plain password
                response.addDataset(
                    Dataset.Builder(view)
                        .setValue(usernameId!!, AutofillValue.forText(entry.username), view)
                        .setValue(passwordId!!, AutofillValue.forText(entry.encryptedPassword), view)
                        .build()
                )
            }

            SessionManager.refreshSession()

            val out = Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response.build())
            setResult(RESULT_OK, out)

        } catch (_: Exception) {
            setResult(RESULT_CANCELED)
        }

        finish()
    }

    private fun filter(entries: List<VaultEntry>, key: String?): List<VaultEntry> {
        if (key.isNullOrEmpty()) return emptyList()
        val lower = key.lowercase()
        return entries.filter { e ->
            val website = e.website?.lowercase() ?: ""
            val title   = e.title.lowercase()
            website.contains(lower) || lower.contains(website) || title.contains(lower)
        }
    }
}
