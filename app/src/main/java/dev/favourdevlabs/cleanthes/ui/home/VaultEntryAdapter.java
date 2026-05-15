package dev.favourdevlabs.cleanthes.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import dev.favourdevlabs.cleanthes.R;
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;

import java.util.List;
import java.util.ArrayList;

public class VaultEntryAdapter extends RecyclerView.Adapter<VaultEntryAdapter.VaultEntryViewHolder> {

    public interface OnEntryClickListener {
        void onEntryClick(VaultEntry entry);

        void onCopyClick(String password);
    }

    private List<VaultEntry> entries = new ArrayList<>();
    private final OnEntryClickListener listener;
    private final Context context;

    public VaultEntryAdapter(Context context, OnEntryClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public List<VaultEntry> getEntries() {
        return entries;
    }

    // Inside VaultEntryAdapter.java

    public void removeAt(int position) {
        if (position >= 0 && position < entries.size()) {
            entries.remove(position);
            notifyItemRemoved(position);
            // Important: notify range changed if you use positions for logic elsewhere
            notifyItemRangeChanged(position, entries.size());
        }
    }

    public void insertAt(int position, VaultEntry entry) {
        if (position >= 0 && position <= entries.size()) {
            entries.add(position, entry);
            notifyItemInserted(position);
        }
    }

    @NonNull
    @Override
    public VaultEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cleanthes_entry, parent, false);
        return new VaultEntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VaultEntryViewHolder holder, int position) {
        VaultEntry entry = entries.get(position);
        holder.bind(entry, listener);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void submitList(List<VaultEntry> newEntries) {
        final List<VaultEntry> safeNew = (newEntries != null) ? newEntries : new ArrayList<>();
        List<VaultEntry> oldEntries = this.entries;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {

            @Override
            public int getOldListSize() {
                return oldEntries.size();
            }

            @Override
            public int getNewListSize() {
                return safeNew.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return oldEntries.get(oldPos).getId() == safeNew.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                VaultEntry oldEntry = oldEntries.get(oldPos);
                VaultEntry newEntry = safeNew.get(newPos);
                return oldEntry.getTitle().equals(newEntry.getTitle())
                        && oldEntry.getUsername().equals(newEntry.getUsername())
                        && oldEntry.getCategory().equals(newEntry.getCategory())
                        && oldEntry.isFavorite() == newEntry.isFavorite();
            }
        });
        this.entries = new ArrayList<>(safeNew);
        diffResult.dispatchUpdatesTo(this);
    }

    static class VaultEntryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitial, tvTitle, tvUsername;
        private final View iconBg;
        private final ImageButton btnCopy;

        VaultEntryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Update these to match the professional XML IDs
            tvInitial = itemView.findViewById(R.id.item_tv_initial);
            tvTitle = itemView.findViewById(R.id.item_tv_title);
            tvUsername = itemView.findViewById(R.id.item_tv_username);
            iconBg = itemView.findViewById(R.id.item_icon_bg);
            btnCopy = itemView.findViewById(R.id.item_btn_copy);
        }

        void bind(VaultEntry entry, OnEntryClickListener listener) {
            tvTitle.setText(entry.getTitle());
            tvUsername.setText(entry.getUsername());

            // Set the initial (e.g., 'G' for Google)
            String initial = entry.getTitle().isEmpty() ? "?"
                    : String.valueOf(entry.getTitle().charAt(0)).toUpperCase();
            tvInitial.setText(initial);

            // Set the category circle color
            int circleColor = entry.isFavorite()
                    ? android.graphics.Color.parseColor("#FFB300")
                    : getCategoryColor(entry.getCategory());
            iconBg.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(circleColor));

            // Professional interactions
            itemView.setOnClickListener(v -> listener.onEntryClick(entry));
            btnCopy.setOnClickListener(v -> listener.onCopyClick(entry.getEncryptedPassword()));
        }

        private int getCategoryColor(String category) {
            int[] palette = {
                    Color.parseColor("#2E86AB"), Color.parseColor("#A23B72"),
                    Color.parseColor("#F18F01"), Color.parseColor("#3DAA6E"),
                    // ... add more if you like
            };
            return palette[Math.abs(category.hashCode()) % palette.length];
        }
    }

}
