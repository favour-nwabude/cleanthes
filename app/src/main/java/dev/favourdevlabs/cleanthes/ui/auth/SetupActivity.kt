package dev.favourdevlabs.cleanthes.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.favourdevlabs.cleanthes.ui.home.HomeActivity
import dev.favourdevlabs.cleanthes.ui.components.*
import dev.favourdevlabs.cleanthes.ui.theme.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupActivity : ComponentActivity() {

    private val viewModel: SetupViewModel by viewModels()
    private val splashHandler = Handler(Looper.getMainLooper())

    // mutableStateOf at Activity scope — Compose observes these directly
    private var splashDone  by mutableStateOf(false)
    private var showContent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !splashDone }
        super.onCreate(savedInstanceState)

        // Navigation events are Activity concerns — handle them here, not in a composable
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navEvents.collect { event ->
                    when (event) {
                        SetupNavEvent.NavigateToHome -> {
                            startActivity(
                                Intent(this@SetupActivity, HomeActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            )
                            finish()
                        }
                    }
                }
            }
        }

        setContent {
            CleanthesTheme {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SetupScreen(viewModel = viewModel)
                }
            }
        }

        splashHandler.postDelayed(::onSplashComplete, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        splashHandler.removeCallbacksAndMessages(null)
    }

    private fun onSplashComplete() {
        try {
            if (getEncryptedPrefs().getBoolean(KEY_VAULT_EXISTS, false)) {
                splashDone = true
                startActivity(
                    Intent(this, LoginActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
                return
            }
        } catch (_: Exception) {}

        showContent = true
        splashDone  = true
    }

    // Routing-only — ViewModel owns the write path
    private fun getEncryptedPrefs() = EncryptedSharedPreferences.create(
        this,
        PREFS_NAME,
        MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun SetupScreen(viewModel: SetupViewModel) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 72.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Forge Your Citadel",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your master password cannot be recovered.\nChoose with the gravity it deserves.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }

        // ── Password + strength ───────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanthesPasswordField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Master Password",
                visible = uiState.passwordVisible,
                onVisibilityToggle = viewModel::onPasswordVisibilityToggle,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            )
            PasswordStrengthBar(score = uiState.strengthScore)
            AnimatedVisibility(visible = uiState.password.isNotEmpty()) {
                Text(
                    text = strengthLabel(uiState.strengthScore),
                    style = MaterialTheme.typography.labelSmall,
                    color = strengthColor(uiState.strengthScore),
                )
            }
        }

        // ── Confirm + match indicator ─────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CleanthesPasswordField(
                value = uiState.confirm,
                onValueChange = viewModel::onConfirmChange,
                label = "Confirm Password",
                visible = uiState.confirmVisible,
                onVisibilityToggle = viewModel::onConfirmVisibilityToggle,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus(); viewModel.attemptSetup() },
            )
            AnimatedVisibility(visible = uiState.matchState != SetupUiState.MatchState.EMPTY) {
                val (text, color) = when (uiState.matchState) {
                    SetupUiState.MatchState.MATCH    -> "✓ Passwords match"           to Success
                    SetupUiState.MatchState.MISMATCH -> "✗ Passwords do not match"    to Danger
                    SetupUiState.MatchState.EMPTY    -> ""                             to Color.Transparent
                }
                Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
            }
        }

        // ── Inline error ──────────────────────────────────────────────────────
        AnimatedVisibility(visible = uiState.errorMessage != null) {
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Acknowledge ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated)
                .clickable { viewModel.onAcknowledgeToggle(!uiState.acknowledged) }
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(
                checked = uiState.acknowledged,
                onCheckedChange = viewModel::onAcknowledgeToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor   = GoldPrimary,
                    uncheckedColor = TextMuted,
                    checkmarkColor = OnGold,
                ),
            )
            Text(
                text = "I understand this password cannot be recovered. The gate does not ask twice.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        // ── Create button ─────────────────────────────────────────────────────
        Button(
            onClick = viewModel::attemptSetup,
            enabled = uiState.canCreate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = GoldPrimary,
                contentColor           = OnGold,
                disabledContainerColor = GoldPrimary.copy(alpha = 0.3f),
                disabledContentColor   = OnGold.copy(alpha = 0.3f),
            ),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = OnGold,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text  = "SEAL THE VAULT",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun strengthLabel(score: Int): String = when (score) {
    1    -> "Very Weak"
    2    -> "Weak"
    3    -> "Fair"
    4    -> "Strong"
    5    -> "Very Strong"
    else -> ""
}

private fun strengthColor(score: Int): Color = when (score) {
    1    -> StrengthVeryWeak
    2    -> StrengthWeak
    3    -> StrengthFair
    4    -> StrengthStrong
    5    -> StrengthVeryStrong
    else -> Color.Transparent
}
