with open('./app/src/main/java/dev/favourdevlabs/cleanthes/ui/addedit/AddEditActivity.java', 'r') as f:
    src = f.read()

# 1. Add ColorStateList import
src = src.replace(
    'import android.content.DialogInterface;',
    'import android.content.DialogInterface;\nimport android.content.res.ColorStateList;'
)

# 2. Force gold on save button after binding
src = src.replace(
    '        btnDelete = findViewById(R.id.btn_delete);',
    '        btnDelete = findViewById(R.id.btn_delete);\n\n        btnSave.setBackgroundTintList(\n                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));'
)

# 3. Use string resources for screen title in determineMode()
src = src.replace(
    '            tvScreenTitle.setText("Edit Entry");',
    '            tvScreenTitle.setText(getString(R.string.addedit_title_edit));'
)
src = src.replace(
    '            tvScreenTitle.setText("New Entry");',
    '            tvScreenTitle.setText(getString(R.string.addedit_title_new));'
)

# 4. Re-assert gold in setFormEnabled so loading state doesn't go gray
src = src.replace(
    '        btnSave.setEnabled(enabled);\n        btnSave.setAlpha(enabled ? 1f : 0.5f);',
    '        btnSave.setEnabled(enabled);\n        btnSave.setAlpha(enabled ? 1f : 0.5f);\n        btnSave.setBackgroundTintList(\n                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold)));'
)

# 5. Dialog checkboxes — gold tint
src = src.replace(
    '        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(\n                getColor(R.color.cleanthes_accent)));',
    '        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(\n                getColor(R.color.citadel_gold)));'
)

# 6. Generator dialog preview text — gold
src = src.replace(
    '        tvPreview.setTextColor(getColor(R.color.cleanthes_accent));',
    '        tvPreview.setTextColor(getColor(R.color.citadel_gold));'
)

with open('./app/src/main/java/dev/favourdevlabs/cleanthes/ui/addedit/AddEditActivity.java', 'w') as f:
    f.write(src)

print('Done.')
