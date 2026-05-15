with open('./app/src/main/java/dev/favourdevlabs/cleanthes/ui/auth/SetupActivity.java', 'r') as f:
    src = f.read()

# 1. Imports
src = src.replace(
    'import android.widget.Button;',
    'import android.content.res.ColorStateList;\nimport android.widget.Button;\nimport android.widget.CheckBox;'
)

# 2. Field
src = src.replace(
    '    private boolean passwordVisible = false;',
    '    private CheckBox cbAcknowledge;\n\n    private boolean passwordVisible = false;'
)

# 3. Bind + initial button state
src = src.replace(
    '        btnProgressBar = findViewById(R.id.setup_progress);',
    '        btnProgressBar = findViewById(R.id.setup_progress);\n        cbAcknowledge = findViewById(R.id.setup_cb_acknowledge);\n\n        btnCreate.setEnabled(false);\n        btnCreate.setAlpha(0.35f);\n        btnCreate.setBackgroundTintList(\n                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));'
)

# 4. Checkbox listener
src = src.replace(
    '        btnCreate.setOnClickListener(v -> attemptSetup());',
    '        cbAcknowledge.setOnCheckedChangeListener((buttonView, isChecked) -> {\n            btnCreate.setEnabled(isChecked);\n            btnCreate.setAlpha(isChecked ? 1.0f : 0.35f);\n            btnCreate.setBackgroundTintList(\n                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));\n        });\n\n        btnCreate.setOnClickListener(v -> attemptSetup());'
)

# 5. setLoadingState
src = src.replace(
    '    private void setLoadingState(boolean loading) {\n        btnCreate.setEnabled(!loading);\n        btnCreate.setAlpha(loading ? 0.5f : 1.0f);\n        btnProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);\n    }',
    '    private void setLoadingState(boolean loading) {\n        cbAcknowledge.setEnabled(!loading);\n        btnCreate.setEnabled(!loading);\n        btnCreate.setAlpha(loading ? 0.35f : 1.0f);\n        btnCreate.setBackgroundTintList(\n                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));\n        btnProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);\n    }'
)

with open('./app/src/main/java/dev/favourdevlabs/cleanthes/ui/auth/SetupActivity.java', 'w') as f:
    f.write(src)

print("Done. All 5 patches applied.")
