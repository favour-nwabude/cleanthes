package dev.favourdevlabs.cleanthes.ui.home

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry

class VaultEntryAdapter(
    private val context: Context,
    private val listener: OnEntryClickListener
) : RecyclerView.Adapter<VaultEntryAdapter.VaultEntryViewHolder>() {

    interface OnEntryClickListener {
        fun onEntryClick(entry: VaultEntry)
        fun onCopyClick(password: String)
    }

    private var entries = mutableListOf<VaultEntry>()

    fun getEntries(): List<VaultEntry> = entries

    fun removeAt(position: Int) {
        if (position in entries.indices) {
            entries.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, entries.size)
        }
    }

    fun insertAt(position: Int, entry: VaultEntry) {
        if (position in 0..entries.size) {
            entries.add(position, entry)
            notifyItemInserted(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cleanthes_entry, parent, false)
        return VaultEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: VaultEntryViewHolder, position: Int) =
        holder.bind(entries[position], listener)

    override fun getItemCount(): Int = entries.size

    fun submitList(newEntries: List<VaultEntry>?) {
        val safeNew    = newEntries ?: emptyList()
        val oldEntries = entries.toList()

        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldEntries.size
            override fun getNewListSize() = safeNew.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                oldEntries[oldPos].id == safeNew[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                val o = oldEntries[oldPos]; val n = safeNew[newPos]
                return o.title    == n.title    &&
                       o.username == n.username &&
                       o.category == n.category &&
                       o.isFavorite == n.isFavorite &&
                       o.hasTOTP() == n.hasTOTP()
            }
        })

        entries = safeNew.toMutableList()
        result.dispatchUpdatesTo(this)
    }

    // -------------------------------------------------------------------------

    class VaultEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvInitial:  TextView    = itemView.findViewById(R.id.item_tv_initial)
        private val tvTitle:    TextView    = itemView.findViewById(R.id.item_tv_title)
        private val tvUsername: TextView    = itemView.findViewById(R.id.item_tv_username)
        private val iconBg:     View        = itemView.findViewById(R.id.item_icon_bg)
        private val totpDot:    View        = itemView.findViewById(R.id.item_totp_dot)
        private val btnCopy:    ImageButton = itemView.findViewById(R.id.item_btn_copy)

        fun bind(entry: VaultEntry, listener: OnEntryClickListener) {
            tvTitle.text    = entry.title
            tvUsername.text = entry.username
            tvInitial.text  = if (entry.title.isEmpty()) "?"
                              else entry.title[0].uppercaseChar().toString()

            val circleColor = if (entry.isFavorite) Color.parseColor("#FFB300")
                              else categoryColor(entry.category)
            iconBg.backgroundTintList =
                android.content.res.ColorStateList.valueOf(circleColor)

            totpDot.visibility = if (entry.hasTOTP()) View.VISIBLE else View.GONE

            itemView.setOnClickListener { listener.onEntryClick(entry) }
            btnCopy.setOnClickListener  { listener.onCopyClick(entry.encryptedPassword) }
        }

        private fun categoryColor(category: String): Int {
            val palette = intArrayOf(
                Color.parseColor("#2E86AB"),
                Color.parseColor("#A23B72"),
                Color.parseColor("#F18F01"),
                Color.parseColor("#3DAA6E")
            )
            return palette[Math.abs(category.hashCode()) % palette.size]
        }
    }
}
