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
