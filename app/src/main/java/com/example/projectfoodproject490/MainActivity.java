package com.example.projectfoodproject490;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    TextView welcomeText;
    DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // If not logged in, redirect to login
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            setContentView(R.layout.activity_main);

            welcomeText = findViewById(R.id.welcomeText);
            usersRef = FirebaseDatabase.getInstance().getReference("users");

            // Load user's full name
            loadUserName(currentUser.getUid());

            MaterialButton goToSearchBtn = findViewById(R.id.goToSearchBtn);
            goToSearchBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            });

            MaterialButton allergySearchBtn = findViewById(R.id.allergySearchBtn);
            allergySearchBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AllergySearchActivity.class);
                startActivity(intent);
            });

            MaterialButton historyBtn = findViewById(R.id.goToHistoryBtn);
            historyBtn.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            });

            MaterialButton favoritesBtn = findViewById(R.id.goToFavoritesBtn);
            favoritesBtn.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
            });

            // Fix: Use ImageButton instead of Button for settingsBtn
            ImageButton settingsBtn = findViewById(R.id.settingsBtn);
            settingsBtn.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            });

            MaterialButton logoutBtn = findViewById(R.id.logoutBtn);
            logoutBtn.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
    }

    private void loadUserName(String userId) {
        usersRef.child(userId).child("fullName").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String fullName = snapshot.getValue(String.class);
                if (fullName != null && !fullName.isEmpty()) {
                    welcomeText.setText("Welcome, " + fullName);
                } else {
                    // Fallback to email if full name not found
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        welcomeText.setText("Welcome, " + user.getEmail());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Fallback to email if error occurs
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    welcomeText.setText("Welcome, " + user.getEmail());
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user name when returning from settings
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUserName(currentUser.getUid());
        }
    }
}