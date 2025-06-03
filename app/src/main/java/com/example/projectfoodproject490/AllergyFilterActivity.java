package com.example.projectfoodproject490;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AllergyFilterActivity extends AppCompatActivity {

    private Map<String, CheckBox> allergyCheckboxes = new HashMap<>();
    private MaterialButton applyFilterBtn;
    private MaterialButton clearAllBtn;

    private final String[] commonAllergens = {
            "Milk",
            "Eggs",
            "Peanuts",
            "Hazelnuts",
            "Tree nuts",
            "Fish",
            "Shellfish",
            "Wheat",
            "Soy",
            "Sesame",
            "Mustard",
            "Celery",
            "Lupin",
            "Molluscs",
            "Sulphites",
            "Gluten"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allergy_filter);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        LinearLayout allergiesContainer = findViewById(R.id.allergiesContainer);
        applyFilterBtn = findViewById(R.id.applyFilterBtn);
        clearAllBtn = findViewById(R.id.clearAllBtn);

        for (String allergen : commonAllergens) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(allergen);
            checkBox.setTextSize(16);
            checkBox.setPadding(0, 16, 0, 16);
            allergiesContainer.addView(checkBox);
            allergyCheckboxes.put(allergen, checkBox);
        }

        ArrayList<String> selectedAllergies = getIntent().getStringArrayListExtra("selectedAllergies");
        if (selectedAllergies != null) {
            for (String allergy : selectedAllergies) {
                CheckBox checkBox = allergyCheckboxes.get(allergy);
                if (checkBox != null) {
                    checkBox.setChecked(true);
                }
            }
        }

        applyFilterBtn.setOnClickListener(v -> {
            ArrayList<String> selected = new ArrayList<>();
            for (Map.Entry<String, CheckBox> entry : allergyCheckboxes.entrySet()) {
                if (entry.getValue().isChecked()) {
                    selected.add(entry.getKey());
                }
            }

            if (selected.isEmpty()) {
                Toast.makeText(this, "Please select at least one allergen", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent resultIntent = new Intent();
            resultIntent.putStringArrayListExtra("selectedAllergies", selected);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        clearAllBtn.setOnClickListener(v -> {
            for (CheckBox checkBox : allergyCheckboxes.values()) {
                checkBox.setChecked(false);
            }
        });
    }
}
