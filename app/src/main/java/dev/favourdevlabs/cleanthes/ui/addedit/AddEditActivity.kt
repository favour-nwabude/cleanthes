package dev.favourdevlabs.cleanthes.ui.addedit

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import dev.favourdevlabs.cleanthes.security.OtpAuthParser
import dev.favourdevlabs.cleanthes.security.TOTPGenerator
import dev.favourdevlabs.cleanthes.ui.auth.LoginActivity
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.utils.PasswordGenerator

class AddEditActivity : AuthenticatedActivity() {

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
        const val NO_ENTRY_ID = -1L
    }

    // Form views
    private lateinit var tvScreenTitle: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnSave: Button
    private lateinit var etTitle: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnGenerate: Button
    private lateinit var strengthSegments: Array<View>
    private lateinit var etWebsite: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etNotes: EditText
    private lateinit var etTotpSecret: EditText
    private lateinit var btnScanTotp: ImageButton
    private lateinit var checkBoxFavorite: CheckBox
    private lateinit var tvError: TextView
    private lateinit var btnDelete: Button

    // State
    private var isEditMode = false
    private var entryId = NO_ENTRY_ID
    private var existingEntry: VaultEntry? = null
    private var passwordVisible = false
    private lateinit var repository: VaultRepository

    // TOTP metadata — defaults match RFC 6238 (~99% of services)
    private var scannedTotpAlgorithm = "SHA1"
    private var scannedTotpDigits = 6
    private var scannedTotpPeriod = 30
    private var scannedTotpIssuer: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)
        repository = VaultRepository.getInstance(this)
        bindViews()
        setupCategorySpinner()
        determineMode()
        attachListeners()
    }

    private fun bindViews() {
        tvScreenTitle = findViewById(R.id.addedit_tv_title)
        btnBack = findViewById(R.id.addedit_btn_back)
        btnSave = findViewById(R.id.addedit_btn_save)
        etTitle = findViewById(R.id.addedit_et_title)
        etUsername = findViewById(R.id.addedit_et_username)
        etPassword = findViewById(R.id.addedit_et_password)
        btnTogglePassword = findViewById(R.id.addedit_btn_toggle_password)
        btnGenerate = findViewById(R.id.addedit_btn_generate)
        etWebsite = findViewById(R.id.addedit_et_website)
        spinnerCategory = findViewById(R.id.addedit_spinner_category)
        etNotes = findViewById(R.id.addedit_et_notes)
        etTotpSecret = findViewById(R.id.addedit_et_totp_secret)
        btnScanTotp = findViewById(R.id.addedit_btn_scan_totp)
        checkBoxFavorite = findViewById(R.id.addedit_checkbox_favorite)
        tvError = findViewById(R.id.addedit_tv_error)
        btnDelete = findViewById(R.id.btn_delete)

        btnSave.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold))

        strengthSegments = arrayOf(
            findViewById(R.id.addedit_seg1),
            findViewById(R.id.addedit_seg2),
            findViewById(R.id.addedit_seg3),
            findViewById(R.id.addedit_seg4),
            findViewById(R.id.addedit_seg5)
        )
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.categories_array,
            android.R.layout.simple_spinner_item
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerCategory.adapter = adapter
    }

    private fun determineMode() {
        entryId = intent.getLongExtra(EXTRA_ENTRY_ID, NO_ENTRY_ID)
        if (entryId != NO_ENTRY_ID) {
            isEditMode = true
            tvScreenTitle.text = getString(R.string.addedit_title_edit)
            btnDelete.visibility = View.VISIBLE
            loadExistingEntry()
        } else {
            tvScreenTitle.text = getString(R.string.addedit_title_new)
            btnDelete.visibility = View.GONE
        }
    }

    private fun loadExistingEntry() {
        val key = SessionManager.getSessionKey() ?: run { finish(); return }
        Thread {
            try {
                val entry = repository.getEntryById(entryId, key)
                runOnUiThread {
                    if (entry != null) { existingEntry = entry; populateFields(entry) }
                    else finish()
                }
            } catch (e: Exception) {
                runOnUiThread { finish() }
            }
        }.start()
    }

    private fun populateFields(entry: VaultEntry) {
        etTitle.setText(entry.title)
        etUsername.setText(entry.username)
        etPassword.setText(entry.encryptedPassword)
        etWebsite.setText(entry.website ?: "")
        etNotes.setText(entry.notes ?: "")
        etTotpSecret.setText(entry.totpSecret ?: "")
        checkBoxFavorite.isChecked = entry.isFavorite

        scannedTotpAlgorithm = entry.totpAlgorithm
        scannedTotpDigits = entry.totpDigits
        scannedTotpPeriod = entry.totpPeriod
        scannedTotpIssuer = entry.totpIssuer

        @Suppress("UNCHECKED_CAST")
        val adapter = spinnerCategory.adapter as ArrayAdapter<String>
        val pos = adapter.getPosition(entry.category)
        if (pos >= 0) spinnerCategory.setSelection(pos)

        updateStrengthBar(entry.encryptedPassword)
    }

    private fun attachListeners() {
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { attemptSave() }
        btnDelete.setOnClickListener { confirmDelete() }
        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            togglePasswordVisibility(passwordVisible)
        }
        btnGenerate.setOnClickListener { showGeneratorDialog() }
        btnScanTotp.setOnClickListener {
            IntentIntegrator(this).apply {
                setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                setPrompt("Scan the 2FA QR code from your service's setup page")
                setBeepEnabled(false)
                setOrientationLocked(true)
                initiateScan()
            }
        }
        etPassword.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                updateStrengthBar(s.toString())
                hideError()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let { handleQrResult(it) }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleQrResult(rawContent: String) {
        try {
            val parsed = OtpAuthParser.parse(rawContent)
            etTotpSecret.setText(parsed.secret)
            scannedTotpAlgorithm = parsed.algorithm
            scannedTotpDigits = parsed.digits
            scannedTotpPeriod = parsed.period
            scannedTotpIssuer = parsed.issuer
            if (!isEditMode && etTitle.text.toString().trim().isEmpty() && parsed.issuer != null) {
                etTitle.setText(parsed.issuer)
            }
            hideError()
            Toast.makeText(this, "Authenticator secret imported", Toast.LENGTH_SHORT).show()
        } catch (e: UnsupportedOperationException) {
            showError("HOTP is not supported — only TOTP (time-based) codes.")
        } catch (e: Exception) {
            showError("Invalid QR code — not a valid 2FA setup code.")
        }
    }

    private fun attemptSave() {
        val title = etTitle.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val website = etWebsite.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val notes = etNotes.text.toString().trim()
        val fav = checkBoxFavorite.isChecked
        val totpRaw = etTotpSecret.text.toString().trim().uppercase().replace("\\s+".toRegex(), "")

        if (title.isEmpty()) { showError("Title is required"); etTitle.requestFocus(); return }
        if (username.isEmpty()) { showError("Username or Email is required"); etUsername.requestFocus(); return }
        if (password.isEmpty()) { showError("Password is required"); etPassword.requestFocus(); return }

        if (totpRaw.isNotEmpty()) {
            try {
                TOTPGenerator.generate(totpRaw, scannedTotpDigits, scannedTotpPeriod, scannedTotpAlgorithm)
            } catch (e: Exception) {
                showError("Invalid authenticator secret — check the Base32 code.")
                etTotpSecret.requestFocus()
                return
            }
        }

        val key = SessionManager.getSessionKey() ?: run { finish(); return }

        val finalTotp = totpRaw.ifEmpty { null }
        val finalIssuer = if (finalTotp != null) scannedTotpIssuer else null
        val finalAlgo = scannedTotpAlgorithm
        val finalDigits = scannedTotpDigits
        val finalPeriod = scannedTotpPeriod

        setFormEnabled(false)
        Thread {
            try {
                if (isEditMode && existingEntry != null) {
                    existingEntry!!.apply {
    this.title         = title
    this.username      = username
    this.website       = website.ifEmpty { null }
    this.category      = category
    this.notes         = notes.ifEmpty { null }
    this.isFavorite    = fav
    this.totpSecret    = finalTotp
    this.totpIssuer    = finalIssuer
    this.totpAlgorithm = finalAlgo
    this.totpDigits    = finalDigits
    this.totpPeriod    = finalPeriod
}
                    repository.updateEntry(existingEntry!!, password, key)
                } else {
                    repository.addEntry(
                        title, username, password,
                        website.ifEmpty { null }, category, notes.ifEmpty { null },
                        fav, finalTotp, finalIssuer, finalDigits, finalPeriod, finalAlgo, key
                    )
                }
                runOnUiThread { setFormEnabled(true); finish() }
            } catch (e: Exception) {
                runOnUiThread { setFormEnabled(true); showError("Failed to save entry. Please try again.") }
            }
        }.start()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Delete \"${etTitle.text}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> performDelete() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete() {
        if (!isEditMode || existingEntry == null) return
        Thread {
            try {
                repository.deleteEntry(existingEntry!!.id)
                runOnUiThread { finish() }
            } catch (e: Exception) {
                runOnUiThread { showError("Failed to delete entry.") }
            }
        }.start()
    }

    private fun showGeneratorDialog() {
        val length = intArrayOf(PasswordGenerator.DEFAULT_LENGTH)
        val uppercase = booleanArrayOf(true)
        val lowercase = booleanArrayOf(true)
        val digits = booleanArrayOf(true)
        val special = booleanArrayOf(true)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(20)
            setPadding(pad, pad, pad, 0)
            setBackgroundColor(getColor(R.color.cleanthes_surface))
        }

        val lengthRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tvLen = TextView(this).apply {
            text = "Length: ${length[0]}"
            setTextColor(getColor(R.color.citadel_gold))
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val minus = makeGoldButton("−")
        val plus = makeGoldButton("+")
        lengthRow.addView(tvLen); lengthRow.addView(minus); lengthRow.addView(plus)
        layout.addView(lengthRow)

        val cbUp = makeDialogCheckbox("Uppercase (A-Z)", true)
        val cbLo = makeDialogCheckbox("Lowercase (a-z)", true)
        val cbDi = makeDialogCheckbox("Digits (0-9)", true)
        val cbSp = makeDialogCheckbox("Special (!@#\$...)", true)
        layout.addView(cbUp); layout.addView(cbLo); layout.addView(cbDi); layout.addView(cbSp)

        val tvPreview = TextView(this).apply {
            setTextColor(getColor(R.color.citadel_gold))
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setPadding(0, dpToPx(12), 0, dpToPx(4))
            text = PasswordGenerator.generate(length[0], true, true, true, true)
        }
        layout.addView(tvPreview)

        val btnRegen = Button(this).apply {
            text = "↻ REGENERATE"
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.cleanthes_border))
            setTextColor(getColor(R.color.citadel_gold))
        }
        layout.addView(btnRegen)

        val regen = {
            try {
                tvPreview.text = PasswordGenerator.generate(
                    length[0], uppercase[0], lowercase[0], digits[0], special[0]
                )
            } catch (e: IllegalArgumentException) {
                tvPreview.text = "Select at least one category"
            }
        }

        minus.setOnClickListener { if (length[0] > 8) { length[0]--; tvLen.text = "Length: ${length[0]}"; regen() } }
        plus.setOnClickListener { if (length[0] < 32) { length[0]++; tvLen.text = "Length: ${length[0]}"; regen() } }
        cbUp.setOnCheckedChangeListener { _, c -> uppercase[0] = c; regen() }
        cbLo.setOnCheckedChangeListener { _, c -> lowercase[0] = c; regen() }
        cbDi.setOnCheckedChangeListener { _, c -> digits[0] = c; regen() }
        cbSp.setOnCheckedChangeListener { _, c -> special[0] = c; regen() }
        btnRegen.setOnClickListener { regen() }

        MaterialAlertDialogBuilder(this)
            .setTitle("Forge a key")
            .setView(layout)
            .setPositiveButton("Commit to this") { _, _ ->
                val gen = tvPreview.text.toString()
                if (gen != "Select at least one category") {
                    etPassword.setText(gen)
                    updateStrengthBar(gen)
                    if (!passwordVisible) { passwordVisible = true; togglePasswordVisibility(true) }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun makeGoldButton(text: String) = Button(this).apply {
        this.text = text
        textSize = 16f
        setTextColor(getColor(R.color.cleanthes_black))
        backgroundTintList = ColorStateList.valueOf(getColor(R.color.citadel_gold))
    }

    private fun makeDialogCheckbox(label: String, checked: Boolean) = CheckBox(this).apply {
        text = label
        isChecked = checked
        setTextColor(getColor(R.color.cleanthes_text_secondary))
        buttonTintList = ColorStateList.valueOf(getColor(R.color.citadel_gold))
    }

    private fun updateStrengthBar(password: String) {
        val score = PasswordGenerator.evaluateStrength(password)
        val colorRes = intArrayOf(
            R.color.cleanthes_strength_empty,
            R.color.cleanthes_strength_very_weak,
            R.color.cleanthes_strength_weak,
            R.color.cleanthes_strength_fair,
            R.color.cleanthes_strength_strong,
            R.color.cleanthes_strength_very_strong
        )
        val active = ContextCompat.getColor(this, colorRes[score])
        val empty = ContextCompat.getColor(this, R.color.cleanthes_strength_empty)
        strengthSegments.forEachIndexed { i, seg ->
            seg.setBackgroundColor(if (i < score) active else empty)
        }
    }

    private fun togglePasswordVisibility(visible: Boolean) {
        etPassword.inputType = if (visible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        etPassword.setSelection(etPassword.text.length)
        btnTogglePassword.setImageResource(if (visible) R.drawable.ic_eye_on else R.drawable.ic_eye_off)
    }

    private fun setFormEnabled(enabled: Boolean) {
        btnSave.isEnabled = enabled
        btnSave.alpha = if (enabled) 1f else 0.5f
        btnSave.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold))
        etTitle.isEnabled = enabled
        etPassword.isEnabled = enabled
        etUsername.isEnabled = enabled
    }

    private fun showError(msg: String) { tvError.text = msg; tvError.visibility = View.VISIBLE }
    private fun hideError() { tvError.visibility = View.GONE }
    private fun dpToPx(dp: Int) = Math.round(dp * resources.displayMetrics.density)

    private abstract class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable) {}
    }
}
