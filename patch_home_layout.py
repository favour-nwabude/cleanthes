with open('./app/src/main/res/layout/activity_home.xml', 'r') as f:
    s = f.read()

# 1. Toolbar title — textAllCaps + letterSpacing to match other screens
s = s.replace(
    '                    android:textSize="22sp"\n                    android:fontFamily="sans-serif-black" />',
    '                    android:textSize="22sp"\n                    android:fontFamily="sans-serif-black"\n                    android:textAllCaps="true"\n                    android:letterSpacing="0.1" />'
)

# 2. Empty state icon — gold tint
s = s.replace(
    '            app:tint="@color/cleanthes_text_primary" />',
    '            app:tint="@color/citadel_gold" />'
)

# 3. Empty state title — stoic copy from strings
s = s.replace(
    '                android:text="Empty Vault"',
    '                android:text="@string/home_empty_title"'
)

# 4. Empty state subtitle — stoic copy from strings
s = s.replace(
    '                android:text="Tap + to secure a password"',
    '                android:text="@string/home_empty_subtitle"'
)

# 5. SearchView hint
s = s.replace(
    '            app:queryHint="Search vault..."',
    '            app:queryHint="@string/home_search_hint"'
)

# 6. FAB — gold
s = s.replace(
    '        app:backgroundTint="@color/cleanthes_accent"',
    '        app:backgroundTint="@color/citadel_gold"'
)

with open('./app/src/main/res/layout/activity_home.xml', 'w') as f:
    f.write(s)

print('Done.')
