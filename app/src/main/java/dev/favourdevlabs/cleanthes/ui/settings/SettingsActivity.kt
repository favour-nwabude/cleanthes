package dev.favourdevlabs.cleanthes.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.theme.*

// File-level — accessible to private composables in this file
private val LOCK_VALUES = intArrayOf(1, 5, 15, -1)
private val LOCK_LABELS = arrayOf("1 min", "5 min", "15 min", "Never")
private val CLIP_VALUES = intArrayOf(30, 60, -1)
private val CLIP_LABELS = arrayOf("30s", "60s", "Off")

@AndroidEntryPoint
class SettingsActivity : AuthenticatedActivity() {

    companion object {
        private const val PREFS_NAME = "cleanthes_prefs"
        const val KEY_AUTO_LOCK      = "auto_lock_minutes"
        const val KEY_CLIPBOARD      = "clipboard_clear_seconds"
    }

    private lateinit var prefs:          SharedPreferences
    private lateinit var autofillManager: AutofillManager

    // Compose-observable state — mutated by callbacks and onResume
    private var autoLockMinutes  by mutableStateOf(5)
    private var clipboardSeconds by mutableStateOf(30)
    private var autofillActive   by mutableStateOf(false)

    private val versionName: String by lazy {
        try { packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0" }
        catch (_: Exception) { "1.0.0" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs           = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        autofillManager = getSystemService(AutofillManager::class.java)

        autoLockMinutes  = prefs.getInt(KEY_AUTO_LOCK,  5)
        clipboardSeconds = prefs.getInt(KEY_CLIPBOARD, 30)

        setContent {
            CleanthesTheme {
                SettingsScreen(
                    autoLockMinutes   = autoLockMinutes,
                    clipboardSeconds  = clipboardSeconds,
                    autofillActive    = autofillActive,
                    versionName       = versionName,
                    onAutoLockChange  = { minutes ->
                        prefs.edit().putInt(KEY_AUTO_LOCK, minutes).apply()
                        autoLockMinutes = minutes
                    },
                    onClipboardChange = { seconds ->
                        prefs.edit().putInt(KEY_CLIPBOARD, seconds).apply()
                        clipboardSeconds = seconds
                    },
                    onAutofillClick   = {
                        startActivity(
                            Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        )
                    },
                    onBack            = { finish() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        autofillActive = autofillManager.hasEnabledAutofillServices()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsScreen(
    autoLockMinutes:   Int,
    clipboardSeconds:  Int,
    autofillActive:    Boolean,
    versionName:       String,
    onAutoLockChange:  (Int) -> Unit,
    onClipboardChange: (Int) -> Unit,
    onAutofillClick:   () -> Unit,
    onBack:            () -> Unit,
) {
    var showAutoLockDialog  by remember { mutableStateOf(false) }
    var showClipboardDialog by remember { mutableStateOf(false) }
    var showLicensesDialog  by remember { mutableStateOf(false) }

    val autoLockLabel  = if (autoLockMinutes  == -1) "Never" else "$autoLockMinutes min"
    val clipboardLabel = if (clipboardSeconds == -1) "Off"   else "${clipboardSeconds}s"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SettingsToolbar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp, bottom = 40.dp),
        ) {
            // ── VAULT ─────────────────────────────────────────────────────────
            SectionHeader("VAULT")
            SettingsRow(
                title       = "Auto-lock after",
                value       = autoLockLabel,
                showChevron = true,
                onClick     = { showAutoLockDialog = true },
            )
            RowDivider()
            SettingsRow(
                title       = "Clear clipboard after",
                value       = clipboardLabel,
                showChevron = true,
                onClick     = { showClipboardDialog = true },
            )

            // ── AUTOFILL ──────────────────────────────────────────────────────
            SectionHeader("AUTOFILL", topPadding = 28.dp)
            SettingsRow(
                title       = "Provider",
                value       = if (autofillActive) "Active ✓" else "Enable ›",
                valueColor  = if (autofillActive) Success else GoldPrimary,
                showChevron = false,
                onClick     = if (!autofillActive) onAutofillClick else null,
            )

            // ── ABOUT ─────────────────────────────────────────────────────────
            SectionHeader("ABOUT", topPadding = 28.dp)
            SettingsRow(
                title       = "Version",
                value       = versionName,
                showChevron = false,
                onClick     = null,
            )
            RowDivider()
            SettingsRow(
                title       = "Open-source libraries",
                value       = "",
                showChevron = true,
                onClick     = { showLicensesDialog = true },
            )
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showAutoLockDialog) {
        PickerDialog(
            title     = "Auto-lock after",
            labels    = LOCK_LABELS.toList(),
            onSelect  = { i -> onAutoLockChange(LOCK_VALUES[i]); showAutoLockDialog = false },
            onDismiss = { showAutoLockDialog = false },
        )
    }
    if (showClipboardDialog) {
        PickerDialog(
            title     = "Clear clipboard after",
            labels    = CLIP_LABELS.toList(),
            onSelect  = { i -> onClipboardChange(CLIP_VALUES[i]); showClipboardDialog = false },
            onDismiss = { showClipboardDialog = false },
        )
    }
    if (showLicensesDialog) {
        LicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

// ── Composable primitives ─────────────────────────────────────────────────────

@Composable
private fun SettingsToolbar(onBack: () -> Unit) {
    Column {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint               = TextPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text  = "Settings",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily    = FontFamily.Monospace,
                    letterSpacing = 0.05.em,
                    fontSize      = 18.sp,
                ),
                color = TextPrimary,
            )
        }
        HorizontalDivider(color = SurfaceModal)
    }
}

@Composable
private fun SectionHeader(title: String, topPadding: Dp = 0.dp) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelSmall.copy(
            fontFamily    = FontFamily.Monospace,
            letterSpacing = 0.15.em,
        ),
        color    = GoldPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = topPadding, bottom = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    title:       String,
    value:       String,
    modifier:    Modifier = Modifier,
    valueColor:  Color    = TextSecondary,
    showChevron: Boolean  = true,
    onClick:     (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = title,
            style    = MaterialTheme.typography.bodyLarge,
            color    = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (value.isNotEmpty()) {
            Text(
                text  = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor,
            )
        }
        if (showChevron) {
            Spacer(Modifier.width(4.dp))
            Text(
                text  = "›",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color    = SurfaceModal,
        modifier = Modifier.padding(start = 16.dp),
    )
}

@Composable
private fun PickerDialog(
    title:    String,
    labels:   List<String>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        },
        text             = {
            Column {
                labels.forEachIndexed { index, label ->
                    Text(
                        text     = label,
                        style    = MaterialTheme.typography.bodyLarge,
                        color    = TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton    = {},
        dismissButton    = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor   = SurfaceElevated,
    )
}

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = {
            Text(
                "Open-source libraries",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
        },
        text             = {
            Text(
                text  = "ZXing Android Embedded\nApache 2.0 License\n\n" +
                        "AndroidX Biometric\nApache 2.0 License\n\n" +
                        "AndroidX Security Crypto\nApache 2.0 License\n\n" +
                        "Google Material Components\nApache 2.0 License",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        confirmButton    = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = GoldPrimary)
            }
        },
        containerColor   = SurfaceElevated,
    )
}
