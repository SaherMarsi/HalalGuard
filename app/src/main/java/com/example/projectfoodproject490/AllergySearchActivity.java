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
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AllergySearchActivity extends AppCompatActivity {

    // Constants
    private static final int CAMERA_REQUEST_CODE = 1001;
    private static final int GALLERY_REQUEST_CODE = 1003;
    private static final int PERMISSION_REQUEST_CODE = 1004;
    private static final int ALLERGY_FILTER_REQUEST_CODE = 1002;

    private EditText barcodeInput;
    private Button searchBtn, scanBtn, galleryBtn, selectAllergiesBtn;
    private ChipGroup selectedAllergiesChipGroup;
    private TextView resultText;
    private ArrayList<String> selectedAllergies = new ArrayList<>();

    // Product result views
    private MaterialCardView productResultCard;
    private ImageView productImage;
    private TextView productName;
    private Chip allergyStatusChip;
    private TextView halalStatusText;
    private TextView detectedAllergensText;
    private MaterialButton shareBtnProduct, favoriteBtn;

    // Store current product info for sharing and favorites
    private String currentProductName = "";
    private String currentBarcode = "";
    private String currentImageUrl = "";
    private String currentAllergens = "";
    private boolean currentIsHalal = false;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allergy_search);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        barcodeInput = findViewById(R.id.barcodeInput);
        searchBtn = findViewById(R.id.searchBtn);
        scanBtn = findViewById(R.id.scanBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        selectAllergiesBtn = findViewById(R.id.selectAllergiesBtn);
        selectedAllergiesChipGroup = findViewById(R.id.selectedAllergiesChipGroup);
        resultText = findViewById(R.id.resultText);

        // Initialize product result views
        productResultCard = findViewById(R.id.productResultCard);
        productImage = findViewById(R.id.productImage);
        productName = findViewById(R.id.productName);
        allergyStatusChip = findViewById(R.id.allergyStatusChip);
        halalStatusText = findViewById(R.id.halalStatusText);
        detectedAllergensText = findViewById(R.id.detectedAllergensText);
        shareBtnProduct = findViewById(R.id.shareBtnProduct);
        favoriteBtn = findViewById(R.id.favoriteBtn);

        // Select allergies button
        selectAllergiesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AllergyFilterActivity.class);
            intent.putStringArrayListExtra("selectedAllergies", selectedAllergies);
            startActivityForResult(intent, ALLERGY_FILTER_REQUEST_CODE);
        });

        // Search button
        searchBtn.setOnClickListener(v -> {
            String barcode = barcodeInput.getText().toString().trim();
            if (!barcode.isEmpty()) {
                if (selectedAllergies.isEmpty()) {
                    Toast.makeText(this, "Please select allergens first", Toast.LENGTH_SHORT).show();
                } else {
                    searchProductWithAllergies(barcode);
                }
            } else {
                Toast.makeText(this, "Enter a barcode", Toast.LENGTH_SHORT).show();
            }
        });

        // Scan button
        scanBtn.setOnClickListener(v -> {
            if (selectedAllergies.isEmpty()) {
                Toast.makeText(this, "Please select allergens first", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, BarcodeScannerActivity.class);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            }
        });

        // Gallery button
        galleryBtn.setOnClickListener(v -> {
            if (selectedAllergies.isEmpty()) {
                Toast.makeText(this, "Please select allergens first", Toast.LENGTH_SHORT).show();
            } else {
                checkPermissionsAndOpenGallery();
            }
        });

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
                            Toast.makeText(AllergySearchActivity.this, "No barcode found in the image", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Get the first barcode value
                        String barcodeValue = barcodes.get(0).getRawValue();

                        if (barcodeValue != null && !barcodeValue.isEmpty()) {
                            // Set the barcode value in the input field
                            barcodeInput.setText(barcodeValue);

                            // Search for the product
                            searchProductWithAllergies(barcodeValue);
                        } else {
                            Toast.makeText(AllergySearchActivity.this, "Could not read barcode", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AllergySearchActivity.this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Barcode scanned
            String scannedBarcode = data.getStringExtra("scannedBarcode");
            if (scannedBarcode != null) {
                barcodeInput.setText(scannedBarcode);
                searchProductWithAllergies(scannedBarcode);
            }
        } else if (requestCode == ALLERGY_FILTER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Allergies selected
            selectedAllergies = data.getStringArrayListExtra("selectedAllergies");
            updateSelectedAllergiesDisplay();
        } else if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // Gallery image selected
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

    private void updateSelectedAllergiesDisplay() {
        selectedAllergiesChipGroup.removeAllViews();

        if (selectedAllergies != null && !selectedAllergies.isEmpty()) {
            selectAllergiesBtn.setText("Change Allergies (" + selectedAllergies.size() + ")");

            for (String allergy : selectedAllergies) {
                Chip chip = new Chip(this);
                chip.setText(allergy);
                chip.setClickable(false);
                chip.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                selectedAllergiesChipGroup.addView(chip);
            }
        } else {
            selectAllergiesBtn.setText("Select Allergies");
        }
    }

    private void searchProductWithAllergies(String barcode) {
        // Hide the instruction text and show loading
        resultText.setVisibility(View.GONE);

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
                            productResultCard.setVisibility(View.GONE);
                            resultText.setVisibility(View.VISIBLE);
                            resultText.setText("Product not found in database");
                        });
                        return;
                    }

                    JSONObject product = json.getJSONObject("product");

                    String name = product.optString("product_name", "Unknown");
                    String imageUrl = product.optString("image_url", "");
                    JSONArray allergensArray = product.optJSONArray("allergens_tags");

                    // Check for allergens
                    ArrayList<String> detectedAllergens = new ArrayList<>();
                    boolean hasSelectedAllergen = false;
                    String ingredients = product.optString("ingredients_text", "").toLowerCase();
                    if (allergensArray != null) {
                        for (int i = 0; i < allergensArray.length(); i++) {
                            String allergen = allergensArray.getString(i).replace("en:", "");
                            for (String selected : selectedAllergies) {
                                String keyword = selected.toLowerCase().trim();
                                if (allergen.toLowerCase().contains(selected.toLowerCase())) {
                                    hasSelectedAllergen = true;
                                    detectedAllergens.add(selected);
                                } else if (ingredients.contains(keyword)) {
                                    hasSelectedAllergen = true;
                                    if (!detectedAllergens.contains(selected)) {
                                        detectedAllergens.add(selected);
                                    }
                                }
                            }
                        }
                    }


                    // Check halal status
                    boolean isHalal = checkHalalStatus(product);

                    // Store current product info
                    currentProductName = name;
                    currentBarcode = barcode;
                    currentImageUrl = imageUrl;
                    currentIsHalal = isHalal;
                    currentAllergens = formatAllergens(allergensArray);

                    boolean finalHasSelectedAllergen = hasSelectedAllergen;
                    runOnUiThread(() -> {
                        // Show product card
                        productResultCard.setVisibility(View.VISIBLE);

                        // Set product name
                        productName.setText(name);

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

                        // Set allergy status chip
                        if (finalHasSelectedAllergen) {
                            allergyStatusChip.setText("Allergens Detected");
                            allergyStatusChip.setChipBackgroundColorResource(android.R.color.holo_red_dark);
                            allergyStatusChip.setChipIconResource(R.drawable.ic_close);
                            detectedAllergensText.setText("Contains: " + String.join(", ", detectedAllergens));
                            detectedAllergensText.setTextColor(Color.parseColor("#F44336"));
                            detectedAllergensText.setVisibility(View.VISIBLE);
                        } else {
                            allergyStatusChip.setText("No Allergens Detected");
                            allergyStatusChip.setChipBackgroundColorResource(android.R.color.holo_green_dark);
                            allergyStatusChip.setChipIconResource(R.drawable.ic_check);
                            detectedAllergensText.setVisibility(View.GONE);
                        }

                        // Set halal status
                        if (isHalal) {
                            halalStatusText.setText("Halal Status: Likely Halal");
                            halalStatusText.setTextColor(Color.parseColor("#4CAF50"));
                        } else {
                            halalStatusText.setText("Halal Status: May contain haram ingredients");
                            halalStatusText.setTextColor(Color.parseColor("#F44336"));
                        }

                        // Check if product is in favorites
                        checkIfFavorite(barcode);

                        // Save search history
                        saveToHistory();
                    });

                } else {
                    runOnUiThread(() -> {
                        productResultCard.setVisibility(View.GONE);
                        resultText.setVisibility(View.VISIBLE);
                        resultText.setText("Error fetching product information");
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    productResultCard.setVisibility(View.GONE);
                    resultText.setVisibility(View.VISIBLE);
                    resultText.setText("An error occurred: " + e.getMessage());
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

        String shareText = "Product: " + currentProductName + "\n";

        // Add allergy information
        String allergyStatus = allergyStatusChip.getText().toString();
        shareText += "Allergy Status: " + allergyStatus + "\n";

        if (detectedAllergensText.getVisibility() == View.VISIBLE) {
            shareText += detectedAllergensText.getText().toString() + "\n";
        }

        shareText += "Halal Status: " + (currentIsHalal ? "Likely Halal" : "May contain haram ingredients") + "\n";
        shareText += "Allergens: " + currentAllergens + "\n\n";
        shareText += "Shared from HalalScan App";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Product Info: " + currentProductName);
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
            favoriteItem.put("isHalal", currentIsHalal);
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

    private void saveToHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        Map<String, Object> historyItem = new HashMap<>();
        historyItem.put("barcode", currentBarcode);
        historyItem.put("productName", currentProductName);
        historyItem.put("imageUrl", currentImageUrl);
        historyItem.put("allergens", currentAllergens);
        historyItem.put("isHalal", currentIsHalal);
        historyItem.put("timestamp", System.currentTimeMillis());

        FirebaseDatabase.getInstance().getReference("history")
                .child(userId)
                .push()
                .setValue(historyItem);
    }

    private String formatAllergens(JSONArray allergensArray) {
        if (allergensArray == null || allergensArray.length() == 0) {
            return "None";
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

    private boolean checkHalalStatus(JSONObject product) {
        try {
            String ingredientsText = product.optString("ingredients_text", "").toLowerCase();
            String productName = product.optString("product_name", "").toLowerCase();
            JSONArray categoriesArray = product.optJSONArray("categories_tags");

            // Build one combined string of all available text to check
            StringBuilder combinedText = new StringBuilder();
            combinedText.append(ingredientsText).append(" ").append(productName);
            if (categoriesArray != null) {
                for (int i = 0; i < categoriesArray.length(); i++) {
                    combinedText.append(" ").append(categoriesArray.getString(i).toLowerCase());
                }
            }

            String combined = combinedText.toString();

            // Always haram ingredients (no exceptions)
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

            for (String haram : alwaysHaram) {
                if (combined.contains(haram)) {
                    return false;
                }
            }

            // Conditional haram ingredients (skip if "vegetable" is mentioned)
            String[] contextSensitive = {
                    "e471", "e472", "e473", "e474", "e475", "e476", "e477",
                    "e481", "e482", "e483", "mono and diglycerides", "glycerides", "stearic acid"
            };

            for (String item : contextSensitive) {
                if (combined.contains(item)) {
                    if (!(combined.contains("vegetable") || combined.contains("(veg"))) {
                        return false; // no clarification = assume haram
                    }
                }
            }

            return true; // Nothing haram found
        } catch (Exception e) {
            return true; // Assume halal if check fails
        }
    }
}