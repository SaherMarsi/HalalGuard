package com.example.projectfoodproject490;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.squareup.picasso.Picasso;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<HistoryItem> itemList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(HistoryItem item);
    }

    public HistoryAdapter(List<HistoryItem> itemList, OnItemClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, allergens, swipeHint;
        ImageView image;
        Chip halalChip;
        View rootView;

        public ViewHolder(View view) {
            super(view);
            rootView = view;
            name = view.findViewById(R.id.historyName);
            allergens = view.findViewById(R.id.historyAllergens);
            image = view.findViewById(R.id.historyImage);
            halalChip = view.findViewById(R.id.historyHalalChip);
            swipeHint = view.findViewById(R.id.swipeHint);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = itemList.get(position);

        holder.name.setText(item.productName);
        holder.allergens.setText("Allergens: " + formatAllergens(item.allergens));

        // Set halal status chip
        if (item.isHalal) {
            holder.halalChip.setText("Halal");
            holder.halalChip.setChipBackgroundColorResource(android.R.color.holo_green_dark);
            holder.halalChip.setChipIconResource(R.drawable.ic_check_small);
        } else {
            holder.halalChip.setText("Haram");
            holder.halalChip.setChipBackgroundColorResource(android.R.color.holo_red_dark);
            holder.halalChip.setChipIconResource(R.drawable.ic_close_small);
        }

        // Load image
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Picasso.get()
                    .load(item.imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.placeholder);
        }

        // Show swipe hint on first item (newest)
        if (position == 0) {
            holder.swipeHint.setVisibility(View.VISIBLE);
        } else {
            holder.swipeHint.setVisibility(View.GONE);
        }

        // Set click listener
        holder.rootView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    private String formatAllergens(String allergens) {
        if (allergens == null || allergens.equals("[]") || allergens.equals("None")) {
            return "None";
        }
        // Remove brackets and make it more readable
        String formatted = allergens.replace("[", "").replace("]", "").replace("\"", "");
        if (formatted.isEmpty()) {
            return "None";
        }
        return formatted;
    }
}