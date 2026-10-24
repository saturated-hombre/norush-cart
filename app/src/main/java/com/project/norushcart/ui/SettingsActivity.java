package com.project.norushcart.ui;

import android.Manifest;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.miniproject.myapplication.R;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.location.LocationHelper;

public class SettingsActivity extends AppCompatActivity {

    private MaterialSwitch switchTrackLocation;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION)) ||
                        Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (!granted) {
                    switchTrackLocation.setChecked(false);
                    LocationHelper.setLocationTrackingEnabled(this, false);
                    Snackbar.make(switchTrackLocation, "Location permission was denied.", Snackbar.LENGTH_LONG).show();
                } else {
                    LocationHelper.setLocationTrackingEnabled(this, true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchTrackLocation = findViewById(R.id.switchTrackLocation);
        boolean isTracking = LocationHelper.isLocationTrackingEnabled(this) && LocationHelper.hasLocationPermission(this);
        switchTrackLocation.setChecked(isTracking);

        switchTrackLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!LocationHelper.hasLocationPermission(this)) {
                    locationPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                } else {
                    LocationHelper.setLocationTrackingEnabled(this, true);
                }
            } else {
                LocationHelper.setLocationTrackingEnabled(this, false);
            }
        });

        // Find Nearby Stores Switch (Phase 8)
        MaterialSwitch switchFindNearbyStores = findViewById(R.id.switchFindNearbyStores);
        switchFindNearbyStores.setChecked(com.project.norushcart.location.NearbyStoresHelper.isFindNearbyStoresEnabled(this));
        switchFindNearbyStores.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!com.project.norushcart.location.NearbyStoresHelper.hasSeenNearbyStoresDisclaimer(this)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.settings_nearby_disclaimer_title)
                            .setMessage(R.string.settings_nearby_disclaimer_msg)
                            .setPositiveButton(R.string.settings_nearby_disclaimer_agree, (dialog, which) -> {
                                com.project.norushcart.location.NearbyStoresHelper.setSeenNearbyStoresDisclaimer(this, true);
                                com.project.norushcart.location.NearbyStoresHelper.setFindNearbyStoresEnabled(this, true);
                                switchFindNearbyStores.setChecked(true);
                            })
                            .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
                                switchFindNearbyStores.setChecked(false);
                                com.project.norushcart.location.NearbyStoresHelper.setFindNearbyStoresEnabled(this, false);
                            })
                            .setOnCancelListener(dialog -> {
                                switchFindNearbyStores.setChecked(false);
                                com.project.norushcart.location.NearbyStoresHelper.setFindNearbyStoresEnabled(this, false);
                            })
                            .show();
                } else {
                    com.project.norushcart.location.NearbyStoresHelper.setFindNearbyStoresEnabled(this, true);
                }
            } else {
                com.project.norushcart.location.NearbyStoresHelper.setFindNearbyStoresEnabled(this, false);
            }
        });

        View rowClearHistory = findViewById(R.id.rowClearHistory);
        rowClearHistory.setOnClickListener(v -> showClearHistoryDialog());

        // Mindful Economics: Hourly Wage Setting
        android.widget.TextView tvCurrentHourlyWage = findViewById(R.id.tvCurrentHourlyWage);
        android.content.SharedPreferences prefs = getSharedPreferences(LocationHelper.PREFS_NAME, MODE_PRIVATE);
        float savedWage = prefs.getFloat("hourly_wage", 0f);
        if (savedWage > 0) {
            tvCurrentHourlyWage.setText("₹" + (int) savedWage + " / hr");
        } else {
            tvCurrentHourlyWage.setText("Not set");
        }

        View rowHourlyWage = findViewById(R.id.rowHourlyWage);
        rowHourlyWage.setOnClickListener(v -> {
            HapticHelper.tick(v);
            android.widget.EditText input = new android.widget.EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("e.g. 500");
            float current = prefs.getFloat("hourly_wage", 0f);
            if (current > 0) {
                input.setText(String.valueOf((int) current));
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.settings_hourly_wage)
                    .setMessage("Enter your estimated hourly wage/income rate (₹). This converts impulse purchase price tags into the hours of work required to afford them:")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String txt = input.getText().toString().trim();
                        if (!txt.isEmpty()) {
                            try {
                                float val = Float.parseFloat(txt);
                                prefs.edit().putFloat("hourly_wage", val).apply();
                                tvCurrentHourlyWage.setText("₹" + (int) val + " / hr");
                                HapticHelper.confirm(rowHourlyWage);
                            } catch (NumberFormatException ignored) {}
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        });

        // Google Maps Platform Status
        android.widget.TextView tvMapsKeyStatus = findViewById(R.id.tvMapsKeyStatus);
        android.widget.TextView tvMapsPackageInfo = findViewById(R.id.tvMapsPackageInfo);
        boolean hasValidKey = com.project.norushcart.location.NearbyStoresHelper.hasValidApiKey();
        if (hasValidKey) {
            tvMapsKeyStatus.setText(R.string.settings_maps_status_configured);
            tvMapsKeyStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.status_unlocked));
            tvMapsPackageInfo.setText("Package: " + getPackageName() + " · Live SDK connected");
        } else {
            tvMapsKeyStatus.setText(R.string.settings_maps_status_missing);
            tvMapsKeyStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.amber_solar));
            tvMapsPackageInfo.setText("Package: " + getPackageName() + "\nAdd MAPS_API_KEY in local.properties");
        }
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_clear_history_confirm_title)
                .setMessage(R.string.settings_clear_history_confirm_msg)
                .setPositiveButton(R.string.dialog_clear, (dialog, which) -> {
                    DatabaseHelper.getInstance(this).clearLocationHistory();
                    Snackbar.make(switchTrackLocation, R.string.settings_clear_history_success, Snackbar.LENGTH_LONG).show();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
