package com.example.projectfoodproject490;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.squareup.picasso.Picasso;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private final List<FavoriteItem> itemList;
    private final OnItemClickListener clickListener;
    private final OnItemRemoveListener removeListener;

    public interface OnItemClickListener {
        void onItemClick(FavoriteItem item);
    }

    public interface OnItemRemoveListener {
        void onItemRemove(FavoriteItem item, int position);
    }

    public FavoritesAdapter(List<FavoriteItem> itemList, OnItemClickListener clickListener, OnItemRemoveListener removeListener) {
        this.itemList = itemList;
        this.clickListener = clickListener;
        this.removeListener = removeListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, allergens;
        ImageView image;
        Chip halalChip;
        ImageButton removeBtn;
        View rootView;

        public ViewHolder(View view) {
            super(view);
            rootView = view;
            name = view.findViewById(R.id.favoriteName);
            allergens = view.findViewById(R.id.favoriteAllergens);
            image = view.findViewById(R.id.favoriteImage);
            halalChip = view.findViewById(R.id.favoriteHalalChip);
            removeBtn = view.findViewById(R.id.removeBtn);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FavoriteItem item = itemList.get(position);

        holder.name.setText(item.productName);
        holder.allergens.setText("Allergens: " + formatAllergens(item.allergens));

        if (item.isHalal) {
            holder.halalChip.setText("Halal");
            holder.halalChip.setChipBackgroundColorResource(android.R.color.holo_green_dark);
            holder.halalChip.setChipIconResource(R.drawable.ic_check_small);
        } else {
            holder.halalChip.setText("Haram");
            holder.halalChip.setChipBackgroundColorResource(android.R.color.holo_red_dark);
            holder.halalChip.setChipIconResource(R.drawable.ic_close_small);
        }

        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Picasso.get()
                    .load(item.imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.placeholder);
        }

        holder.rootView.setOnClickListener(v -> clickListener.onItemClick(item));
        holder.removeBtn.setOnClickListener(v -> removeListener.onItemRemove(item, position));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    private String formatAllergens(String allergens) {
        if (allergens == null || allergens.equals("[]") || allergens.equals("None")) {
            return "None";
        }
        String formatted = allergens.replace("[", "").replace("]", "").replace("\"", "");
        if (formatted.isEmpty()) {
            return "None";
        }
        return formatted;
    }
}