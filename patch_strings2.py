with open('./app/src/main/res/values/strings.xml', 'r') as f:
    s = f.read()

new_strings = '''
    <!-- Add/Edit Screen -->
    <string name="addedit_title_new">New Entry</string>
    <string name="addedit_title_edit">Edit Entry</string>
    <string name="addedit_hint_title">Gmail, GitHub, Netflix</string>
    <string name="addedit_hint_username">username or email</string>
    <string name="addedit_hint_password">Set the combination</string>
    <string name="addedit_hint_website">https://example.com</string>
    <string name="addedit_hint_notes">Answers, PINs, anything else</string>
    <string name="addedit_btn_save">COMMIT</string>
    <string name="addedit_btn_generate">FORGE</string>
    <string name="addedit_btn_delete">ERASE ENTRY</string>
    <string name="addedit_checkbox_favorite">Mark as priority</string>

'''

s = s.replace('    <!-- Password Strength Labels -->', new_strings + '    <!-- Password Strength Labels -->')

with open('./app/src/main/res/values/strings.xml', 'w') as f:
    f.write(s)

print('Done.')
