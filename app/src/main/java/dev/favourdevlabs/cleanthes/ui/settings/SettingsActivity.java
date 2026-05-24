package dev.favourdevlabs.cleanthes.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.autofill.AutofillManager;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import dev.favourdevlabs.cleanthes.R;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "cleanthes_prefs";
    public static final String KEY_AUTO_LOCK = "auto_lock_minutes";
    public static final String KEY_CLIPBOARD = "clipboard_clear_seconds";

    private SharedPreferences prefs;
    private AutofillManager autofillManager;

    // Auto-lock options in minutes. -1 means never.
    private static final int[] LOCK_VALUES = { 1, 5, 15, -1 };
    private static final String[] LOCK_LABELS = { "1 min", "5 min", "15 min", "Never" };

    // Clipboard options in seconds. -1 means off.
    private static final int[] CLIP_VALUES = { 30, 60, -1 };
    private static final String[] CLIP_LABELS = { "30s", "60s", "Off" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        autofillManager = getSystemService(AutofillManager.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        bindAutoLock();
        bindClipboard();
        bindAutofill();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh autofill status — user may have just returned
        // from Android's system autofill settings screen
        bindAutofill();
    }

    private void bindAutoLock() {
        TextView valueView = findViewById(R.id.value_auto_lock);

        int current = prefs.getInt(KEY_AUTO_LOCK, 5);
        valueView.setText(labelForLock(current));

        findViewById(R.id.row_auto_lock).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Auto-lock after")
                    .setItems(LOCK_LABELS, (dialog, which) -> {
                        int chosen = LOCK_VALUES[which];
                        prefs.edit().putInt(KEY_AUTO_LOCK, chosen).apply();
                        valueView.setText(LOCK_LABELS[which]);
                    })
                    .show();
        });
    }

    private void bindClipboard() {
        TextView valueView = findViewById(R.id.value_clipboard);

        int current = prefs.getInt(KEY_CLIPBOARD, 30);
        valueView.setText(labelForClip(current));

        findViewById(R.id.row_clipboard).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear clipboard after")
                    .setItems(CLIP_LABELS, (dialog, which) -> {
                        int chosen = CLIP_VALUES[which];
                        prefs.edit().putInt(KEY_CLIPBOARD, chosen).apply();
                        valueView.setText(CLIP_LABELS[which]);
                    })
                    .show();
        });
    }

    private void bindAutofill() {
        TextView statusView = findViewById(R.id.autofill_status_text);
        boolean active = autofillManager.hasEnabledAutofillServices();

        if (active) {
            statusView.setText("Active ✓");
            statusView.setTextColor(getColor(R.color.cleanthes_success));
            // Row is not tappable when already active
            findViewById(R.id.row_autofill).setOnClickListener(null);
        } else {
            statusView.setText("Enable ›");
            statusView.setTextColor(getColor(R.color.cleanthes_accent));
            findViewById(R.id.row_autofill).setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        }
    }

    private String labelForLock(int minutes) {
        if (minutes == -1)
            return "Never";
        return minutes + " min";
    }

    private String labelForClip(int seconds) {
        if (seconds == -1)
            return "Off";
        return seconds + "s";
    }
}
