package dev.favourdevlabs.cleanthes.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.content.res.ColorStateList;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository;
import dev.favourdevlabs.cleanthes.security.BiometricHelper;
import dev.favourdevlabs.cleanthes.security.KeyDerivation;
import dev.favourdevlabs.cleanthes.ui.home.HomeActivity;

import javax.crypto.SecretKey;

public class LoginActivity extends AppCompatActivity {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 30_000L;

    private EditText etPassword;
    private ImageButton btnTogglePassword;
    private TextView tvError;
    private TextView tvAttempts;
    private Button btnUnlock;
    private View divider;
    private TextView tvBiometricHint;
    private ImageButton btnBiometric;
    private ProgressBar progressBar;

    private boolean passwordVisible = false;
    private int failedAttempts = 0;
    private boolean isLockedOut = false;
    private CountDownTimer lockoutTimer = null;

    private String storedAuthSalt;
    private String storedEncSalt;
    private String storedMasterHash;
    private boolean biometricEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_login);

        loadStoredCredentials();
        bindViews();
        attachListeners();

        if (biometricEnabled && BiometricHelper.isBiometricAvailable(this)) {
            BiometricHelper.authenticate(this, new BiometricHelper.AuthCallback() {
                @Override
                public void onSuccess() {
                    deriveKeyAndNavigate(null, true);
                }

                @Override
                public void onFailure() {
                }

                @Override
                public void onError(String errorMessage) {
                    showError(errorMessage);
                }
            });
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lockoutTimer != null) {
            lockoutTimer.cancel();
        }
    }

    private void loadStoredCredentials() {
        try {
            SharedPreferences prefs = getEncryptedPrefs();
            storedAuthSalt = prefs.getString(SetupActivity.KEY_AUTH_SALT, null);
            storedEncSalt = prefs.getString(SetupActivity.KEY_ENC_SALT, null);
            storedMasterHash = prefs.getString(SetupActivity.KEY_MASTER_HASH, null);
            biometricEnabled = prefs.getBoolean(SetupActivity.KEY_BIOMETRIC_ENABLED, false);
        } catch (Exception e) {
            storedAuthSalt = null;
            storedEncSalt = null;
            storedMasterHash = null;
            biometricEnabled = false;
        }
    }

    private void bindViews() {
        etPassword = findViewById(R.id.login_et_password);
        btnTogglePassword = findViewById(R.id.login_btn_toggle_password);
        tvError = findViewById(R.id.login_tv_error);
        tvAttempts = findViewById(R.id.login_tv_attempts);
        btnUnlock = findViewById(R.id.login_btn_unlock);
        divider = findViewById(R.id.login_divider);
        tvBiometricHint = findViewById(R.id.login_tv_biometric_hint);
        btnBiometric = findViewById(R.id.login_btn_biometric);
        progressBar = findViewById(R.id.login_progress);

        // Force gold — Material Button overrides backgroundTint to gray when disabled
        btnUnlock.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));
    }

    private void attachListeners() {

        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hideError();
            }
        });

        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            togglePasswordVisibility(passwordVisible);
        });

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (!isLockedOut)
                attemptPasswordUnlock();
            return true;
        });

        btnUnlock.setOnClickListener(v -> {
            if (!isLockedOut)
                attemptPasswordUnlock();
        });

        btnBiometric.setOnClickListener(v -> {
            if (BiometricHelper.isBiometricAvailable(this)) {
                BiometricHelper.authenticate(this, new BiometricHelper.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        deriveKeyAndNavigate(null, true);
                    }

                    @Override
                    public void onFailure() {
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showError(errorMessage);
                    }
                });
            }

        });
    }

    private void attemptPasswordUnlock() {
        String password = etPassword.getText().toString();

        if (password.isEmpty()) {
            showError("Enter your master password");
            return;
        }

        setLoadingState(true);

        new Thread(() -> verifyPasswordOnBackground(password)).start();
    }

    private void verifyPasswordOnBackground(String attempt) {
        try {
            boolean correct = KeyDerivation.verifyMasterPassword(attempt.toCharArray(), storedAuthSalt,
                    storedMasterHash);

            runOnUiThread(() -> {
                setLoadingState(false);
                if (correct) {
                    failedAttempts = 0;
                    deriveKeyAndNavigate(attempt, false);
                } else {
                    handleFailedAttempt();
                }
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                setLoadingState(false);
                showError(getString(R.string.error_generic));
            });
        }
    }

    private void deriveKeyAndNavigate(String masterPassword, boolean fromBiometric) {
        setLoadingState(true);

        new Thread(() -> {
            try {
                SecretKey sessionKey;

                if (fromBiometric) {
                    sessionKey = SessionManager.getSessionKey();
                    if (sessionKey == null) {
                        runOnUiThread(() -> {
                            setLoadingState(false);
                            showError("Please enter your master password to unlock");
                            setVisibility(View.GONE, divider, tvBiometricHint, btnBiometric);
                        });
                        return;
                    }
                } else {
                    byte[] saltBytes = android.util.Base64.decode(storedEncSalt, android.util.Base64.DEFAULT);
                    sessionKey = KeyDerivation.deriveKey(masterPassword.toCharArray(), saltBytes);
                    SessionManager.setSessionKey(sessionKey);
                }

                final SecretKey finalKey = sessionKey;

                runOnUiThread(() -> {
                    setLoadingState(false);
                    navigateToHome();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    showError(getString(R.string.error_generic));
                });
            }
        }).start();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleFailedAttempt() {
        failedAttempts++;
        showError(getString(R.string.error_wrong_password));

        if (failedAttempts >= MAX_ATTEMPTS) {
            startLockout();
        } else {
            int remaining = MAX_ATTEMPTS - failedAttempts;
            tvAttempts.setText(getString(R.string.login_attempts_remaining, remaining));
            tvAttempts.setVisibility(View.VISIBLE);
        }

        shakeView(etPassword.getParent() instanceof View ? (View) etPassword.getParent() : etPassword);

        etPassword.setText("");
    }

    private void startLockout() {
        isLockedOut = true;
        btnUnlock.setEnabled(false);
        etPassword.setEnabled(false);

        lockoutTimer = new CountDownTimer(LOCKOUT_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                tvAttempts.setText(getString(R.string.login_locked_out, secondsLeft));
                tvAttempts.setVisibility(View.VISIBLE);
                tvAttempts.setTextColor(ContextCompat.getColor(LoginActivity.this, R.color.cleanthes_error));
            }

            @Override
            public void onFinish() {
                isLockedOut = false;
                failedAttempts = 0;
                btnUnlock.setEnabled(true);
                etPassword.setEnabled(true);
                tvAttempts.setVisibility(View.GONE);
                tvError.setVisibility(View.GONE);
                etPassword.requestFocus();
            }
        };
        lockoutTimer.start();
    }

    private void togglePasswordVisibility(boolean visible) {
        int inputType = visible
                ? android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;

        etPassword.setInputType(inputType);
        etPassword.setSelection(etPassword.getText().length());
        btnTogglePassword.setImageResource(visible ? R.drawable.ic_eye_on : R.drawable.ic_eye_off);
    }

    private void setLoadingState(boolean loading) {
        btnUnlock.setEnabled(!loading && !isLockedOut);
        btnUnlock.setAlpha(loading ? 0.5f : 1.0f);
        btnUnlock.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private void setVisibility(int visibility, View... views) {
        for (View v : views)
            v.setVisibility(visibility);
    }

    private void shakeView(View view) {
        android.animation.ObjectAnimator shake = android.animation.ObjectAnimator.ofFloat(view, "translationX", 0, -16,
                16, -12, 12, -8, 8, -4, 4, 0);
        shake.setDuration(500);
        shake.start();
    }

    private SharedPreferences getEncryptedPrefs() throws Exception {
        MasterKey masterKey = new MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();

        return EncryptedSharedPreferences.create(
                this,
                SetupActivity.PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
