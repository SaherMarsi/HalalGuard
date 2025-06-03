package com.example.projectfoodproject490;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

public class SearchActivity extends AppCompatActivity {

    // Constants
    private static final int CAMERA_REQUEST_CODE = 1001;
    private static final int GALLERY_REQUEST_CODE = 1003;
    private static final int PERMISSION_REQUEST_CODE = 1004;

    EditText barcodeInput;
    Button searchBtn, scanBtn, galleryBtn;
    MaterialButton shareBtnProduct, favoriteBtn;
    TextView nameText, allergenText, halalText;
    ImageView productImage;
    MaterialCardView resultCard;
    Chip halalChip;

    // Store current product info for sharing and favorites
    private String currentProductName = "";
    private String currentBarcode = "";
    private String currentImageUrl = "";
    private String currentAllergens = "";
    private HalalStatus currentHalalStatus;
    private boolean isFavorite = false;
    private ArrayList<String> selectedAllergies = new ArrayList<>();

    // Comprehensive list of haram ingredients and E-numbers
    private final String[] haramIngredients = {
            "pork", "swine", "pig", "bacon", "ham", "lard", "pepperoni",
            "prosciutto", "salami", "chorizo", "pancetta", "gelatin", "gelatine",
            "alcohol", "ethanol", "wine", "beer", "spirits", "liquor", "vodka",
            "whiskey", "rum", "brandy", "champagne", "sake", "ethyl alcohol",
            "e120", "e441", "e542", "e904", "e124", "e631", "e635",
            "e471", "e472", "e473", "e474", "e475", "e476", "e477",
            "e481", "e482", "e483",
            "rennet", "pepsin", "lipase", "animal fat", "beef fat", "tallow",
            "shortening", "glycerides", "stearic acid", "l-cysteine", "shellac",
            "cochineal", "carmine", "blood", "carrion"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize views
        barcodeInput = findViewById(R.id.barcodeInput);
        searchBtn = findViewById(R.id.searchBtn);
        scanBtn = findViewById(R.id.scanBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        nameText = findViewById(R.id.nameText);
        allergenText = findViewById(R.id.allergenText);
        halalText = findViewById(R.id.halalText);
        productImage = findViewById(R.id.productImage);
        resultCard = findViewById(R.id.resultCard);
        halalChip = findViewById(R.id.halalChip);
        shareBtnProduct = findViewById(R.id.shareBtnProduct);
        favoriteBtn = findViewById(R.id.favoriteBtn);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Check if we have incoming data from allergy search
        Intent intent = getIntent();
        if (intent.hasExtra("selectedAllergies")) {
            selectedAllergies = intent.getStringArrayListExtra("selectedAllergies");
        }
        if (intent.hasExtra("barcode")) {
            String barcode = intent.getStringExtra("barcode");
            barcodeInput.setText(barcode);
            fetchProductData(barcode);
        }

        searchBtn.setOnClickListener(v -> {
            String barcode = barcodeInput.getText().toString().trim();
            if (!barcode.isEmpty()) {
                fetchProductData(barcode);
            } else {
                Toast.makeText(this, "Enter a barcode", Toast.LENGTH_SHORT).show();
            }
        });

        scanBtn.setOnClickListener(v -> {
            Intent scanIntent = new Intent(this, BarcodeScannerActivity.class);
            startActivityForResult(scanIntent, CAMERA_REQUEST_CODE);
        });

        galleryBtn.setOnClickListener(v -> checkPermissionsAndOpenGallery());

        // Share button click listener
        shareBtnProduct.setOnClickListener(v -> shareProductInfo());

        // Favorite button click listener
        favoriteBtn.setOnClickListener(v -> toggleFavorite());
    }

    private void checkPermissionsAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        } else {
            // For Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Product Image"), GALLERY_REQUEST_CODE);
    }

