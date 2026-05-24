package dev.favourdevlabs.cleanthes.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.widget.ImageButton;
import android.widget.TextView;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;
import dev.favourdevlabs.cleanthes.ui.addedit.AddEditActivity;
import dev.favourdevlabs.cleanthes.ui.auth.LoginActivity;
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager;
import dev.favourdevlabs.cleanthes.ui.detail.DetailActivity;
import dev.favourdevlabs.cleanthes.ui.settings.SettingsActivity;

import java.util.List;

public class HomeActivity extends AppCompatActivity implements VaultEntryAdapter.OnEntryClickListener {

    public static final String EXTRA_ENTRY_ID = "extra_entry_id";

    private RecyclerView recyclerView;
    private VaultEntryAdapter adapter;
    private HomeViewModel viewModel;
    private SearchView searchView;
    private ChipGroup chipGroup;
    private View emptyState;
    private TextView tvEntryCount;
    private ImageButton btnSearch;
    private ImageButton btnLock;
    private ImageButton btnSettings;
    private FloatingActionButton fabAdd;
    private View rootView;

    private boolean searchVisible = false;
    private boolean isLaunchingChild = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        bindViews();
        setupRecyclerView();
        setupViewModel();
        attachListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!SessionManager.isUnlocked()) {
            redirectToLogin();
            return;
        }

        SessionManager.refreshSession();
        viewModel.loadEntries();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLaunchingChild) {
            SessionManager.clearSession();
        }
        isLaunchingChild = false;
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.home_recycler_view);
        searchView = findViewById(R.id.home_search_view);
        chipGroup = findViewById(R.id.home_chip_group);
        emptyState = findViewById(R.id.home_empty_state);
        tvEntryCount = findViewById(R.id.home_tv_entry_count);
        btnSearch = findViewById(R.id.home_btn_search);
        btnSettings = findViewById(R.id.home_btn_settings);
        btnLock = findViewById(R.id.home_btn_lock);
        fabAdd = findViewById(R.id.home_fab_add);
        rootView = findViewById(android.R.id.content);
    }

    private void setupRecyclerView() {
        adapter = new VaultEntryAdapter(this, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator != null) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        setupSwipeToDelete();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                VaultEntry swiped = adapter.getEntries().get(position);

                adapter.removeAt(position);

                Snackbar.make(rootView,
                        "\"" + swiped.getTitle() + "\" deleted",
                        Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> {
                            adapter.insertAt(position, swiped);
                            recyclerView.scrollToPosition(position);
                        })
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    viewModel.deleteEntry(swiped.getId());
                                }
                            }
                        })
                        .setBackgroundTint(getColor(R.color.cleanthes_surface))
                        .setTextColor(getColor(R.color.cleanthes_text_primary))
                        .setActionTextColor(getColor(R.color.citadel_gold))
                        .show();
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        viewModel.getFilteredEntries().observe(this, entries -> {
            adapter.submitList(entries);

            boolean isEmpty = entries == null || entries.isEmpty();
            emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        viewModel.getEntryCount().observe(this, count -> {
            if (count == null || count == 0) {
                tvEntryCount.setText("");
            } else {
                tvEntryCount.setText(count + " " + (count == 1 ? "entry" : "entries"));
            }
        });

        viewModel.getCategories().observe(this, this::buildCategoryChips);

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.cleanthes_surface))
                        .setTextColor(getColor(R.color.cleanthes_text_primary))
                        .show();
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
        });
    }

    private void attachListeners() {

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditActivity.class);
            isLaunchingChild = true;
            startActivity(intent);
        });

        btnSearch.setOnClickListener(v -> toggleSearchBar());
        btnSettings.setOnClickListener(
                v -> startActivity(new Intent(this, SettingsActivity.class)));

        btnLock.setOnClickListener(v -> lockVault());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.setSearchQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            viewModel.setSearchQuery("");
            toggleSearchBar();
            return false;
        });
    }

    private void buildCategoryChips(List<String> categoryNames) {
        chipGroup.removeAllViews();

        Chip allChip = createChip("All");
        allChip.setChecked(true);
        chipGroup.addView(allChip);

        if (categoryNames != null) {
            for (String category : categoryNames) {
                chipGroup.addView(createChip(category));
            }
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkIds) -> {
            if (checkIds.isEmpty())
                return;
            Chip selected = group.findViewById(checkIds.get(0));
            if (selected != null) {
                viewModel.setCategory(selected.getText().toString());
            }
        });
    }

    private Chip createChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setId(View.generateViewId());
        chip.setCheckable(true);
        chip.setChipBackgroundColorResource(R.color.cleanthes_surface);
        chip.setTextColor(getColor(R.color.cleanthes_text_secondary));
        chip.setCheckedIconVisible(false);

        chip.setChipStrokeColorResource(R.color.cleanthes_border);
        chip.setChipStrokeWidth(1f);

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                chip.setChipBackgroundColorResource(R.color.citadel_gold);
                chip.setTextColor(getColor(R.color.cleanthes_black));
                chip.setChipStrokeColorResource(R.color.citadel_gold);
            } else {
                chip.setChipBackgroundColorResource(R.color.cleanthes_surface);
                chip.setTextColor(getColor(R.color.cleanthes_text_secondary));
                chip.setChipStrokeColorResource(R.color.cleanthes_border);
            }
        });

        return chip;
    }

    @Override
    public void onEntryClick(VaultEntry entry) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(EXTRA_ENTRY_ID, entry.getId());
        isLaunchingChild = true;
        startActivity(intent);
    }

    private void toggleSearchBar() {
        searchVisible = !searchVisible;

        if (searchVisible) {
            searchView.setVisibility(View.VISIBLE);
            searchView.requestFocus();
            btnSearch.setImageResource(R.drawable.ic_close);
        } else {
            searchView.setQuery("", false);
            searchView.clearFocus();
            searchView.setVisibility(View.GONE);
            viewModel.setSearchQuery("");
            btnSearch.setImageResource(R.drawable.ic_search);
        }
    }

    private void lockVault() {
        SessionManager.clearSession();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onCopyClick(String password) {
        // This provides the actual logic for the button in the list
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(
                android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("password", password);
        clipboard.setPrimaryClip(clip);

        android.widget.Toast.makeText(this, "Password copied to clipboard", android.widget.Toast.LENGTH_SHORT).show();
    }

}
