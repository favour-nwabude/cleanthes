package dev.favourdevlabs.cleanthes.ui.auth

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import dev.favourdevlabs.cleanthes.security.BiometricHelper
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import dev.favourdevlabs.cleanthes.ui.home.HomeActivity
import javax.crypto.SecretKey

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
    }

    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var tvError: TextView
    private lateinit var tvAttempts: TextView
    private lateinit var btnUnlock: Button
    private lateinit var divider: View
    private lateinit var tvBiometricHint: TextView
    private lateinit var btnBiometric: ImageButton
    private lateinit var progressBar: ProgressBar

    private var passwordVisible = false
    private var failedAttempts = 0
    private var isLockedOut = false
    private var lockoutTimer: CountDownTimer? = null

    private var storedAuthSalt: String? = null
    private var storedEncSalt: String? = null
    private var storedMasterHash: String? = null
    private var biometricEnabled = false
    private var storedBiometricSecret: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        loadStoredCredentials()
        bindViews()
        attachListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        lockoutTimer?.cancel()
    }

    private fun loadStoredCredentials() {
        try {
            val prefs = getEncryptedPrefs()
            storedAuthSalt = prefs.getString(SetupActivity.KEY_AUTH_SALT, null)
            storedEncSalt = prefs.getString(SetupActivity.KEY_ENC_SALT, null)
            storedMasterHash = prefs.getString(SetupActivity.KEY_MASTER_HASH, null)
            biometricEnabled = prefs.getBoolean(SetupActivity.KEY_BIOMETRIC_ENABLED, false)
            storedBiometricSecret = prefs.getString(SetupActivity.KEY_BIOMETRIC_SECRET, null)
        } catch (e: Exception) {
            storedAuthSalt = null
            storedEncSalt = null
            storedMasterHash = null
            biometricEnabled = false
            storedBiometricSecret = null
        }
    }

    private fun bindViews() {
        etPassword = findViewById(R.id.login_et_password)
        btnTogglePassword = findViewById(R.id.login_btn_toggle_password)
        tvError = findViewById(R.id.login_tv_error)
        tvAttempts = findViewById(R.id.login_tv_attempts)
        btnUnlock = findViewById(R.id.login_btn_unlock)
        divider = findViewById(R.id.login_divider)
        tvBiometricHint = findViewById(R.id.login_tv_biometric_hint)
        btnBiometric = findViewById(R.id.login_btn_biometric)
        progressBar = findViewById(R.id.login_progress)

        if (biometricEnabled && BiometricHelper.isBiometricAvailable(this)) {
            divider.visibility = View.VISIBLE
            tvBiometricHint.visibility = View.VISIBLE
            btnBiometric.visibility = View.VISIBLE
        }

        btnUnlock.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold))
    }

    private fun attachListeners() {
        etPassword.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                hideError()
            }
        })

        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            togglePasswordVisibility(passwordVisible)
        }

        etPassword.setOnEditorActionListener { _, _, _ ->
            if (!isLockedOut) attemptPasswordUnlock()
            true
        }

        btnUnlock.setOnClickListener {
            if (!isLockedOut) attemptPasswordUnlock()
        }

        btnBiometric.setOnClickListener {
            if (BiometricHelper.isBiometricAvailable(this)) {
                BiometricHelper.authenticate(this, object : BiometricHelper.AuthCallback {
                    override fun onSuccess() = deriveKeyAndNavigate(null, fromBiometric = true)
                    override fun onFailure() {}
                    override fun onError(errorMessage: String) = showError(errorMessage)
                })
            }
        }
    }

    private fun attemptPasswordUnlock() {
        val password = etPassword.text.toString()
        if (password.isEmpty()) { showError("Enter your master password"); return }
        setLoadingState(true)
        Thread { verifyPasswordOnBackground(password) }.start()
    }

    private fun verifyPasswordOnBackground(attempt: String) {
    val authSalt   = storedAuthSalt   ?: return
    val masterHash = storedMasterHash ?: return
    try {
        val correct = KeyDerivation.verifyMasterPassword(
            attempt.toCharArray(), authSalt, masterHash
        )
           runOnUiThread {
                setLoadingState(false)
                if (correct) { failedAttempts = 0; deriveKeyAndNavigate(attempt, fromBiometric = false) }
                else handleFailedAttempt()
            }
        } catch (e: Exception) {
            runOnUiThread { setLoadingState(false); showError(getString(R.string.error_generic)) }
        }
    }

    private fun deriveKeyAndNavigate(masterPassword: String?, fromBiometric: Boolean) {
        setLoadingState(true)
        Thread {
            try {
                val saltBytes = android.util.Base64.decode(storedEncSalt, android.util.Base64.DEFAULT)
                val source = if (fromBiometric) storedBiometricSecret!! else masterPassword!!
                val sessionKey = KeyDerivation.deriveKey(source.toCharArray(), saltBytes)
                SessionManager.setSessionKey(sessionKey)
                runOnUiThread { setLoadingState(false); navigateToHome() }
            } catch (e: Exception) {
                runOnUiThread { setLoadingState(false); showError(getString(R.string.error_generic)) }
            }
        }.start()
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun handleFailedAttempt() {
        failedAttempts++
        showError(getString(R.string.error_wrong_password))
        if (failedAttempts >= MAX_ATTEMPTS) {
            startLockout()
        } else {
            val remaining = MAX_ATTEMPTS - failedAttempts
            tvAttempts.text = getString(R.string.login_attempts_remaining, remaining)
            tvAttempts.visibility = View.VISIBLE
        }
        shakeView(etPassword.parent as? View ?: etPassword)
        etPassword.setText("")
    }

    private fun startLockout() {
        isLockedOut = true
        btnUnlock.isEnabled = false
        etPassword.isEnabled = false

        lockoutTimer = object : CountDownTimer(LOCKOUT_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                tvAttempts.text = getString(R.string.login_locked_out, secondsLeft)
                tvAttempts.visibility = View.VISIBLE
                tvAttempts.setTextColor(ContextCompat.getColor(this@LoginActivity, R.color.cleanthes_error))
            }

            override fun onFinish() {
                isLockedOut = false
                failedAttempts = 0
                btnUnlock.isEnabled = true
                etPassword.isEnabled = true
                tvAttempts.visibility = View.GONE
                tvError.visibility = View.GONE
                etPassword.requestFocus()
            }
        }.start()
    }

    private fun togglePasswordVisibility(visible: Boolean) {
        etPassword.inputType = if (visible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        etPassword.setSelection(etPassword.text.length)
        btnTogglePassword.setImageResource(if (visible) R.drawable.ic_eye_on else R.drawable.ic_eye_off)
    }

    private fun setLoadingState(loading: Boolean) {
        btnUnlock.isEnabled = !loading && !isLockedOut
        btnUnlock.alpha = if (loading) 0.5f else 1.0f
        btnUnlock.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold))
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) { tvError.text = message; tvError.visibility = View.VISIBLE }
    private fun hideError() { tvError.visibility = View.GONE }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, -16f, 16f, -12f, 12f, -8f, 8f, -4f, 4f, 0f)
            .apply { duration = 500 }
            .start()
    }

    @Throws(Exception::class)
    private fun getEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            this,
            SetupActivity.PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private abstract class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable) {}
    }
}
