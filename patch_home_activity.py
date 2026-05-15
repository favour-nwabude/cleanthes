with open('./app/src/main/java/dev/favourdevlabs/cleanthes/ui/home/HomeActivity.java', 'r') as f:
    src = f.read()

# 1. Remove searchDivider field — never bound, causes NullPointerException on search tap
src = src.replace('    private View searchDivider;\n', '')

# 2. Remove searchDivider.setVisibility(View.VISIBLE) in toggle open
src = src.replace(
    '            searchDivider.setVisibility(View.VISIBLE);\n            searchView.requestFocus();',
    '            searchView.requestFocus();'
)

# 3. Remove searchDivider.setVisibility(View.GONE) in toggle close
src = src.replace(
    '            searchView.setVisibility(View.GONE);\n            searchDivider.setVisibility(View.GONE);\n            viewModel.setSearchQuery("");',
    '            searchView.setVisibility(View.GONE);\n            viewModel.setSearchQuery("");'
)

# 4. Chip selected — gold background, BLACK text (not gold-on-gold), gold border
src = src.replace(
    '                chip.setChipBackgroundColorResource(R.color.cleanthes_accent);\n                chip.setTextColor(getColor(R.color.cleanthes_accent));\n                chip.setChipStrokeColorResource(R.color.cleanthes_accent);',
    '                chip.setChipBackgroundColorResource(R.color.citadel_gold);\n                chip.setTextColor(getColor(R.color.cleanthes_black));\n                chip.setChipStrokeColorResource(R.color.citadel_gold);'
)

# 5. Snackbar action — gold
src = src.replace(
    '                        .setActionTextColor(getColor(R.color.cleanthes_accent))',
    '                        .setActionTextColor(getColor(R.color.citadel_gold))'
)

with open('./app/src/main/java/dev/favourdevlabs/cleanthes/ui/home/HomeActivity.java', 'w') as f:
    f.write(src)

print('Done.')
