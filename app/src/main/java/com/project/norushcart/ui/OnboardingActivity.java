package com.project.norushcart.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.miniproject.myapplication.MainActivity;
import com.miniproject.myapplication.R;
import com.project.norushcart.location.LocationHelper;

public class OnboardingActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (fineGranted || coarseGranted) {
                    LocationHelper.setLocationTrackingEnabled(this, true);
                }
                finishOnboarding();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        MaterialButton btnEnable = findViewById(R.id.btnEnableLocation);
        MaterialButton btnSkip = findViewById(R.id.btnSkipOnboarding);

        btnEnable.setOnClickListener(v -> {
            java.util.List<String> perms = new java.util.ArrayList<>();
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            locationPermissionLauncher.launch(perms.toArray(new String[0]));
        });

        btnSkip.setOnClickListener(v -> {
            finishOnboarding();
        });
    }

    private void finishOnboarding() {
        LocationHelper.setOnboardingCompleted(this, true);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
