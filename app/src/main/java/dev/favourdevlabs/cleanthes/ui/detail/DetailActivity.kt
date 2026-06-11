package dev.favourdevlabs.cleanthes.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.favourdevlabs.cleanthes.ui.addedit.AddEditActivity
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.theme.*

@AndroidEntryPoint
class DetailActivity : AuthenticatedActivity() {

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }

    private val viewModel: DetailViewModel by viewModels()
    private var entryId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L)

        setContent {
            CleanthesTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState.shouldFinish) {
                    if (uiState.shouldFinish) finish()
                }

                DetailScreen(
                    uiState          = uiState,
                    onBack           = { finish() },
                    onEdit           = {
                        startActivity(
                            Intent(this, AddEditActivity::class.java).apply {
                                putExtra(AddEditActivity.EXTRA_ENTRY_ID, entryId)
                            }
                        )
                    },
                    onTogglePassword = viewModel::togglePasswordVisibility,
                    onCopy           = ::copyToClipboard,
                )
            }
        }
    }

    // Single load point — fires on first resume and after returning from EditActivity
    override fun onResume() {
        super.onResume()
        if (entryId != -1L) viewModel.loadEntry(entryId)
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseTotpUpdater()
    }

    private fun copyToClipboard(label: String, value: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun DetailScreen(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onTogglePassword: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DetailToolbar(title = uiState.title, onBack = onBack, onEdit = onEdit)

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        } else {
            DetailContent(
                uiState          = uiState,
                onTogglePassword = onTogglePassword,
                onCopy           = onCopy,
            )
        }
    }
}

@Composable
private fun DetailToolbar(title: String, onBack: () -> Unit, onEdit: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint               = TextPrimary,
                )
            }
            Text(
                text      = title.uppercase(),
                style     = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.1.em),
                color     = TextPrimary,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                modifier  = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            Button(
                onClick          = onEdit,
                modifier         = Modifier.height(36.dp),
                shape            = RoundedCornerShape(6.dp),
                contentPadding   = PaddingValues(horizontal = 16.dp),
                colors           = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor   = OnGold,
                ),
            ) {
                Text(
                    text  = "EDIT",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                )
            }
        }
        HorizontalDivider(color = SurfaceModal, thickness = 1.dp)
    }
}

@Composable
private fun DetailContent(
    uiState: DetailUiState,
    onTogglePassword: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 40.dp),
    ) {
        // Category
        Text(
            text  = uiState.category.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.12.em),
            color = TextSecondary,
        )
        HorizontalDivider(
            color    = SurfaceModal,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )

        // Username
        Spacer(Modifier.height(16.dp))
        DetailRow(label = "USERNAME / EMAIL", value = uiState.username) {
            IconButton(onClick = { onCopy("username", uiState.username) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy username", tint = GoldPrimary)
            }
        }

        // Password
        Spacer(Modifier.height(4.dp))
        Text("PASSWORD", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier         = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = uiState.displayPassword,
                style    = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                color    = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onTogglePassword) {
                Icon(
                    imageVector        = if (uiState.passwordVisible) Icons.Default.VisibilityOff
                                         else Icons.Default.Visibility,
                    contentDescription = "Toggle password visibility",
                    tint               = TextSecondary,
                )
            }
            IconButton(onClick = { onCopy("password", uiState.password) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy password", tint = GoldPrimary)
            }
        }

        // TOTP (conditional)
        if (uiState.hasTOTP) {
            Spacer(Modifier.height(20.dp))
            TotpSection(
                code             = uiState.totpCode,
                secondsRemaining = uiState.totpSecondsRemaining,
                period           = uiState.totpPeriod,
                onCopy           = { onCopy("totp", uiState.totpCode.replace(" ", "")) },
            )
        }

        // Website (conditional)
        if (!uiState.website.isNullOrEmpty()) {
            Spacer(Modifier.height(20.dp))
            DetailRow(
                label      = "WEBSITE",
                value      = uiState.website,
                valueStyle = MaterialTheme.typography.bodyLarge.copy(color = GoldPrimary),
            )
        }

        // Notes (conditional)
        if (!uiState.notes.isNullOrEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("NOTES", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                text  = uiState.notes,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                color = TextPrimary,
            )
        }

        // Priority (conditional)
        if (uiState.isFavorite) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.Star,
                    contentDescription = null,
                    tint               = GoldPrimary,
                    modifier           = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = "Priority entry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldPrimary,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.12.em),
                color = TextSecondary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text     = value,
                style    = valueStyle,
                color    = valueStyle.color.takeOrElse { TextPrimary },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailingContent()
    }
}

@Composable
private fun TotpSection(
    code: String,
    secondsRemaining: Int,
    period: Int,
    onCopy: () -> Unit,
) {
    // Smooth per-second interpolation — the bar glides rather than jumps
    val animatedProgress by animateFloatAsState(
        targetValue   = secondsRemaining.toFloat() / period.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label         = "totp_progress",
    )

    Column {
        Text(
            "AUTHENTICATOR CODE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.12.em),
            color = TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text         = code,
                fontSize     = 30.sp,
                fontFamily   = FontFamily.Monospace,
                fontWeight   = FontWeight.Bold,
                letterSpacing = 0.08.em,
                color        = GoldPrimary,
                modifier     = Modifier.weight(1f),
            )
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector        = Icons.Default.ContentCopy,
                    contentDescription = "Copy authenticator code",
                    tint               = GoldPrimary,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress  = { animatedProgress },
            modifier  = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color     = GoldPrimary,
            trackColor = SurfaceModal,
        )
    }
}
