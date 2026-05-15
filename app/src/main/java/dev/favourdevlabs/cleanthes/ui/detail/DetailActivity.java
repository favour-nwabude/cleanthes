package dev.favourdevlabs.cleanthes.ui.detail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository;
import dev.favourdevlabs.cleanthes.ui.addedit.AddEditActivity;
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager;

import java.util.concurrent.Executors;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_ENTRY_ID = "extra_entry_id";

    private TextView tvToolbarTitle;
    private ImageButton btnBack;
    private Button btnEdit;
    private TextView tvCategory;
    private TextView tvUsername;
    private TextView tvPassword;
    private ImageButton btnTogglePassword;
    private ImageButton btnCopyUsername;
    private ImageButton btnCopyPassword;
    private TextView labelWebsite;
    private TextView tvWebsite;
    private TextView labelNotes;
    private TextView tvNotes;
    private TextView tvFavorite;

    private VaultRepository repository;
    private String plainPassword = "";
    private boolean passwordVisible = false;
    private long entryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        repository = VaultRepository.getInstance(this);
        bindViews();
        attachListeners();

        entryId = getIntent().getLongExtra(EXTRA_ENTRY_ID, -1);

        if (entryId == -1 || !SessionManager.isUnlocked()) {
            Toast.makeText(this, "Session invalid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEntry(entryId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Keep session alive + reload in case user just edited this entry
        SessionManager.refreshSession();
        if (entryId != -1 && SessionManager.isUnlocked()) {
            loadEntry(entryId);
        }
    }

    private void bindViews() {
        tvToolbarTitle = findViewById(R.id.detail_tv_toolbar_title);
        btnBack = findViewById(R.id.detail_btn_back);
        btnEdit = findViewById(R.id.detail_btn_edit);
        tvCategory = findViewById(R.id.detail_tv_category);
        tvUsername = findViewById(R.id.detail_tv_username);
        tvPassword = findViewById(R.id.detail_tv_password);
        btnTogglePassword = findViewById(R.id.detail_btn_toggle_password);
        btnCopyUsername = findViewById(R.id.detail_btn_copy_username);
        btnCopyPassword = findViewById(R.id.detail_btn_copy_password);
        labelWebsite = findViewById(R.id.detail_label_website);
        tvWebsite = findViewById(R.id.detail_tv_website);
        labelNotes = findViewById(R.id.detail_label_notes);
        tvNotes = findViewById(R.id.detail_tv_notes);
        tvFavorite = findViewById(R.id.detail_tv_favorite);

        // Force gold — Material Button overrides tint when disabled
        btnEdit.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));
    }

    private void attachListeners() {

        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditActivity.class);
            intent.putExtra(AddEditActivity.EXTRA_ENTRY_ID, entryId);
            startActivity(intent);
        });

        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            tvPassword.setText(passwordVisible ? plainPassword : "••••••••••••");
            btnTogglePassword.setImageResource(
                    passwordVisible ? R.drawable.ic_eye_on : R.drawable.ic_eye_off);
        });

        btnCopyUsername.setOnClickListener(v -> copyToClipboard("username", tvUsername.getText().toString()));

        btnCopyPassword.setOnClickListener(v -> copyToClipboard("password", plainPassword));
    }

    private void loadEntry(long id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                VaultEntry entry = repository.getEntryById(id, SessionManager.getSessionKey());
                if (entry != null) {
                    runOnUiThread(() -> populateUI(entry));
                } else {
                    runOnUiThread(this::finish);
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading entry", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void populateUI(VaultEntry entry) {
        // Reset password visibility on reload
        passwordVisible = false;
        plainPassword = entry.getEncryptedPassword();

        tvToolbarTitle.setText(entry.getTitle());
        tvCategory.setText(entry.getCategory());
        tvUsername.setText(entry.getUsername());
        tvPassword.setText("••••••••••••");
        btnTogglePassword.setImageResource(R.drawable.ic_eye_off);

        // Website — only shown when present
        boolean hasWebsite = entry.getWebsite() != null && !entry.getWebsite().isEmpty();
        labelWebsite.setVisibility(hasWebsite ? View.VISIBLE : View.GONE);
        tvWebsite.setVisibility(hasWebsite ? View.VISIBLE : View.GONE);
        if (hasWebsite)
            tvWebsite.setText(entry.getWebsite());

        // Notes — only shown when present
        boolean hasNotes = entry.getNotes() != null && !entry.getNotes().isEmpty();
        labelNotes.setVisibility(hasNotes ? View.VISIBLE : View.GONE);
        tvNotes.setVisibility(hasNotes ? View.VISIBLE : View.GONE);
        if (hasNotes)
            tvNotes.setText(entry.getNotes());

        // Priority indicator
        tvFavorite.setVisibility(entry.isFavorite() ? View.VISIBLE : View.GONE);
    }

    private void copyToClipboard(String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}
