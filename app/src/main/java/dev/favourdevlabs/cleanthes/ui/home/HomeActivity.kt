package dev.favourdevlabs.cleanthes.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.ui.addedit.AddEditActivity
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.detail.DetailActivity
import dev.favourdevlabs.cleanthes.ui.settings.SettingsActivity

class HomeActivity : AuthenticatedActivity(), VaultEntryAdapter.OnEntryClickListener {

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VaultEntryAdapter
    private lateinit var viewModel: HomeViewModel
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var emptyState: View
    private lateinit var tvEntryCount: TextView
    private lateinit var btnSearch: ImageButton
    private lateinit var btnLock: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var rootView: View

    private var searchVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        bindViews()
        setupRecyclerView()
        setupViewModel()
        attachListeners()
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) viewModel.loadEntries()
    }

    private fun bindViews() {
        recyclerView = findViewById(R.id.home_recycler_view)
        searchView = findViewById(R.id.home_search_view)
        chipGroup = findViewById(R.id.home_chip_group)
        emptyState = findViewById(R.id.home_empty_state)
        tvEntryCount = findViewById(R.id.home_tv_entry_count)
        btnSearch = findViewById(R.id.home_btn_search)
        btnSettings = findViewById(R.id.home_btn_settings)
        btnLock = findViewById(R.id.home_btn_lock)
        fabAdd = findViewById(R.id.home_fab_add)
        rootView = findViewById(android.R.id.content)
    }

    private fun setupRecyclerView() {
        adapter = VaultEntryAdapter(this, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        (recyclerView.itemAnimator as? SimpleItemAnimator)
            ?.setSupportsChangeAnimations(false)

        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val swiped = adapter.getEntries()[position]
                adapter.removeAt(position)

                Snackbar.make(rootView, "\"${swiped.title}\" deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        adapter.insertAt(position, swiped)
                        recyclerView.scrollToPosition(position)
                    }
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(snackbar: Snackbar, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) viewModel.deleteEntry(swiped.id)
                        }
                    })
                    .setBackgroundTint(getColor(R.color.cleanthes_surface))
                    .setTextColor(getColor(R.color.cleanthes_text_primary))
                    .setActionTextColor(getColor(R.color.citadel_gold))
                    .show()
            }

            /**
             * Paints the delete affordance behind the swiping card.
             *
             * Draw order matters:
             * 1. Red background — painted onto the canvas first
             * 2. Trash icon — painted on top of the background
             * 3. super — draws the card on top, covering what hasn't slid away
             *
             * Result: red background and trash icon are only visible in the
             * region the card has already moved away from.
             */
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    val item = viewHolder.itemView

                    val bgPaint = Paint().apply {
                        color = 0xFFB71C1C.toInt()
                        isAntiAlias = true
                    }
                    c.drawRect(
                        item.right + dX, item.top.toFloat(),
                        item.right.toFloat(), item.bottom.toFloat(),
                        bgPaint
                    )

                    ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_delete)?.let { icon ->
                        val iconSize = dpToPx(22)
                        val iconMargin = (item.height - iconSize) / 2
                        val iconRight = item.right - iconMargin
                        icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                        icon.setBounds(iconRight - iconSize, item.top + iconMargin, iconRight, item.bottom - iconMargin)
                        icon.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        viewModel.filteredEntries.observe(this) { entries ->
            adapter.submitList(entries)
            val isEmpty = entries.isNullOrEmpty()
            emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.entryCount.observe(this) { count ->
            tvEntryCount.text = when {
                count == null || count == 0 -> ""
                else -> "$count ${if (count == 1) "entry" else "entries"}"
            }
        }

        viewModel.categories.observe(this) { buildCategoryChips(it) }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(R.color.cleanthes_surface))
                    .setTextColor(getColor(R.color.cleanthes_text_primary))
                    .show()
            }
        }

        viewModel.isLoading.observe(this) { /* reserved */ }
    }

    private fun attachListeners() {
        fabAdd.setOnClickListener { startActivity(Intent(this, AddEditActivity::class.java)) }
        btnSearch.setOnClickListener { toggleSearchBar() }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        btnLock.setOnClickListener { lockVault() }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.setSearchQuery(query); return true
            }
            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.setSearchQuery(newText); return true
            }
        })

        searchView.setOnCloseListener {
            viewModel.setSearchQuery("")
            toggleSearchBar()
            false
        }
    }

    private fun buildCategoryChips(categoryNames: List<String>?) {
        chipGroup.removeAllViews()
        createChip("All").also { it.isChecked = true; chipGroup.addView(it) }
        categoryNames?.forEach { chipGroup.addView(createChip(it)) }

        chipGroup.setOnCheckedStateChangeListener { group, checkIds ->
            if (checkIds.isEmpty()) return@setOnCheckedStateChangeListener
            val selected = group.findViewById<Chip>(checkIds[0])
            selected?.let { viewModel.setCategory(it.text.toString()) }
        }
    }

    private fun createChip(text: String) = Chip(this).apply {
        this.text = text
        id = View.generateViewId()
        isCheckable = true
        setChipBackgroundColorResource(R.color.cleanthes_surface)
        setTextColor(getColor(R.color.cleanthes_text_secondary))
        isCheckedIconVisible = false
        setChipStrokeColorResource(R.color.cleanthes_border)
        chipStrokeWidth = 1f

        setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setChipBackgroundColorResource(R.color.citadel_gold)
                setTextColor(getColor(R.color.cleanthes_black))
                setChipStrokeColorResource(R.color.citadel_gold)
            } else {
                setChipBackgroundColorResource(R.color.cleanthes_surface)
                setTextColor(getColor(R.color.cleanthes_text_secondary))
                setChipStrokeColorResource(R.color.cleanthes_border)
            }
        }
    }

    override fun onEntryClick(entry: VaultEntry) {
        startActivity(Intent(this, DetailActivity::class.java).apply {
            putExtra(EXTRA_ENTRY_ID, entry.id)
        })
    }

    override fun onCopyClick(password: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("password", password))
        Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun toggleSearchBar() {
        searchVisible = !searchVisible
        if (searchVisible) {
            searchView.visibility = View.VISIBLE
            searchView.requestFocus()
            btnSearch.setImageResource(R.drawable.ic_close)
        } else {
            searchView.setQuery("", false)
            searchView.clearFocus()
            searchView.visibility = View.GONE
            viewModel.setSearchQuery("")
            btnSearch.setImageResource(R.drawable.ic_search)
        }
    }

    private fun lockVault() {
        SessionManager.clearSession()
        redirectToLogin()
    }

    private fun dpToPx(dp: Int) = Math.round(dp * resources.displayMetrics.density)
}
