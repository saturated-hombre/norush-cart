package com.project.norushcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.miniproject.myapplication.BuildConfig;
import com.miniproject.myapplication.R;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.ProductItem;
import com.project.norushcart.location.LocationHelper;
import com.project.norushcart.network.UrlScraperHelper;
import com.project.norushcart.service.SchedulerHelper;

public class ShareReceiverActivity extends AppCompatActivity {

    private UrlScraperHelper scraperHelper;

    private LinearLayout layoutLoading;
    private LinearLayout layoutContent;
    private TextView tvLoadingStatus;
    private TextView tvRetailer;
    private TextView tvProductTitle;
    private TextView tvProductPrice;
    private TextView tvLockChip;
    private TextView tvLocationChip;
    private View layoutLocationChip;
    private ShapeableImageView ivProductImage;
    private TextView tvHoursOfLife;
    private com.google.android.material.chip.ChipGroup chipGroupEmotions;
    private MaterialButton btnDone;
    private ImageView btnCloseSheet;

    private ProductItem parsedItem = null;
    private Double capturedLat = null;
    private Double capturedLng = null;
    private Float capturedAcc = null;

    private boolean scraperFinished = false;
    private boolean locationFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_sheet);

        // Position window at bottom as bottom sheet modal
        if (getWindow() != null) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            getWindow().setAttributes(params);
        }

        initViews();

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
            startParallelProcessing(rawContent);
        } else {
            Toast.makeText(this, "No valid content shared", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutContent = findViewById(R.id.layoutContent);
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus);
        tvRetailer = findViewById(R.id.tvRetailer);
        tvProductTitle = findViewById(R.id.tvProductTitle);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvLockChip = findViewById(R.id.tvLockChip);
        tvLocationChip = findViewById(R.id.tvLocationChip);
        layoutLocationChip = findViewById(R.id.layoutLocationChip);
        ivProductImage = findViewById(R.id.ivProductImage);
        tvHoursOfLife = findViewById(R.id.tvHoursOfLife);
        chipGroupEmotions = findViewById(R.id.chipGroupEmotions);
        btnDone = findViewById(R.id.btnDone);
        btnCloseSheet = findViewById(R.id.btnCloseSheet);

        if (chipGroupEmotions != null) {
            chipGroupEmotions.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    HapticHelper.tick(group);
                }
            });
        }

        btnCloseSheet.setOnClickListener(v -> {
            HapticHelper.tick(v);
            finish();
        });
        btnDone.setOnClickListener(v -> {
            HapticHelper.confirm(v);
            finish();
        });
    }

    private void startParallelProcessing(String rawContent) {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);
        tvLoadingStatus.setText(R.string.share_sheet_scraping);

        // 1. Parallel Location Fetch with 2s timeout
        LocationHelper.fetchCurrentLocation(this, (latitude, longitude, accuracy) -> {
            capturedLat = latitude;
            capturedLng = longitude;
            capturedAcc = accuracy;
            locationFinished = true;
            checkCompletion();
        });

        // 2. Parallel Product Scraper via JSoup
        scraperHelper.scrapeProduct(rawContent, new UrlScraperHelper.ScrapeCallback() {
            @Override
            public void onSuccess(ProductItem item) {
                if (isFinishing() || isDestroyed()) return;
                parsedItem = item;
                scraperFinished = true;
                checkCompletion();
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ShareReceiverActivity.this,
                        "Failed to parse item: " + errorMessage,
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private synchronized void checkCompletion() {
        if (!scraperFinished || !locationFinished) {
            return;
        }

        if (isFinishing() || isDestroyed()) return;

        if (parsedItem == null) {
            Toast.makeText(this, "Unable to extract product details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Attach captured coordinates & accuracy
        parsedItem.setLatitude(capturedLat);
        parsedItem.setLongitude(capturedLng);
        parsedItem.setLocationAccuracy(capturedAcc);

        // Quarantine calculation based on BuildConfig
        long cooldownDuration = (long) BuildConfig.COOLDOWN_MINUTES_DEBUG * 60L * 1000L;
        long now = System.currentTimeMillis();
        parsedItem.setTimestampAdded(now);
        parsedItem.setUnlockTimestamp(now + cooldownDuration);
        parsedItem.setStatus(ProductItem.STATUS_LOCKED);

        // Persist to SQLite
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        long id = dbHelper.insertItem(parsedItem);
        if (id != -1) {
            parsedItem.setId((int) id);
            SchedulerHelper.scheduleCooldown(this, parsedItem);
        }

        // Present the finished intercepted card
        displayInterceptedProduct(parsedItem);
    }

    private void displayInterceptedProduct(ProductItem item) {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);

        tvRetailer.setText(item.getSourcePlatform() != null ? item.getSourcePlatform() : "Online Store");
        tvProductTitle.setText(item.getTitle());
        tvProductPrice.setText(item.getFormattedPrice());

        // Tactile sensation upon successful quarantine
        HapticHelper.confirm(layoutContent);

        // Calculate "Cost in Hours of Life" if user set an hourly wage
        android.content.SharedPreferences prefs = getSharedPreferences(LocationHelper.PREFS_NAME, MODE_PRIVATE);
        float hourlyWage = prefs.getFloat("hourly_wage", 0f);
        if (hourlyWage > 0 && item.getPrice() > 0 && tvHoursOfLife != null) {
            double hours = item.getPrice() / hourlyWage;
            String formattedHours = String.format(java.util.Locale.getDefault(), "%.1f", hours);
            tvHoursOfLife.setText("⏱️ Cost: ~" + formattedHours + " hours of your hard work. Still worth it?");
            tvHoursOfLife.setVisibility(View.VISIBLE);
        } else if (tvHoursOfLife != null) {
            tvHoursOfLife.setVisibility(View.GONE);
        }

        if (BuildConfig.COOLDOWN_MINUTES_DEBUG <= 2) {
            tvLockChip.setText("⚡ Locked for 2 minutes (Debug)");
        } else {
            tvLockChip.setText("🔒 Locked for 48 hours");
        }

        if (item.hasLocation()) {
            layoutLocationChip.setVisibility(View.VISIBLE);
            if (item.getPlaceLabel() != null && !item.getPlaceLabel().isEmpty()) {
                tvLocationChip.setText("📍 " + item.getPlaceLabel());
            } else {
                tvLocationChip.setText("📍 Trigger Geotagged");
            }
        } else {
            layoutLocationChip.setVisibility(View.GONE);
        }

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.bg_privacy_pill)
                    .error(R.drawable.bg_privacy_pill)
                    .into(ivProductImage);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scraperHelper != null) {
            scraperHelper.shutdown();
        }
    }
}
