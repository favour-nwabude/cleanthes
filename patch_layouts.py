import re

# ── activity_add_edit.xml ──────────────────────────────────────────
with open('./app/src/main/res/layout/activity_add_edit.xml', 'r') as f:
    s = f.read()

# 1. Fix textSize bug: 18dp → 18sp, add textAllCaps + letterSpacing
s = s.replace(
    'android:textSize="18dp"\n                android:textStyle="bold"',
    'android:textSize="18sp"\n                android:textStyle="bold"\n                android:textAllCaps="true"\n                android:letterSpacing="0.1"'
)

# 2. Save button — gold, COMMIT, letterSpacing
s = s.replace(
    'android:text="Save"\n                android:textColor="@color/cleanthes_black"\n                android:textSize="13sp"\n                android:textStyle="bold"\n                android:backgroundTint="@color/cleanthes_accent"',
    'android:text="@string/addedit_btn_save"\n                android:textColor="@color/cleanthes_black"\n                android:textSize="13sp"\n                android:textStyle="bold"\n                android:letterSpacing="0.1"\n                android:backgroundTint="@color/citadel_gold"'
)

# 3. Generate button — FORGE, gold filled, no emoji
s = s.replace(
    'android:text="\u26a1 Generate Password"\n            android:textColor="@color/cleanthes_accent"\n            android:textSize="13sp"\n            android:background="@drawable/bg_generate_button"',
    'android:text="@string/addedit_btn_generate"\n            android:textColor="@color/cleanthes_black"\n            android:textSize="13sp"\n            android:textStyle="bold"\n            android:letterSpacing="0.15"\n            android:backgroundTint="@color/citadel_gold"'
)

# 4. Title hint
s = s.replace('android:hint="e.g. Gmail, Nextflix"', 'android:hint="@string/addedit_hint_title"')

# 5. Username hint
s = s.replace('android:hint="e.g. pepe@gmail.com"', 'android:hint="@string/addedit_hint_username"')

# 6. Password hint
s = s.replace('android:hint="Enter or generate a password"', 'android:hint="@string/addedit_hint_password"')

# 7. Website hint
s = s.replace('android:hint="http://example.com"', 'android:hint="@string/addedit_hint_website"')

# 8. Notes hint
s = s.replace('android:hint="Security question answers, Pins etc"', 'android:hint="@string/addedit_hint_notes"')

# 9. Favorite checkbox — gold tint, stoic label
s = s.replace(
    'android:text="Add to favorites"\n            android:textColor="@color/cleanthes_text_secondary"\n            android:textSize="14sp"',
    'android:text="@string/addedit_checkbox_favorite"\n            android:textColor="@color/cleanthes_text_secondary"\n            android:textSize="14sp"\n            android:buttonTint="@color/citadel_gold"'
)

# 10. Delete button text
s = s.replace('android:text="Delete Entry"', 'android:text="@string/addedit_btn_delete"')

with open('./app/src/main/res/layout/activity_add_edit.xml', 'w') as f:
    f.write(s)
print('activity_add_edit.xml done.')

# ── item_cleanthes_entry.xml ───────────────────────────────────────
with open('./app/src/main/res/layout/item_cleanthes_entry.xml', 'r') as f:
    s = f.read()

s = s.replace('app:tint="@color/cleanthes_accent"', 'app:tint="@color/citadel_gold"')

with open('./app/src/main/res/layout/item_cleanthes_entry.xml', 'w') as f:
    f.write(s)
print('item_cleanthes_entry.xml done.')

# ── activity_home.xml — fix empty state hardcoded strings ──────────
with open('./app/src/main/res/layout/activity_home.xml', 'r') as f:
    s = f.read()

s = s.replace('android:text="Empty Vault"', 'android:text="@string/home_empty_title"')
s = s.replace('android:text="Tap + to secure a password"', 'android:text="@string/home_empty_subtitle"')

with open('./app/src/main/res/layout/activity_home.xml', 'w') as f:
    f.write(s)
print('activity_home.xml done.')
