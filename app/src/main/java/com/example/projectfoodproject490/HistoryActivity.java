package com.example.projectfoodproject490;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;
    private List<String> historyKeys;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private DatabaseReference historyRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize views
        recyclerView = findViewById(R.id.historyRecycler);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Add menu with delete all option
        toolbar.inflateMenu(R.menu.menu_history);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_all) {
                showDeleteAllDialog();
                return true;
            }
            return false;
        });

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();
        historyKeys = new ArrayList<>();
        adapter = new HistoryAdapter(historyList, this::onHistoryItemClick);
        recyclerView.setAdapter(adapter);

        // Show loading
        progressBar.setVisibility(View.VISIBLE);

        // Setup swipe to delete
        setupSwipeToDelete();

        loadHistory();
    }

    private void loadHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        historyRef = FirebaseDatabase.getInstance()
                .getReference("history")
                .child(userId);

        historyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                historyList.clear();
                historyKeys.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    HistoryItem item = child.getValue(HistoryItem.class);
                    if (item != null) {
                        historyList.add(item);
                        historyKeys.add(child.getKey());
                    }
                }

                // Reverse the lists to show newest first
                ArrayList<HistoryItem> reversedItems = new ArrayList<>();
                ArrayList<String> reversedKeys = new ArrayList<>();
                for (int i = historyList.size() - 1; i >= 0; i--) {
                    reversedItems.add(historyList.get(i));
                    reversedKeys.add(historyKeys.get(i));
                }
                historyList.clear();
                historyKeys.clear();
                historyList.addAll(reversedItems);
                historyKeys.addAll(reversedKeys);

                if (historyList.isEmpty()) {
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
                Toast.makeText(HistoryActivity.this, "Error loading history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            private ColorDrawable background = new ColorDrawable(Color.parseColor("#F44336"));
            private Drawable deleteIcon = ContextCompat.getDrawable(HistoryActivity.this, R.drawable.ic_delete);
            private int iconMargin = (int) getResources().getDimension(R.dimen.icon_margin);

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // Save item for undo
                final HistoryItem deletedItem = historyList.get(position);
                final String deletedKey = historyKeys.get(position);

                // Remove item from list
                historyList.remove(position);
                historyKeys.remove(position);
                adapter.notifyItemRemoved(position);

                // Check if list is empty
                if (historyList.isEmpty()) {
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }

                // Show snackbar with undo option
                Snackbar snackbar = Snackbar.make(recyclerView, "Item deleted", Snackbar.LENGTH_LONG);
                snackbar.setAction("UNDO", v -> {
                    // Restore item
                    historyList.add(position, deletedItem);
                    historyKeys.add(position, deletedKey);
                    adapter.notifyItemInserted(position);
                    recyclerView.scrollToPosition(position);

                    // Hide empty state if needed
                    if (!historyList.isEmpty()) {
                        emptyStateLayout.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });

                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            // Delete from Firebase if not undone
                            historyRef.child(deletedKey).removeValue()
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(HistoryActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                                        // Restore item on failure
                                        historyList.add(position, deletedItem);
                                        historyKeys.add(position, deletedKey);
                                        adapter.notifyItemInserted(position);
                                    });
                        }
                    }
                });

                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                int itemHeight = itemView.getHeight();

                // Draw red background
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                // Draw delete icon
                int iconTop = itemView.getTop() + (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                int iconRight = itemView.getRight() - iconMargin;

                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void onHistoryItemClick(HistoryItem item) {
        // You can implement opening product details here if needed
        Toast.makeText(this, item.productName, Toast.LENGTH_SHORT).show();
    }

    private void showDeleteAllDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete All History")
                .setMessage("Are you sure you want to delete all history? This action cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    deleteAllHistory();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllHistory() {
        historyRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "All history deleted", Toast.LENGTH_SHORT).show();
                    historyList.clear();
                    historyKeys.clear();
                    adapter.notifyDataSetChanged();
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete history", Toast.LENGTH_SHORT).show();
                });
    }
}