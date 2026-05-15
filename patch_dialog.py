with open('./app/src/main/java/dev/favourdevlabs/cleanthes/ui/addedit/AddEditActivity.java', 'r') as f:
    src = f.read()

# 1. Dark theme dialog — MaterialAlertDialogBuilder respects app theme
src = src.replace(
    'new AlertDialog.Builder(this)\n                .setTitle("Generate password")',
    'new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)\n                .setTitle("Forge a key")'
)

# 2. Dark content background
src = src.replace(
    '        layout.setPadding(pad, pad, pad, 0);',
    '        layout.setPadding(pad, pad, pad, 0);\n        layout.setBackgroundColor(getColor(R.color.cleanthes_surface));'
)

# 3. Gold minus button
src = src.replace(
    '        Button btnMinus = new Button(this);\n        btnMinus.setText("-");\n        btnMinus.setTextSize(16);',
    '        Button btnMinus = new Button(this);\n        btnMinus.setText("−");\n        btnMinus.setTextSize(16);\n        btnMinus.setTextColor(getColor(R.color.cleanthes_black));\n        btnMinus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.citadel_gold)));'
)

# 4. Gold plus button
src = src.replace(
    '        Button btnPlus = new Button(this);\n        btnPlus.setText("+");\n        btnPlus.setTextSize(16);',
    '        Button btnPlus = new Button(this);\n        btnPlus.setText("+");\n        btnPlus.setTextSize(16);\n        btnPlus.setTextColor(getColor(R.color.cleanthes_black));\n        btnPlus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.citadel_gold)));'
)

# 5. Gold REGENERATE button
src = src.replace(
    '        btnRegan.setText("↻ Regenerate");\n        btnRegan.setTextColor(getColor(R.color.cleanthes_text_secondary));',
    '        btnRegan.setText("↻  REGENERATE");\n        btnRegan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.cleanthes_border)));\n        btnRegan.setTextColor(getColor(R.color.citadel_gold));'
)

# 6. Length label — gold text
src = src.replace(
    '        tvLengthLabel.setTextColor(getColor(R.color.cleanthes_text_primary));',
    '        tvLengthLabel.setTextColor(getColor(R.color.citadel_gold));'
)

# 7. Positive button copy — stoic
src = src.replace(
    '.setPositiveButton("Use this password",',
    '.setPositiveButton("Commit to this",'
)

with open('./app/src/main/java/dev/favourdevlabs/cleanthes/ui/addedit/AddEditActivity.java', 'w') as f:
    f.write(src)
print('Done.')
