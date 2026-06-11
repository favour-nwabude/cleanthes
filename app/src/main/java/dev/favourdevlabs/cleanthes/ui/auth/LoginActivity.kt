package dev.favourdevlabs.cleanthes.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.security.BiometricHelper
import dev.favourdevlabs.cleanthes.ui.components.CleanthesPasswordField
import dev.favourdevlabs.cleanthes.ui.home.HomeActivity
import dev.favourdevlabs.cleanthes.ui.theme.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        LoginEvent.NavigateToHome -> {
                            startActivity(
                                Intent(this@LoginActivity, HomeActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                            finish()
                        }
                        LoginEvent.TriggerBiometric -> triggerBiometric()
                    }
                }
            }
        }

        setContent {
            CleanthesTheme {
                LoginScreen(viewModel = viewModel)
            }
        }
    }

    // Biometric stays in Activity — BiometricPrompt requires FragmentActivity context
    private fun triggerBiometric() {
        BiometricHelper.authenticate(
            this,
            object : BiometricHelper.AuthCallback {
                override fun onSuccess()                    = viewModel.onBiometricSuccess()
                override fun onFailure()                    = viewModel.onBiometricFailure()
                override fun onError(errorMessage: String) = viewModel.onBiometricError(errorMessage)
            }
        )
    }
}

@Composable
private fun LoginScreen(viewModel: LoginViewModel) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager  = LocalFocusManager.current

    // ── Shake animation ───────────────────────────────────────────────────────
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(uiState.shakeCounter) {
        if (uiState.shakeCounter > 0) {
            shakeOffset.animateTo(
                targetValue   = 0f,
                animationSpec = keyframes {
                    durationMillis = 500
                    (-16f) at 50
                      16f  at 100
                    (-12f) at 150
                      12f  at 200
                    (-8f)  at 250
                      8f   at 300
                    (-4f)  at 350
                      4f   at 400
                      0f   at 500
                }
            )
        }
    }

    // ── Attempts/lockout text — formatted in composable, not ViewModel ────────
    val attemptsText: String? = when {
        uiState.isLockedOut ->
            stringResource(R.string.login_locked_out, uiState.lockoutSecondsRemaining)
        uiState.failedAttempts > 0 ->
            stringResource(R.string.login_attempts_remaining, MAX_ATTEMPTS - uiState.failedAttempts)
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp)
    ) {

        // ── Main content ──────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Icon(
                imageVector        = Icons.Default.Lock,
                contentDescription = null,
                tint               = GoldPrimary,
                modifier           = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text          = stringResource(R.string.app_name).uppercase(),
                fontSize      = 34.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.12.em,
                color         = TextPrimary,
                textAlign     = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text      = stringResource(R.string.login_subtitle),
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            // ── Password + feedback ───────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text  = stringResource(R.string.login_label_password).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                CleanthesPasswordField(
                    value              = uiState.password,
                    onValueChange      = viewModel::onPasswordChange,
                    label              = stringResource(R.string.hint_master_password),
                    visible            = uiState.passwordVisible,
                    onVisibilityToggle = viewModel::onPasswordVisibilityToggle,
                    enabled            = !uiState.isLockedOut,
                    imeAction          = ImeAction.Done,
                    onImeAction        = {
                        focusManager.clearFocus()
                        viewModel.attemptPasswordUnlock()
                    },
                    modifier           = Modifier.offset(x = shakeOffset.value.dp),
                )
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    uiState.errorMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = Danger)
                    }
                }
                AnimatedVisibility(visible = attemptsText != null) {
                    attemptsText?.let {
                        Text(
                            text  = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.isLockedOut) Danger else GoldDim,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Unlock button ─────────────────────────────────────────────────
            Button(
                onClick  = { focusManager.clearFocus(); viewModel.attemptPasswordUnlock() },
                enabled  = !uiState.isLockedOut && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
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
                    Text(stringResource(R.string.login_btn_unlock), style = MaterialTheme.typography.labelLarge)
                }
            }

            // ── Biometric section (conditional) ───────────────────────────────
            if (uiState.showBiometricSection) {
                Spacer(Modifier.height(28.dp))
                HorizontalDivider(color = SurfaceModal, thickness = 1.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text      = stringResource(R.string.login_biometric_hint),
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                IconButton(
                    onClick  = viewModel::requestBiometricAuth,
                    enabled  = !uiState.isAuthenticating && !uiState.isLoading,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated),
                ) {
                    Icon(
                        imageVector        = Icons.Default.Fingerprint,
                        contentDescription = stringResource(R.string.cd_fingerprint_icon),
                        tint               = GoldPrimary.copy(
                            alpha = if (uiState.isAuthenticating) 0.4f else 1f
                        ),
                        modifier           = Modifier.size(36.dp),
                    )
                }
            }
        }

        // ── Stoic quote — bottom-anchored ─────────────────────────────────────
        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(40.dp).height(1.dp).background(GoldPrimary.copy(alpha = 0.5f)))
            Spacer(Modifier.height(16.dp))
            Text(
                text      = "The willing are led by fate;\nthe unwilling are dragged.",
                style     = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color     = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "— Cleanthes of Assos",
                style     = MaterialTheme.typography.labelSmall,
                color     = GoldDim,
                textAlign = TextAlign.Center,
            )
        }
    }
}
