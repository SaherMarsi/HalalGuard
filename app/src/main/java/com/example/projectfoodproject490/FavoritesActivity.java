package com.example.projectfoodproject490;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;
    private List<FavoriteItem> favoritesList;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private DatabaseReference favoritesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerView = findViewById(R.id.favoritesRecycler);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritesList = new ArrayList<>();
        adapter = new FavoritesAdapter(favoritesList, this::onFavoriteItemClick, this::onFavoriteRemove);
        recyclerView.setAdapter(adapter);

        progressBar.setVisibility(View.VISIBLE);
        loadFavorites();
    }

    private void loadFavorites() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        favoritesRef = FirebaseDatabase.getInstance()
                .getReference("favorites")
                .child(userId);

        favoritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                favoritesList.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    FavoriteItem item = child.getValue(FavoriteItem.class);
                    if (item != null) {
                        favoritesList.add(item);
                    }
                }

                favoritesList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

                if (favoritesList.isEmpty()) {
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateLayout.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(FavoritesActivity.this, "Error loading favorites", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onFavoriteItemClick(FavoriteItem item) {
        Toast.makeText(this, item.productName, Toast.LENGTH_SHORT).show();
    }

    private void onFavoriteRemove(FavoriteItem item, int position) {
        favoritesRef.child(item.barcode).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove", Toast.LENGTH_SHORT).show();
                });
    }
}