    private void processImageForBarcode(Uri imageUri) {
        try {
            // Get bitmap from Uri
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Configure barcode scanner options
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E)
                    .build();

            // Process image with ML Kit for barcode detection
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            BarcodeScanner scanner = BarcodeScanning.getClient(options);

            Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.isEmpty()) {
                            // No barcode found
                            Toast.makeText(SearchActivity.this, "No barcode found in the image", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Get the first barcode value
                        String barcodeValue = barcodes.get(0).getRawValue();

                        if (barcodeValue != null && !barcodeValue.isEmpty()) {
                            // Set the barcode value in the input field
                            barcodeInput.setText(barcodeValue);

                            // Search for the product
                            fetchProductData(barcodeValue);
                        } else {
                            Toast.makeText(SearchActivity.this, "Could not read barcode", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SearchActivity.this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String scannedBarcode = data.getStringExtra("scannedBarcode");
            if (scannedBarcode != null) {
                barcodeInput.setText(scannedBarcode);
                fetchProductData(scannedBarcode);
            }
        } else if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            processImageForBarcode(imageUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchProductData(String barcode) {
        new Thread(() -> {
            try {
                URL url = new URL("https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder jsonBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(jsonBuilder.toString());

                    // Check if product was found
                    int status = json.optInt("status", 0);
                    if (status != 1) {
                        runOnUiThread(() -> {
                            resultCard.setVisibility(View.GONE);
                            Toast.makeText(this, "Product not found in database", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    JSONObject product = json.getJSONObject("product");

                    String name = product.optString("product_name", "Unknown");
                    String imageUrl = product.optString("image_url", "");
                    JSONArray allergensArray = product.optJSONArray("allergens_tags");

                    String allergens = formatAllergens(allergensArray);

                    // Use the improved halal checker
                    HalalStatus halalStatus = checkHalalStatusDetailed(product);

                    runOnUiThread(() -> {
                        // Store current product info
                        currentProductName = name;
                        currentBarcode = barcode;
                        currentImageUrl = imageUrl;
                        currentAllergens = allergens;
                        currentHalalStatus = halalStatus;

                        // Show the result card with found product
                        resultCard.setVisibility(View.VISIBLE);

                        // Check if product is in favorites
                        checkIfFavorite(barcode);

                        // Save search history if user is logged in
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            Map<String, Object> historyItem = new HashMap<>();
                            historyItem.put("barcode", barcode);
                            historyItem.put("productName", name);
                            historyItem.put("imageUrl", imageUrl);
                            historyItem.put("allergens", (allergens != null ? allergens : "None"));
                            historyItem.put("isHalal", halalStatus.confidence != HalalStatus.Confidence.HARAM);

                            FirebaseDatabase.getInstance().getReference("history")
                                    .child(userId)
                                    .push()
                                    .setValue(historyItem);
                        }

                        // Update UI
                        nameText.setText(name);

                        // Check and highlight allergens
                        String displayAllergens = allergens;
                        boolean containsSelectedAllergen = false;
                        if (!selectedAllergies.isEmpty() && allergensArray != null) {
                            for (int i = 0; i < allergensArray.length(); i++) {
                                try {
                                    String allergen = allergensArray.getString(i).replace("en:", "");
                                    for (String selected : selectedAllergies) {
                                        if (allergen.toLowerCase().contains(selected.toLowerCase())) {
                                            containsSelectedAllergen = true;
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (containsSelectedAllergen) {
                                displayAllergens = "⚠️ CONTAINS YOUR ALLERGENS: " + allergens;
                                allergenText.setTextColor(Color.parseColor("#F44336"));
                            } else {
                                allergenText.setTextColor(Color.parseColor("#666666"));
                            }
                        }

                        allergenText.setText(displayAllergens);

                        // Update halal status chip based on confidence level
                        halalChip.setText(halalStatus.confidence.getLabel());
                        halalChip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor(halalStatus.confidence.getColorHex())));

                        // Set appropriate icon
                        switch (halalStatus.confidence) {
                            case CERTIFIED_HALAL:
                            case LIKELY_HALAL:
                                halalChip.setChipIconResource(R.drawable.ic_check);
                                break;
                            case DOUBTFUL:
                                halalChip.setChipIconResource(R.drawable.ic_help);
                                break;
                            case HARAM:
                                halalChip.setChipIconResource(R.drawable.ic_close);
                                break;
                        }

                        // Show detailed status
                        halalText.setText(halalStatus.getReason());
                        if (halalStatus.getProblematicIngredients().size() > 0) {
                            halalText.append("\n\nProblematic ingredients: " +
                                    String.join(", ", halalStatus.getProblematicIngredients()));
                        }
                        halalText.setVisibility(View.VISIBLE);

                        // Color code the text based on status
                        halalText.setTextColor(Color.parseColor(halalStatus.confidence.getColorHex()));

                        // Load image
                        if (!imageUrl.isEmpty()) {
                            Picasso.get()
                                    .load(imageUrl)
                                    .placeholder(R.drawable.placeholder)
                                    .error(R.drawable.placeholder)
                                    .into(productImage);
                        } else {
                            productImage.setImageResource(R.drawable.placeholder);
                        }
                    });

                } else {
                    // Handle non-200 response codes
                    runOnUiThread(() -> {
                        resultCard.setVisibility(View.GONE);
                        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                            Toast.makeText(this, "Product not found", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Server error: " + responseCode, Toast.LENGTH_LONG).show();
                        }
                    });
                }

            } catch (java.net.UnknownHostException e) {
                runOnUiThread(() -> {
                    resultCard.setVisibility(View.GONE);
                    Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
                });
            } catch (java.net.SocketTimeoutException e) {
                runOnUiThread(() -> {
                    resultCard.setVisibility(View.GONE);
                    Toast.makeText(this, "Connection timeout. Please try again.", Toast.LENGTH_LONG).show();
                });
            } catch (JSONException e) {
                runOnUiThread(() -> {
                    resultCard.setVisibility(View.GONE);
                    Toast.makeText(this, "Error parsing product data", Toast.LENGTH_LONG).show();
                });
                e.printStackTrace();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    resultCard.setVisibility(View.GONE);
                    Toast.makeText(this, "An error occurred: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void shareProductInfo() {
        if (currentProductName.isEmpty()) {
            Toast.makeText(this, "No product to share", Toast.LENGTH_SHORT).show();
            return;
        }

        String shareText = "Product: " + currentProductName + "\n" +
                "Halal Status: " + currentHalalStatus.confidence.getLabel() + "\n" +
                "Reason: " + currentHalalStatus.getReason() + "\n" +
                "Allergens: " + currentAllergens;

        if (currentHalalStatus.getProblematicIngredients().size() > 0) {
            shareText += "\nProblematic ingredients: " +
                    String.join(", ", currentHalalStatus.getProblematicIngredients());
        }

        shareText += "\n\nShared from HalalScan App";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Halal Status: " + currentProductName);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share Product Info"));
    }

    private void toggleFavorite() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentBarcode.isEmpty()) {
            Toast.makeText(this, "No product to favorite", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        DatabaseReference favRef = FirebaseDatabase.getInstance()
                .getReference("favorites")
                .child(userId)
                .child(currentBarcode);

        if (isFavorite) {
            // Remove from favorites
            favRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        isFavorite = false;
                        updateFavoriteButton();
                        Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to remove favorite", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add to favorites
            Map<String, Object> favoriteItem = new HashMap<>();
            favoriteItem.put("barcode", currentBarcode);
            favoriteItem.put("productName", currentProductName);
            favoriteItem.put("imageUrl", currentImageUrl);
            favoriteItem.put("allergens", currentAllergens);
            favoriteItem.put("isHalal", currentHalalStatus.confidence != HalalStatus.Confidence.HARAM);
            favoriteItem.put("timestamp", System.currentTimeMillis());

            favRef.setValue(favoriteItem)
                    .addOnSuccessListener(aVoid -> {
                        isFavorite = true;
                        updateFavoriteButton();
                        Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add favorite", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void checkIfFavorite(String barcode) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            runOnUiThread(() -> {
                isFavorite = false;
                updateFavoriteButton();
            });
            return;
        }

        String userId = user.getUid();
        FirebaseDatabase.getInstance()
                .getReference("favorites")
                .child(userId)
                .child(barcode)
                .get()
                .addOnCompleteListener(task -> {
                    runOnUiThread(() -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            isFavorite = true;
                        } else {
                            isFavorite = false;
                        }
                        updateFavoriteButton();
                    });
                });
    }

    private void updateFavoriteButton() {
        if (favoriteBtn == null) return;

        if (isFavorite) {
            favoriteBtn.setText("Remove from Favorites");
            favoriteBtn.setIconResource(R.drawable.ic_star_filled);
            favoriteBtn.setIconTint(ColorStateList.valueOf(Color.parseColor("#FFC107")));
        } else {
            favoriteBtn.setText("Add to Favorites");
            favoriteBtn.setIconResource(R.drawable.ic_star_outline);
            favoriteBtn.setIconTint(ColorStateList.valueOf(Color.parseColor("#FFC107")));
        }
    }

    private HalalStatus checkHalalStatusDetailed(JSONObject product) {
        try {
            String ingredientsText = product.optString("ingredients_text", "").toLowerCase();
            String productName = product.optString("product_name", "").toLowerCase();
            JSONArray categoriesArray = product.optJSONArray("categories_tags");

            StringBuilder combinedText = new StringBuilder();
            combinedText.append(ingredientsText).append(" ").append(productName);
            if (categoriesArray != null) {
                for (int i = 0; i < categoriesArray.length(); i++) {
                    combinedText.append(" ").append(categoriesArray.getString(i).toLowerCase());
                }
            }

            String combined = combinedText.toString();

            String[] alwaysHaram = {
                    "pork", "swine", "pig", "bacon", "ham", "lard", "pepperoni",
                    "prosciutto", "salami", "chorizo", "pancetta", "gelatin", "gelatine",
                    "alcohol", "ethanol", "wine", "beer", "spirits", "liquor", "vodka",
                    "whiskey", "rum", "brandy", "champagne", "sake", "ethyl alcohol",
                    "alcoholic", "brew", "fermented",
                    "e120", "e441", "e542", "e904", "e124", "e631", "e635",
                    "rennet", "pepsin", "lipase", "animal fat", "beef fat", "tallow",
                    "shortening", "l-cysteine", "animal enzymes", "carmine", "cochineal",
                    "shellac", "blood", "carrion"
            };

            List<String> foundHaram = new ArrayList<>();
            for (String haram : alwaysHaram) {
                if (combined.contains(haram)) {
                    foundHaram.add(haram);
                }
            }

            String[] contextSensitive = {
                    "e471", "e472", "e473", "e474", "e475", "e476", "e477",
                    "e481", "e482", "e483", "mono and diglycerides", "glycerides", "stearic acid"
            };

            for (String item : contextSensitive) {
                if (combined.contains(item) && !(combined.contains("vegetable") || combined.contains("(veg"))) {
                    foundHaram.add(item + " (not clarified)");
                }
            }

            if (!foundHaram.isEmpty()) {
                HalalStatus status = new HalalStatus(HalalStatus.Confidence.HARAM, "Contains haram or unverified ingredients.");
                for (String i : foundHaram) status.addProblematicIngredient(i);
                return status;
            }

            JSONArray labelsArray = product.optJSONArray("labels_tags");
            if (labelsArray != null) {
                for (int i = 0; i < labelsArray.length(); i++) {
                    String label = labelsArray.optString(i, "").toLowerCase();
                    if (label.contains("halal")) {
                        return new HalalStatus(HalalStatus.Confidence.CERTIFIED_HALAL, "Product is labeled as halal.");
                    }
                }
            }

            return new HalalStatus(HalalStatus.Confidence.LIKELY_HALAL, "No haram ingredients detected. Not certified.");
        } catch (Exception e) {
            return new HalalStatus(HalalStatus.Confidence.DOUBTFUL, "Unable to verify all ingredients");
        }
    }

    private String formatAllergens(JSONArray allergensArray) {
        if (allergensArray == null || allergensArray.length() == 0) {
            return "None detected";
        }

        StringBuilder allergensBuilder = new StringBuilder();
        for (int i = 0; i < allergensArray.length(); i++) {
            try {
                String allergen = allergensArray.getString(i)
                        .replace("en:", "")
                        .replace("-", " ");
                allergen = allergen.substring(0, 1).toUpperCase() + allergen.substring(1);

                if (i > 0) allergensBuilder.append(", ");
                allergensBuilder.append(allergen);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return allergensBuilder.toString();
    }

    // Inner class for Halal Status
    public static class HalalStatus {
        public enum Confidence {
            CERTIFIED_HALAL("Certified Halal", "#4CAF50"),
            LIKELY_HALAL("Likely Halal", "#8BC34A"),
            DOUBTFUL("Doubtful", "#FF9800"),
            HARAM("Haram", "#F44336");

            private final String label;
            private final String colorHex;

            Confidence(String label, String colorHex) {
                this.label = label;
                this.colorHex = colorHex;
            }

            public String getLabel() { return label; }
            public String getColorHex() { return colorHex; }
        }

        private Confidence confidence;
        private String reason;
        private List<String> problematicIngredients;

        public HalalStatus(Confidence confidence, String reason) {
            this.confidence = confidence;
            this.reason = reason;
            this.problematicIngredients = new ArrayList<>();
        }

        public void addProblematicIngredient(String ingredient) {
            if (!problematicIngredients.contains(ingredient)) {
                problematicIngredients.add(ingredient);
            }
        }

        public Confidence getConfidence() { return confidence; }
        public String getReason() { return reason; }
        public List<String> getProblematicIngredients() { return problematicIngredients; }
    }
}