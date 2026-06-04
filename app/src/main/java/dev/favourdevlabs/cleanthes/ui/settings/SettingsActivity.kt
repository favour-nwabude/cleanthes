package dev.favourdevlabs.cleanthes.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.autofill.AutofillManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity

class SettingsActivity : AuthenticatedActivity() {

    companion object {
        private const val PREFS_NAME = "cleanthes_prefs"
        const val KEY_AUTO_LOCK = "auto_lock_minutes"
        const val KEY_CLIPBOARD = "clipboard_clear_seconds"

        private val LOCK_VALUES = intArrayOf(1, 5, 15, -1)
        private val LOCK_LABELS = arrayOf("1 min", "5 min", "15 min", "Never")

        private val CLIP_VALUES = intArrayOf(30, 60, -1)
        private val CLIP_LABELS = arrayOf("30s", "60s", "Off")
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var autofillManager: AutofillManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        autofillManager = getSystemService(AutofillManager::class.java)

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }

        bindAutoLock()
        bindClipboard()
        bindAutofill()
        bindVersion()
        bindLicenses()
    }

    override fun onResume() {
        super.onResume()
        bindAutofill()
    }

    private fun bindAutoLock() {
        val valueView = findViewById<TextView>(R.id.value_auto_lock)
        valueView.text = labelForLock(prefs.getInt(KEY_AUTO_LOCK, 5))

        findViewById<android.view.View>(R.id.row_auto_lock).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Auto-lock after")
                .setItems(LOCK_LABELS) { _, which ->
                    prefs.edit().putInt(KEY_AUTO_LOCK, LOCK_VALUES[which]).apply()
                    valueView.text = LOCK_LABELS[which]
                }
                .show()
        }
    }

    private fun bindClipboard() {
        val valueView = findViewById<TextView>(R.id.value_clipboard)
        valueView.text = labelForClip(prefs.getInt(KEY_CLIPBOARD, 30))

        findViewById<android.view.View>(R.id.row_clipboard).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear clipboard after")
                .setItems(CLIP_LABELS) { _, which ->
                    prefs.edit().putInt(KEY_CLIPBOARD, CLIP_VALUES[which]).apply()
                    valueView.text = CLIP_LABELS[which]
                }
                .show()
        }
    }

    private fun bindAutofill() {
        val statusView = findViewById<TextView>(R.id.autofill_status_text)
        val active = autofillManager.hasEnabledAutofillServices()

        if (active) {
            statusView.text = "Active ✓"
            statusView.setTextColor(getColor(R.color.cleanthes_success))
            findViewById<android.view.View>(R.id.row_autofill).setOnClickListener(null)
        } else {
            statusView.text = "Enable ›"
            statusView.setTextColor(getColor(R.color.cleanthes_accent))
            findViewById<android.view.View>(R.id.row_autofill).setOnClickListener {
                startActivity(Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }
    }

    private fun bindVersion() {
        val versionView = findViewById<TextView>(R.id.value_version)
        versionView.text = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun bindLicenses() {
        findViewById<android.view.View>(R.id.row_licenses).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Open-source libraries")
                .setMessage(
                    "ZXing Android Embedded\n" +
                    "Apache 2.0 License\n\n" +
                    "AndroidX Biometric\n" +
                    "Apache 2.0 License\n\n" +
                    "AndroidX Security Crypto\n" +
                    "Apache 2.0 License\n\n" +
                    "Google Material Components\n" +
                    "Apache 2.0 License"
                )
                .setPositiveButton("Close", null)
                .show()
        }
    }

    private fun labelForLock(minutes: Int) = if (minutes == -1) "Never" else "$minutes min"
    private fun labelForClip(seconds: Int) = if (seconds == -1) "Off" else "${seconds}s"
}
