package dev.favourdevlabs.cleanthes.ui.addedit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.zxing.integration.android.IntentIntegrator
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.components.CleanthesPasswordField
import dev.favourdevlabs.cleanthes.ui.components.PasswordStrengthBar
import dev.favourdevlabs.cleanthes.ui.theme.*
import dev.favourdevlabs.cleanthes.common.PasswordGenerator
import kotlinx.coroutines.launch

// Replaces IntentIntegrator + onActivityResult — no deprecation warnings
private class ScanQrContract : ActivityResultContract<Unit, String?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        IntentIntegrator(context as Activity).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Scan the 2FA QR code from your service's setup page")
            setBeepEnabled(false)
            setOrientationLocked(true)
        }.createScanIntent()

    override fun parseResult(resultCode: Int, intent: Intent?): String? =
        IntentIntegrator.parseActivityResult(resultCode, intent)?.contents
}

@AndroidEntryPoint
class AddEditActivity : AuthenticatedActivity() {

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
        const val NO_ENTRY_ID    = -1L
    }

    private val viewModel: AddEditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initForEntry(intent.getLongExtra(EXTRA_ENTRY_ID, NO_ENTRY_ID))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        AddEditEvent.NavigateBack -> finish()
                    }
                }
            }
        }

        setContent {
            CleanthesTheme {
                AddEditScreen(
                    viewModel = viewModel,
                    onBack    = { finish() },
                )
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun AddEditScreen(viewModel: AddEditViewModel, onBack: () -> Unit) {
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val categories  = stringArrayResource(R.array.categories_array).toList()

    // Set default category for new entries (composable-layer concern — needs string resources)
    LaunchedEffect(Unit) {
        if (uiState.category.isEmpty() && categories.isNotEmpty()) {
            viewModel.onCategoryChange(categories.first())
        }
    }

    var showGeneratorDialog by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(ScanQrContract()) { contents ->
        contents?.let { viewModel.onQrResult(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AddEditToolbar(
            isEditMode = uiState.isEditMode,
            isLoading  = uiState.isLoading,
            onBack     = onBack,
            onSave     = viewModel::attemptSave,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {

            // ── TITLE ─────────────────────────────────────────────────────────
            FieldSection(label = "TITLE *") {
                FormTextField(
                    value         = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    hint          = "e.g. Gmail, GitHub",
                    keyboardType  = KeyboardType.Text,
                    enabled       = !uiState.isLoading,
                )
            }

            // ── USERNAME ──────────────────────────────────────────────────────
            FieldSection(label = "USERNAME | EMAIL") {
                FormTextField(
                    value         = uiState.username,
                    onValueChange = viewModel::onUsernameChange,
                    hint          = "you@example.com",
                    keyboardType  = KeyboardType.Email,
                    enabled       = !uiState.isLoading,
                )
            }

            // ── PASSWORD ──────────────────────────────────────────────────────
            FieldSection(label = "PASSWORD *") {
                CleanthesPasswordField(
                    value              = uiState.password,
                    onValueChange      = viewModel::onPasswordChange,
                    label              = "Password",
                    visible            = uiState.passwordVisible,
                    onVisibilityToggle = viewModel::onPasswordVisibilityToggle,
                    enabled            = !uiState.isLoading,
                    imeAction          = ImeAction.Next,
                )
                PasswordStrengthBar(score = uiState.strengthScore)
                OutlinedButton(
                    onClick          = { showGeneratorDialog = true },
                    modifier         = Modifier.fillMaxWidth().height(36.dp),
                    shape            = RoundedCornerShape(6.dp),
                    border           = BorderStroke(1.dp, GoldDim),
                    colors           = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                    contentPadding   = PaddingValues(horizontal = 16.dp),
                    enabled          = !uiState.isLoading,
                ) {
                    Text("⚡ GENERATE", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
                }
            }

            // ── WEBSITE ───────────────────────────────────────────────────────
            FieldSection(label = "WEBSITE (OPTIONAL)") {
                FormTextField(
                    value         = uiState.website,
                    onValueChange = viewModel::onWebsiteChange,
                    hint          = "https://example.com",
                    keyboardType  = KeyboardType.Uri,
                    enabled       = !uiState.isLoading,
                )
            }

            // ── CATEGORY ──────────────────────────────────────────────────────
            FieldSection(label = "CATEGORY") {
                CategoryDropdown(
                    categories = categories,
                    selected   = uiState.category.ifEmpty { categories.firstOrNull() ?: "" },
                    onSelect   = viewModel::onCategoryChange,
                    enabled    = !uiState.isLoading,
                )
            }

            // ── NOTES ─────────────────────────────────────────────────────────
            FieldSection(label = "NOTES (OPTIONAL)") {
                FormTextField(
                    value         = uiState.notes,
                    onValueChange = viewModel::onNotesChange,
                    hint          = "Any additional info...",
                    keyboardType  = KeyboardType.Text,
                    singleLine    = false,
                    minLines      = 3,
                    maxLines      = 6,
                    enabled       = !uiState.isLoading,
                )
            }

            // ── AUTHENTICATOR SECRET ──────────────────────────────────────────
            FieldSection(label = "AUTHENTICATOR SECRET (OPTIONAL)") {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value         = uiState.totpSecret,
                        onValueChange = viewModel::onTotpSecretChange,
                        label         = { Text("Paste or scan Base32 secret") },
                        singleLine    = true,
                        enabled       = !uiState.isLoading,
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType    = KeyboardType.Text,
                            capitalization  = KeyboardCapitalization.Characters,
                            imeAction       = ImeAction.Done,
                        ),
                        colors   = cleanthesOutlinedTextFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick  = { scanLauncher.launch(Unit) },
                        enabled  = !uiState.isLoading,
                    ) {
                        Icon(
                            imageVector        = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR code",
                            tint               = GoldPrimary,
                        )
                    }
                }
            }

            // ── FAVOURITE ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onFavoriteToggle(!uiState.isFavorite) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked         = uiState.isFavorite,
                    onCheckedChange = viewModel::onFavoriteToggle,
                    colors          = CheckboxDefaults.colors(
                        checkedColor   = GoldPrimary,
                        uncheckedColor = TextMuted,
                        checkmarkColor = OnGold,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Mark as priority entry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            // ── ERROR ─────────────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Danger)
                }
            }

            // ── DELETE (edit mode only) ────────────────────────────────────────
            if (uiState.isEditMode) {
                OutlinedButton(
                    onClick          = { viewModel.showDeleteDialog(true) },
                    modifier         = Modifier.fillMaxWidth().height(48.dp),
                    shape            = RoundedCornerShape(8.dp),
                    border           = BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                    colors           = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                    enabled          = !uiState.isLoading,
                ) {
                    Text("DELETE ENTRY", style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp))
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showGeneratorDialog) {
        PasswordGeneratorDialog(
            onDismiss = { showGeneratorDialog = false },
            onCommit  = { password ->
                viewModel.onGeneratedPassword(password)
                showGeneratorDialog = false
            },
        )
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            containerColor   = SurfaceElevated,
            title            = {
                Text("Delete Entry", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            },
            text             = {
                Text(
                    "Delete \"${uiState.title}\"? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            },
            confirmButton    = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Delete", color = Danger)
                }
            },
            dismissButton    = {
                TextButton(onClick = { viewModel.showDeleteDialog(false) }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
}

// ── Composable primitives ─────────────────────────────────────────────────────

@Composable
private fun AddEditToolbar(
    isEditMode: Boolean,
    isLoading:  Boolean,
    onBack:     () -> Unit,
    onSave:     () -> Unit,
) {
    Column {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go back", tint = TextPrimary)
            }
            Text(
                text     = if (isEditMode) "EDIT ENTRY" else "NEW ENTRY",
                style    = MaterialTheme.typography.titleLarge.copy(
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.1.em,
                ),
                color    = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            Button(
                onClick          = onSave,
                enabled          = !isLoading,
                modifier         = Modifier.height(36.dp),
                shape            = RoundedCornerShape(6.dp),
                contentPadding   = PaddingValues(horizontal = 16.dp),
                colors           = ButtonDefaults.buttonColors(
                    containerColor         = GoldPrimary,
                    contentColor           = OnGold,
                    disabledContainerColor = GoldPrimary.copy(alpha = 0.4f),
                    disabledContentColor   = OnGold.copy(alpha = 0.4f),
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        color       = OnGold,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("SAVE", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
                }
            }
        }
        HorizontalDivider(color = SurfaceModal)
    }
}

@Composable
private fun FieldSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.em),
            color = TextSecondary,
        )
        content()
    }
}

@Composable
private fun FormTextField(
    value:         String,
    onValueChange: (String) -> Unit,
    hint:          String,
    modifier:      Modifier       = Modifier,
    keyboardType:  KeyboardType   = KeyboardType.Text,
    imeAction:     ImeAction      = ImeAction.Next,
    enabled:       Boolean        = true,
    singleLine:    Boolean        = true,
    minLines:      Int            = 1,
    maxLines:      Int            = if (singleLine) 1 else Int.MAX_VALUE,
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = { Text(hint) },
        singleLine      = singleLine,
        minLines        = minLines,
        maxLines        = maxLines,
        enabled         = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        colors          = cleanthesOutlinedTextFieldColors(),
        modifier        = modifier.fillMaxWidth(),
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CategoryDropdown(
    categories: List<String>,
    selected:   String,
    onSelect:   (String) -> Unit,
    enabled:    Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded        = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value        = selected,
            onValueChange = {},
            readOnly     = true,
            enabled      = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors       = cleanthesOutlinedTextFieldColors(),
            modifier     = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded          = expanded,
            onDismissRequest  = { expanded = false },
            modifier          = Modifier.background(SurfaceElevated),
        ) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text    = { Text(cat, color = TextPrimary) },
                    onClick = { onSelect(cat); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun PasswordGeneratorDialog(onDismiss: () -> Unit, onCommit: (String) -> Unit) {
    var length    by remember { mutableStateOf(PasswordGenerator.DEFAULT_LENGTH) }
    var uppercase by remember { mutableStateOf(true) }
    var lowercase by remember { mutableStateOf(true) }
    var digits    by remember { mutableStateOf(true) }
    var special   by remember { mutableStateOf(true) }
    var preview   by remember { mutableStateOf("") }
    var regenKey  by remember { mutableStateOf(0) }

    // Regenerate whenever any parameter changes, or regenKey is bumped
    LaunchedEffect(length, uppercase, lowercase, digits, special, regenKey) {
        preview = try {
            PasswordGenerator.generate(length, uppercase, lowercase, digits, special)
        } catch (_: IllegalArgumentException) { "" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceElevated,
        title            = {
            Text("Forge a Key", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Length control
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Length: $length",
                        style    = MaterialTheme.typography.bodyLarge,
                        color    = GoldPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { if (length > 8)  length-- }) {
                        Text("−", color = GoldPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { if (length < 32) length++ }) {
                        Text("+", color = GoldPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Checkboxes
                GeneratorCheckbox("Uppercase (A-Z)",   uppercase) { uppercase = it }
                GeneratorCheckbox("Lowercase (a-z)",   lowercase) { lowercase = it }
                GeneratorCheckbox("Digits (0-9)",      digits)    { digits    = it }
                GeneratorCheckbox("Special (!@#\$...)", special)  { special   = it }
                // Preview
                if (preview.isNotEmpty()) {
                    Text(
                        text     = preview,
                        style    = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color    = GoldPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDeep, RoundedCornerShape(6.dp))
                            .padding(12.dp),
                    )
                } else {
                    Text(
                        "Select at least one category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
                // Regenerate
                OutlinedButton(
                    onClick  = { regenKey++ },
                    modifier = Modifier.fillMaxWidth(),
                    border   = BorderStroke(1.dp, GoldDim),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                ) {
                    Text("↻  REGENERATE", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton    = {
            TextButton(
                onClick  = { if (preview.isNotEmpty()) { onCommit(preview); onDismiss() } },
                enabled  = preview.isNotEmpty(),
            ) {
                Text("Commit to this", color = GoldPrimary)
            }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
    )
}

@Composable
private fun GeneratorCheckbox(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked         = checked,
            onCheckedChange = onChange,
            colors          = CheckboxDefaults.colors(
                checkedColor   = GoldPrimary,
                uncheckedColor = TextMuted,
                checkmarkColor = OnGold,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

// Shared colors for all OutlinedTextFields in this file
@Composable
private fun cleanthesOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = GoldPrimary,
    unfocusedBorderColor  = TextMuted.copy(alpha = 0.35f),
    disabledBorderColor   = TextMuted.copy(alpha = 0.15f),
    focusedLabelColor     = GoldPrimary,
    unfocusedLabelColor   = TextMuted,
    disabledLabelColor    = TextMuted.copy(alpha = 0.4f),
    cursorColor           = GoldPrimary,
    focusedTextColor      = TextPrimary,
    unfocusedTextColor    = TextPrimary,
    disabledTextColor     = TextPrimary.copy(alpha = 0.4f),
)
