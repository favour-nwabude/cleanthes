package dev.favourdevlabs.cleanthes.ui.addedit;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository;
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager;
import dev.favourdevlabs.cleanthes.ui.home.HomeActivity;
import dev.favourdevlabs.cleanthes.utils.PasswordGenerator;

import javax.crypto.SecretKey;

public class AddEditActivity extends AppCompatActivity {

    public static final String EXTRA_ENTRY_ID = "extra_entry_id";
    public static final long NO_ENTRY_ID = -1L;

    private TextView tvScreenTitle;
    private ImageButton btnBack;
    private Button btnSave;
    private EditText etTitle;
    private EditText etUsername;
    private EditText etPassword;
    private ImageButton btnTogglePassword;
    private Button btnGenerate;
    private View[] strengthSegments;
    private EditText etWebsite;
    private Spinner spinnerCategory;
    private EditText etNotes;
    private CheckBox checkBoxFavorite;
    private TextView tvError;
    private Button btnDelete;

    private boolean isEditMode = false;
    private long entryId = NO_ENTRY_ID;
    private VaultEntry existingEntry = null;
    private boolean passwordVisible = false;
    private VaultRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         

        setContentView(R.layout.activity_add_edit);

        repository = VaultRepository.getInstance(this);

        bindViews();
        setupCategorySpinner();
        determineMode();
        attachListeners();
    }

    private void bindViews() {
        tvScreenTitle = findViewById(R.id.addedit_tv_title);
        btnBack = findViewById(R.id.addedit_btn_back);
        btnSave = findViewById(R.id.addedit_btn_save);
        etTitle = findViewById(R.id.addedit_et_title);
        etUsername = findViewById(R.id.addedit_et_username);
        etPassword = findViewById(R.id.addedit_et_password);
        btnTogglePassword = findViewById(R.id.addedit_btn_toggle_password);
        btnGenerate = findViewById(R.id.addedit_btn_generate);
        etWebsite = findViewById(R.id.addedit_et_website);
        spinnerCategory = findViewById(R.id.addedit_spinner_category);
        etNotes = findViewById(R.id.addedit_et_notes);
        checkBoxFavorite = findViewById(R.id.addedit_checkbox_favorite);
        tvError = findViewById(R.id.addedit_tv_error);
        btnDelete = findViewById(R.id.btn_delete);

        btnSave.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));

        strengthSegments = new View[] {
                findViewById(R.id.addedit_seg1),
                findViewById(R.id.addedit_seg2),
                findViewById(R.id.addedit_seg3),
                findViewById(R.id.addedit_seg4),
                findViewById(R.id.addedit_seg5)
        };
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.categories_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void determineMode() {
        entryId = getIntent().getLongExtra(EXTRA_ENTRY_ID, NO_ENTRY_ID);

        if (entryId != NO_ENTRY_ID) {
            isEditMode = true;
            tvScreenTitle.setText(getString(R.string.addedit_title_edit));
            btnDelete.setVisibility(View.VISIBLE);
            loadExistingEntry();
        } else {
            isEditMode = false;
            tvScreenTitle.setText(getString(R.string.addedit_title_new));
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void loadExistingEntry() {
        SecretKey key = SessionManager.getSessionKey();
        if (key == null) {
            finish();
            return;
        }

        new Thread(() -> {
            try {
                VaultEntry entry = repository.getEntryById(entryId, key);
                runOnUiThread(() -> {
                    if (entry != null) {
                        existingEntry = entry;
                        populateFields(entry);
                    } else {
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(this::finish);
            }
        }).start();
    }

    private void populateFields(VaultEntry entry) {
        etTitle.setText(entry.getTitle());
        etUsername.setText(entry.getUsername());
        etPassword.setText(entry.getEncryptedPassword());
        etWebsite.setText(entry.getWebsite() != null ? entry.getWebsite() : "");
        etNotes.setText(entry.getNotes() != null ? entry.getNotes() : "");
        checkBoxFavorite.setChecked(entry.isFavorite());

        ArrayAdapter adapter = (ArrayAdapter) spinnerCategory.getAdapter();
        int pos = adapter.getPosition(entry.getCategory());
        if (pos >= 0)
            spinnerCategory.setSelection(pos);

        updateStrengthBar(entry.getEncryptedPassword());
    }

    private void attachListeners() {

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> attemptSave());

        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            togglePasswordVisibility(passwordVisible);
        });

        btnGenerate.setOnClickListener(v -> showGeneratorDialog());

        btnDelete.setOnClickListener(v -> confirmDelete());

        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStrengthBar(s.toString());
                hideError();
            }
        });
    }

    private void attemptSave() {
        String title = etTitle.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String website = etWebsite.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String notes = etNotes.getText().toString().trim();
        boolean fav = checkBoxFavorite.isChecked();

        if (title.isEmpty()) {
            showError("Title is required");
            etTitle.requestFocus();
            return;
        }
        if (username.isEmpty()) {
            showError("Username or Email is required");
            etUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Password is required");
            etPassword.requestFocus();
            return;
        }

        SecretKey key = SessionManager.getSessionKey();
        if (key == null) {
            finish();
            return;
        }

        setFormEnabled(false);

        new Thread(() -> {
            try {
                if (isEditMode && existingEntry != null) {
                    existingEntry.setTitle(title);
                    existingEntry.setUsername(username);
                    existingEntry.setWebsite(website.isEmpty() ? null : website);
                    existingEntry.setCategory(category);
                    existingEntry.setNotes(notes.isEmpty() ? null : notes);
                    existingEntry.setFavorite(fav);
                    repository.updateEntry(existingEntry, password, key);
                } else {
                    repository.addEntry(
                            title, username, password,
                            website.isEmpty() ? null : website,
                            category,
                            notes.isEmpty() ? null : notes,
                            fav, key);
                }
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    showError("Failed to save entry. Please try again");
                });
            }
        }).start();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete \""
                        + etTitle.getText().toString() + "\"?\n\nThis cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> performDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete() {
        if (!isEditMode || existingEntry == null)
            return;

        new Thread(() -> {
            try {
                repository.deleteEntry(existingEntry.getId());
                runOnUiThread(this::finish);
            } catch (Exception e) {
                runOnUiThread(() -> showError("Failed to delete entry"));
            }
        }).start();
    }

    private void showGeneratorDialog() {
        final int[] length = { PasswordGenerator.DEFAULT_LENGTH };
        final boolean[] uppercase = { true };
        final boolean[] lowercase = { true };
        final boolean[] digits = { true };
        final boolean[] special = { true };

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = dpToPx(20);
        layout.setPadding(pad, pad, pad, 0);
        layout.setBackgroundColor(getColor(R.color.cleanthes_surface));

        android.widget.LinearLayout lengthRow = new android.widget.LinearLayout(this);
        lengthRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        lengthRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvLengthLabel = new TextView(this);
        tvLengthLabel.setText("Length: " + length[0]);
        tvLengthLabel.setTextColor(getColor(R.color.citadel_gold));
        tvLengthLabel.setTextSize(15);
        tvLengthLabel.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button btnMinus = new Button(this);
        btnMinus.setText("−");
        btnMinus.setTextSize(16);
        btnMinus.setTextColor(getColor(R.color.cleanthes_black));
        btnMinus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.citadel_gold)));

        Button btnPlus = new Button(this);
        btnPlus.setText("+");
        btnPlus.setTextSize(16);
        btnPlus.setTextColor(getColor(R.color.cleanthes_black));
        btnPlus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.citadel_gold)));

        lengthRow.addView(tvLengthLabel);
        lengthRow.addView(btnMinus);
        lengthRow.addView(btnPlus);
        layout.addView(lengthRow);

        CheckBox cbUpper = makeDialogCheckbox("Uppercase (A-Z)", true);
        CheckBox cbLower = makeDialogCheckbox("Lowercase (a-z)", true);
        CheckBox cbDigits = makeDialogCheckbox("Digits (0-9)", true);
        CheckBox cbSpecial = makeDialogCheckbox("Special (!@#$...)", true);
        layout.addView(cbUpper);
        layout.addView(cbLower);
        layout.addView(cbDigits);
        layout.addView(cbSpecial);

        TextView tvPreview = new TextView(this);
        tvPreview.setTextColor(getColor(R.color.citadel_gold));
        tvPreview.setTextSize(14);
        tvPreview.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvPreview.setPadding(0, dpToPx(12), 0, dpToPx(4));
        tvPreview.setText(PasswordGenerator.generate(
                length[0], true, true, true, true));
        layout.addView(tvPreview);

        Button btnRegan = new Button(this);
        btnRegan.setText("↻  REGENERATE");
        btnRegan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.cleanthes_border)));
        btnRegan.setTextColor(getColor(R.color.citadel_gold));
        layout.addView(btnRegan);

        Runnable regenerate = () -> {
            try {
                String p = PasswordGenerator.generate(
                        length[0],
                        uppercase[0], lowercase[0],
                        digits[0], special[0]);
                tvPreview.setText(p);
            } catch (IllegalArgumentException e) {
                tvPreview.setText("Select at least one category");
            }
        };

        btnMinus.setOnClickListener(v -> {
            if (length[0] > 8) {
                length[0]--;
                tvLengthLabel.setText("Length: " + length[0]);
                regenerate.run();
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (length[0] < 32) {
                length[0]++;
                tvLengthLabel.setText("Length: " + length[0]);
                regenerate.run();
            }
        });

        cbUpper.setOnCheckedChangeListener((b, c) -> {
            uppercase[0] = c;
            regenerate.run();
        });
        cbLower.setOnCheckedChangeListener((b, c) -> {
            lowercase[0] = c;
            regenerate.run();
        });
        cbDigits.setOnCheckedChangeListener((b, c) -> {
            digits[0] = c;
            regenerate.run();
        });
        cbSpecial.setOnCheckedChangeListener((b, c) -> {
            special[0] = c;
            regenerate.run();
        });
        btnRegan.setOnClickListener(v -> regenerate.run());

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Forge a key")
                .setView(layout)
                .setPositiveButton("Commit to this", (dialog, which) -> {
                    String generated = tvPreview.getText().toString();
                    if (!generated.equals("Select at least one category")) {
                        etPassword.setText(generated);
                        updateStrengthBar(generated);

                        if (!passwordVisible) {
                            passwordVisible = true;
                            togglePasswordVisibility(true);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private CheckBox makeDialogCheckbox(String label, boolean checked) {
        CheckBox cb = new CheckBox(this);
        cb.setText(label);
        cb.setChecked(checked);
        cb.setTextColor(getColor(R.color.cleanthes_text_secondary));
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(
                getColor(R.color.citadel_gold)));
        return cb;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void updateStrengthBar(String password) {
        int score = PasswordGenerator.evaluateStrength(password);
        int[] colorRes = {
                R.color.cleanthes_strength_empty,
                R.color.cleanthes_strength_very_weak,
                R.color.cleanthes_strength_weak,
                R.color.cleanthes_strength_fair,
                R.color.cleanthes_strength_strong,
                R.color.cleanthes_strength_very_strong
        };

        int activeColor = ContextCompat.getColor(this, colorRes[score]);
        int emptyColor = ContextCompat.getColor(this, R.color.cleanthes_strength_empty);

        for (int i = 0; i < strengthSegments.length; i++) {
            strengthSegments[i].setBackgroundColor(i < score ? activeColor : emptyColor);
        }
    }

    private void togglePasswordVisibility(boolean visible) {
        int inputType = visible
                ? android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
        etPassword.setInputType(inputType);
        etPassword.setSelection(etPassword.getText().length());
        btnTogglePassword.setImageResource(
                visible ? R.drawable.ic_eye_on : R.drawable.ic_eye_off);
    }

    private void setFormEnabled(boolean enabled) {
        btnSave.setEnabled(enabled);
        btnSave.setAlpha(enabled ? 1f : 0.5f);
        btnSave.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));
        etTitle.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        etUsername.setEnabled(enabled);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
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
