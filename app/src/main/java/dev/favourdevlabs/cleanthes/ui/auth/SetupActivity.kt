package dev.favourdevlabs.cleanthes.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import dev.favourdevlabs.cleanthes.ui.home.HomeActivity

class SetupActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME            = "vault_secure_prefs"
        const val KEY_VAULT_EXISTS      = "vault_exists"
        const val KEY_AUTH_SALT         = "auth_salt"
        const val KEY_ENC_SALT          = "enc_salt"
        const val KEY_MASTER_HASH       = "master_hash"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_BIOMETRIC_SECRET  = "biometric_secret"

        const val MIN_PASSWORD_LENGTH  = 8
        const val MAX_FAILED_ATTEMPTS  = 5

        const val STRENGTH_VERY_WEAK   = 0
        const val STRENGTH_WEAK        = 1
        const val STRENGTH_FAIR        = 2
        const val STRENGTH_STRONG      = 3
        const val STRENGTH_VERY_STRONG = 4
    }

    private lateinit var etPassword:       EditText
    private lateinit var etConfirm:        EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnToggleConfirm:  ImageButton
    private lateinit var strengthSegments: Array<View>
    private lateinit var tvStrengthLabel:  TextView
    private lateinit var tvMatchIndicator: TextView
    private lateinit var tvError:          TextView
    private lateinit var btnCreate:        Button
    private lateinit var btnProgressBar:   ProgressBar
    private lateinit var cbAcknowledge:    CheckBox

    private var passwordVisible = false
    private var confirmVisible  = false

    private val splashHandler = Handler(Looper.getMainLooper())
    private var splashDone    = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !splashDone }
        super.onCreate(savedInstanceState)
        splashHandler.postDelayed(::onSplashComplete, 2000)
    }

    private fun onSplashComplete() {
        splashDone = true
        try {
            if (getEncryptedPrefs().getBoolean(KEY_VAULT_EXISTS, false)) {
                startActivity(
                    Intent(this, LoginActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
                return
            }
        } catch (_: Exception) {}

        setContentView(R.layout.activity_setup)
        bindViews()
        attachListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        splashHandler.removeCallbacksAndMessages(null)
    }

    private fun bindViews() {
        etPassword        = findViewById(R.id.setup_et_password)
        etConfirm         = findViewById(R.id.setup_et_confirm)
        btnTogglePassword = findViewById(R.id.setup_btn_toggle_password)
        btnToggleConfirm  = findViewById(R.id.setup_btn_confirm)
        tvStrengthLabel   = findViewById(R.id.setup_tv_strength_label)
        tvMatchIndicator  = findViewById(R.id.setup_tv_match_indicator)
        tvError           = findViewById(R.id.setup_tv_error)
        btnCreate         = findViewById(R.id.setup_btn_create)
        btnProgressBar    = findViewById(R.id.setup_progress)
        cbAcknowledge     = findViewById(R.id.setup_cb_acknowledge)

        btnCreate.isEnabled = false
        btnCreate.alpha = 0.35f
        btnCreate.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.citadel_gold)
        )

        strengthSegments = arrayOf(
            findViewById(R.id.setup_strength_seg1),
            findViewById(R.id.setup_strength_seg2),
            findViewById(R.id.setup_strength_seg3),
            findViewById(R.id.setup_strength_seg4),
            findViewById(R.id.setup_strength_seg5)
        )
    }

    private fun attachListeners() {
        etPassword.addTextChangedListener(simpleWatcher { updateStrengthBar(it); updateMatchIndicator(); hideError() })
        etConfirm.addTextChangedListener(simpleWatcher { updateMatchIndicator(); hideError() })
        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            togglePasswordVisibility(etPassword, btnTogglePassword, passwordVisible)
        }
        btnToggleConfirm.setOnClickListener {
            confirmVisible = !confirmVisible
            togglePasswordVisibility(etConfirm, btnToggleConfirm, confirmVisible)
        }
        etConfirm.setOnEditorActionListener { _, _, _ -> attemptSetup(); true }
        cbAcknowledge.setOnCheckedChangeListener { _, isChecked ->
            btnCreate.isEnabled = isChecked
            btnCreate.alpha = if (isChecked) 1.0f else 0.35f
            btnCreate.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.citadel_gold)
            )
        }
        btnCreate.setOnClickListener { attemptSetup() }
    }

    private fun attemptSetup() {
        val password = etPassword.text.toString()
        val confirm  = etConfirm.text.toString()

        if (password.length < MIN_PASSWORD_LENGTH) {
            showError(getString(R.string.error_password_too_short)); return
        }
        if (!password.contains(Regex("\\d"))) {
            showError(getString(R.string.error_password_no_number)); return
        }
        if (!password.contains(Regex("[!@#\$%^&*()_+\\-=\\[\\]{}|;':\",./<>?]"))) {
            showError(getString(R.string.error_password_no_special)); return
        }
        if (password != confirm) {
            showError(getString(R.string.error_passwords_no_match)); return
        }

        setLoadingState(true)
        Thread { performSetup(password) }.start()
    }

    private fun performSetup(masterPassword: String) {
        try {
            val storedHash   = KeyDerivation.hashPassword(masterPassword.toCharArray())
            val encSaltBytes = KeyDerivation.generateSalt()
            val encSalt      = Base64.encodeToString(encSaltBytes, Base64.NO_WRAP)

            getEncryptedPrefs().edit()
                .putBoolean(KEY_VAULT_EXISTS,      true)
                .putString(KEY_AUTH_SALT,          storedHash.saltBase64)
                .putString(KEY_ENC_SALT,           encSalt)
                .putString(KEY_MASTER_HASH,        storedHash.hashBase64)
                .putBoolean(KEY_BIOMETRIC_ENABLED, true)
                .putString(KEY_BIOMETRIC_SECRET,   masterPassword)
                .apply()

            runOnUiThread(::navigateHome)
        } catch (_: Exception) {
            runOnUiThread {
                setLoadingState(false)
                showError(getString(R.string.error_setup_failed))
            }
        }
    }

    private fun navigateHome() {
        startActivity(
            Intent(this, HomeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun updateStrengthBar(password: String) {
        val score = computeStrengthScore(password)
        val color = getStrengthColor(score)
        val empty = ContextCompat.getColor(this, R.color.cleanthes_strength_empty)
        strengthSegments.forEachIndexed { i, seg ->
            seg.setBackgroundColor(if (i < score) color else empty)
        }
        tvStrengthLabel.text = getStrengthLabel(score)
        tvStrengthLabel.setTextColor(color)
    }

    private fun computeStrengthScore(password: String): Int {
        if (password.isEmpty()) return 0
        var score = 0
        if (password.length >= MIN_PASSWORD_LENGTH)                      score++
        if (password.contains(Regex("[A-Z]")))                           score++
        if (password.contains(Regex("\\d")))                             score++
        if (password.contains(Regex("[!@#\$%^&*()_+\\-=\\[\\]{}|]")))  score++
        if (password.length >= 16)                                        score++
        return score
    }

    private fun getStrengthColor(score: Int): Int {
        val res = when (score) {
            1    -> R.color.cleanthes_strength_very_weak
            2    -> R.color.cleanthes_strength_weak
            3    -> R.color.cleanthes_strength_fair
            4    -> R.color.cleanthes_strength_strong
            5    -> R.color.cleanthes_strength_very_strong
            else -> R.color.cleanthes_strength_empty
        }
        return ContextCompat.getColor(this, res)
    }

    private fun getStrengthLabel(score: Int): String = when (score) {
        1    -> getString(R.string.cleanthes_strength_very_weak)
        2    -> getString(R.string.cleanthes_strength_weak)
        3    -> getString(R.string.cleanthes_strength_fair)
        4    -> getString(R.string.cleanthes_strength_strong)
        5    -> getString(R.string.cleanthes_strength_very_strong)
        else -> ""
    }

    private fun updateMatchIndicator() {
        val password = etPassword.text.toString()
        val confirm  = etConfirm.text.toString()
        if (confirm.isEmpty()) { tvMatchIndicator.text = ""; return }
        if (password == confirm) {
            tvMatchIndicator.text = "✓ Passwords match"
            tvMatchIndicator.setTextColor(ContextCompat.getColor(this, R.color.cleanthes_success))
        } else {
            tvMatchIndicator.text = "✗ Passwords do not match"
            tvMatchIndicator.setTextColor(ContextCompat.getColor(this, R.color.cleanthes_error))
        }
    }

    private fun togglePasswordVisibility(field: EditText, toggle: ImageButton, visible: Boolean) {
        field.inputType = if (visible)
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        field.setSelection(field.text.length)
        toggle.setImageResource(if (visible) R.drawable.ic_eye_on else R.drawable.ic_eye_off)
    }

    private fun setLoadingState(loading: Boolean) {
        cbAcknowledge.isEnabled = !loading
        btnCreate.isEnabled = !loading
        btnCreate.alpha = if (loading) 0.35f else 1.0f
        btnCreate.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.citadel_gold)
        )
        btnProgressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }

    @Throws(Exception::class)
    private fun getEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            this,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun simpleWatcher(onChanged: (String) -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun afterTextChanged(s: android.text.Editable?) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChanged(s?.toString() ?: "")
        }
    }
}

