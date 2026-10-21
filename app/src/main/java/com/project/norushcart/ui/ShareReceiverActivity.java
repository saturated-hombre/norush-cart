package com.project.norushcart.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.ProductItem;
import com.project.norushcart.network.UrlScraperHelper;
import com.project.norushcart.service.SchedulerHelper;

public class ShareReceiverActivity extends AppCompatActivity {

    private AlertDialog progressDialog;
    private UrlScraperHelper scraperHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scraperHelper = new UrlScraperHelper();

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        String rawContent = null;

        if (Intent.ACTION_SEND.equals(action) && type != null && "text/plain".equals(type)) {
            rawContent = intent.getStringExtra(Intent.EXTRA_TEXT);
        } else if (intent != null && intent.hasExtra("MANUAL_URL")) {
            rawContent = intent.getStringExtra("MANUAL_URL");
        }

        if (rawContent != null && !rawContent.trim().isEmpty()) {
            showProgressDialog();
            processLink(rawContent);
        } else {
            Toast.makeText(this, "No valid content shared", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showProgressDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(50, 50, 50, 50);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        layout.addView(progressBar);

        TextView tvMessage = new TextView(this);
        tvMessage.setText("Interception Active: Quarantining item...");
        tvMessage.setTextSize(16);
        tvMessage.setPadding(30, 0, 0, 0);
        layout.addView(tvMessage);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setCancelable(false);

        progressDialog = builder.create();
        progressDialog.show();
    }

    private void processLink(String rawContent) {
        scraperHelper.scrapeProduct(rawContent, new UrlScraperHelper.ScrapeCallback() {
            @Override
            public void onSuccess(ProductItem item) {
                if (isFinishing() || isDestroyed()) return;

                dismissProgressDialog();

                if (item != null) {
                    fetchLocationAndSave(item);
                } else {
                    Toast.makeText(ShareReceiverActivity.this,
                            "Unable to parse product details.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;

                dismissProgressDialog();
                Toast.makeText(ShareReceiverActivity.this,
                        "Failed to quarantine item: " + errorMessage,
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void fetchLocationAndSave(ProductItem item) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            try {
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                item.setLatitude(location.getLatitude());
                                item.setLongitude(location.getLongitude());
                            } else {
                                item.setLatitude(0.0);
                                item.setLongitude(0.0);
                            }
                            saveAndFinish(item);
                        })
                        .addOnFailureListener(this, e -> {
                            item.setLatitude(0.0);
                            item.setLongitude(0.0);
                            saveAndFinish(item);
                        });
            } catch (Exception e) {
                item.setLatitude(0.0);
                item.setLongitude(0.0);
                saveAndFinish(item);
            }
        } else {
            item.setLatitude(0.0);
            item.setLongitude(0.0);
            saveAndFinish(item);
        }
    }

    private void saveAndFinish(ProductItem item) {
        long now = System.currentTimeMillis();
        item.setTimestampAdded(now);
        item.setUnlockTimestamp(now + 172800000L); // 48 hours in milliseconds
        item.setStatus(ProductItem.STATUS_LOCKED);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(ShareReceiverActivity.this);
        long id = dbHelper.insertItem(item);

        if (id != -1) {
            item.setId((int) id);
            SchedulerHelper.scheduleCooldown(ShareReceiverActivity.this, item);
            Toast.makeText(ShareReceiverActivity.this,
                    "Item quarantined for 48 hours in NoRush Cart",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(ShareReceiverActivity.this,
                    "Failed to save item to quarantine queue.",
                    Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
        if (scraperHelper != null) {
            scraperHelper.shutdown();
        }
    }
}
