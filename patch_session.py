with open('./app/src/main/java/dev/favourdevlabs/cleanthes/ui/home/HomeActivity.java', 'r') as f:
    src = f.read()

# 1. Add isLaunchingChild flag
src = src.replace(
    '    private boolean searchVisible = false;',
    '    private boolean searchVisible = false;\n    private boolean isLaunchingChild = false;'
)

# 2. Flag set before launching AddEditActivity
src = src.replace(
    '        fabAdd.setOnClickListener(v -> {\n            Intent intent = new Intent(this, AddEditActivity.class);\n            startActivity(intent);\n        });',
    '        fabAdd.setOnClickListener(v -> {\n            Intent intent = new Intent(this, AddEditActivity.class);\n            isLaunchingChild = true;\n            startActivity(intent);\n        });'
)

# 3. Flag set before launching DetailActivity
src = src.replace(
    '        Intent intent = new Intent(this, DetailActivity.class);\n        intent.putExtra(EXTRA_ENTRY_ID, entry.getId());\n        startActivity(intent);',
    '        Intent intent = new Intent(this, DetailActivity.class);\n        intent.putExtra(EXTRA_ENTRY_ID, entry.getId());\n        isLaunchingChild = true;\n        startActivity(intent);'
)

# 4. Guard clearSession — only fires when app actually goes to background
src = src.replace(
    '    @Override\n    protected void onStop() {\n        super.onStop();\n        SessionManager.clearSession();\n    }',
    '    @Override\n    protected void onStop() {\n        super.onStop();\n        if (!isLaunchingChild) {\n            SessionManager.clearSession();\n        }\n        isLaunchingChild = false;\n    }'
)

with open('./app/src/main/java/dev/favourdevlabs/cleanthes/ui/home/HomeActivity.java', 'w') as f:
    f.write(src)

print('Done.')
