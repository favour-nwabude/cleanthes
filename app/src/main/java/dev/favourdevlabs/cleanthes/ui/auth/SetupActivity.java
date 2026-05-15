package dev.favourdevlabs.cleanthes.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.content.res.ColorStateList;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Base64;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.security.KeyDerivation;
import dev.favourdevlabs.cleanthes.ui.home.HomeActivity;

public class SetupActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "vault_secure_prefs";
    public static final String KEY_VAULT_EXISTS = "vault_exists";
    public static final String KEY_AUTH_SALT = "auth_salt";
    public static final String KEY_ENC_SALT = "enc_salt";
    public static final String KEY_MASTER_HASH = "master_hash";
    public static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_FAILED_ATTEMPTS = 5;

    public static final int STRENGTH_VERY_WEAK = 0;
    public static final int STRENGTH_WEAK = 1;
    public static final int STRENGTH_FAIR = 2;
    public static final int STRENGTH_STRONG = 3;
    public static final int STRENGTH_VERY_STRONG = 4;

    private EditText etPassword;
    private EditText etConfirm;
    private ImageButton btnTogglePassword;
    private ImageButton btnToggleConfirm;
    private View[] strengthSegments; // 5 bar segments.
    private TextView tvStrengthLabel;
    private TextView tvMatchIndicator;
    private TextView tvError;
    private Button btnCreate;
    private ProgressBar btnProgressBar;

    private CheckBox cbAcknowledge;

    private boolean passwordVisible = false;
    private boolean confirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

                // Vault guard — if vault already exists, never show setup again
        try {
            SharedPreferences vaultCheck = getEncryptedPrefs();
            if (vaultCheck.getBoolean(KEY_VAULT_EXISTS, false)) {
                Intent toLogin = new Intent(this, LoginActivity.class);
                toLogin.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(toLogin);
                finish();
                return;
            }
        } catch (Exception ignored) { }

        setContentView(R.layout.activity_setup);

        bindViews();
        attachListeners();
    }

    private void bindViews() {
        etPassword = findViewById(R.id.setup_et_password);
        etConfirm = findViewById(R.id.setup_et_confirm);
        btnTogglePassword = findViewById(R.id.setup_btn_toggle_password);
        btnToggleConfirm = findViewById(R.id.setup_btn_confirm);
        tvStrengthLabel = findViewById(R.id.setup_tv_strength_label);
        tvMatchIndicator = findViewById(R.id.setup_tv_match_indicator);
        tvError = findViewById(R.id.setup_tv_error);
        btnCreate = findViewById(R.id.setup_btn_create);
        btnProgressBar = findViewById(R.id.setup_progress);
        cbAcknowledge = findViewById(R.id.setup_cb_acknowledge);

        btnCreate.setEnabled(false);
        btnCreate.setAlpha(0.35f);
        btnCreate.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));

        strengthSegments = new View[] {
                findViewById(R.id.setup_strength_seg1),
                findViewById(R.id.setup_strength_seg2),
                findViewById(R.id.setup_strength_seg3),
                findViewById(R.id.setup_strength_seg4),
                findViewById(R.id.setup_strength_seg5)
        };
    }

    private void attachListeners() {

        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrengthBar(s.toString());
                updateMatchIndicator();
                hideError();
            }
        });

        etConfirm.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateMatchIndicator();
                hideError();
            }
        });

        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            togglePasswordVisibility(etPassword, btnTogglePassword, passwordVisible);
        });

        btnToggleConfirm.setOnClickListener(v -> {
            confirmVisible = !confirmVisible;
            togglePasswordVisibility(etConfirm, btnToggleConfirm, confirmVisible);
        });

        etConfirm.setOnEditorActionListener((v, actionId, event) -> {
            attemptSetup();
            return true;
        });

        cbAcknowledge.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnCreate.setEnabled(isChecked);
            btnCreate.setAlpha(isChecked ? 1.0f : 0.35f);
            btnCreate.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));
        });

        btnCreate.setOnClickListener(v -> attemptSetup());
    }

    private void attemptSetup() {
        String password = etPassword.getText().toString();
        String confirm = etConfirm.getText().toString();

        if (password.length() < MIN_PASSWORD_LENGTH) {
            showError(getString(R.string.error_password_too_short));
            return;
        }

        if (!password.matches(".*\\d.*")) {
            showError(getString(R.string.error_password_no_number));
            return;
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?].*")) {
            showError(getString(R.string.error_password_no_special));
            return;
        }

        if (!password.equals(confirm)) {
            showError(getString(R.string.error_passwords_no_match));
            return;
        }

        setLoadingState(true);

        final String finalPassword = password;
        new Thread(() -> performSetup(finalPassword)).start();
    }

    private void performSetup(String masterPassword) {
        try {
            // 1. Generate the hash and salt in one go
            // The method generates its own salt internally and returns a StoredHash object
            KeyDerivation.StoredHash storedHash = KeyDerivation.hashPassword(masterPassword.toCharArray());

            // 2. Generate the encryption salt (used for your database/files, not auth)
            byte[] encSaltBytes = KeyDerivation.generateSalt();
            String encSalt = Base64.encodeToString(encSaltBytes, Base64.NO_WRAP);

            SharedPreferences prefs = getEncryptedPrefs();
            prefs.edit()
                    .putBoolean(KEY_VAULT_EXISTS, true)
                    // Get the salt that was generated inside hashPassword()
                    .putString(KEY_AUTH_SALT, storedHash.saltBase64)
                    .putString(KEY_ENC_SALT, encSalt)
                    // Get the hash string from the object
                    .putString(KEY_MASTER_HASH, storedHash.hashBase64)
                    .putBoolean(KEY_BIOMETRIC_ENABLED, false)
                    .apply();

            runOnUiThread(this::navigateHome);

        } catch (Exception e) {
            runOnUiThread(() -> {
                setLoadingState(false);
                showError(getString(R.string.error_setup_failed));
            });
        }
    }

    private void navigateHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateStrengthBar(String password) {
        int score = computeStrengthScore(password);
        int[] strengthColors = getStrengthColors(score);
        String strengthLabel = getStrengthLabel(score);

        for (int i = 0; i < strengthSegments.length; i++) {
            int color = (i < score)
                    ? strengthColors[0]
                    : ContextCompat.getColor(this, R.color.cleanthes_strength_empty);
            strengthSegments[i].setBackgroundColor(color);
        }

        tvStrengthLabel.setText(strengthLabel);
        tvStrengthLabel.setTextColor(strengthColors[0]);
    }

    private int computeStrengthScore(String password) {
        if (password.isEmpty())
            return 0;
        int score = 0;
        if (password.length() >= MIN_PASSWORD_LENGTH)
            score++;
        if (password.matches(".*[A-Z].*"))
            score++;
        if (password.matches(".*\\d.*"))
            score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|].*"))
            score++;
        if (password.length() >= 16)
            score++;
        return score;
    }

    private int[] getStrengthColors(int score) {
        int colorRes;
        switch (score) {
            case 1:
                colorRes = R.color.cleanthes_strength_very_weak;
                break;
            case 2:
                colorRes = R.color.cleanthes_strength_weak;
                break;
            case 3:
                colorRes = R.color.cleanthes_strength_fair;
                break;
            case 4:
                colorRes = R.color.cleanthes_strength_strong;
                break;
            case 5:
                colorRes = R.color.cleanthes_strength_very_strong;
                break;
            default:
                colorRes = R.color.cleanthes_strength_empty;
                break;
        }
        return new int[] { ContextCompat.getColor(this, colorRes) };
    }

    private String getStrengthLabel(int score) {
        switch (score) {
            case 1:
                return getString(R.string.cleanthes_strength_very_weak);
            case 2:
                return getString(R.string.cleanthes_strength_weak);
            case 3:
                return getString(R.string.cleanthes_strength_fair);
            case 4:
                return getString(R.string.cleanthes_strength_strong);
            case 5:
                return getString(R.string.cleanthes_strength_very_strong);
            default:
                return "";
        }
    }

    private void updateMatchIndicator() {
        String password = etPassword.getText().toString();
        String confirm = etConfirm.getText().toString();

        if (confirm.isEmpty()) {
            tvMatchIndicator.setText("");
            return;
        }

        if (password.equals(confirm)) {
            tvMatchIndicator.setText("✓ Passwords match");
            tvMatchIndicator.setTextColor(
                    ContextCompat.getColor(this, R.color.cleanthes_success));
        } else {
            tvMatchIndicator.setText("✗ Passwords do not match");
            tvMatchIndicator.setTextColor(
                    ContextCompat.getColor(this, R.color.cleanthes_error));
        }
    }

    private void togglePasswordVisibility(EditText field, ImageButton toggle, boolean visible) {
        int inputType = visible
                ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;

        field.setInputType(inputType);
        field.setSelection(field.getText().length());

        toggle.setImageResource(visible ? R.drawable.ic_eye_on : R.drawable.ic_eye_off);
    }

    private void setLoadingState(boolean loading) {
        cbAcknowledge.setEnabled(!loading);
        btnCreate.setEnabled(!loading);
        btnCreate.setAlpha(loading ? 0.35f : 1.0f);
        btnCreate.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));
        btnProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private SharedPreferences getEncryptedPrefs() throws Exception {
        MasterKey masterKey = new MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                this,
                PREFS_NAME,
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
