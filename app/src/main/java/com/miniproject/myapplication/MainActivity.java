package com.miniproject.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.miniproject.myapplication.ui.ProductAdapter;
import com.miniproject.myapplication.ui.WebViewActivity;
import com.project.norushcart.data.ProductItem;
import com.project.norushcart.service.SchedulerHelper;
import com.project.norushcart.ui.CheckoutWebViewActivity;
import com.project.norushcart.ui.CooldownAdapter;
import com.project.norushcart.ui.ImpulseMapFragment;
import com.project.norushcart.ui.ReviewAdapter;
import com.project.norushcart.ui.ShareReceiverActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements CooldownAdapter.OnFastForwardListener, ReviewAdapter.OnReviewActionListener {

    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 101;

    private View containerCoolingDown;
    private View containerReadyReview;
    private View containerSavings;
    private View containerMap;

    private RecyclerView rvCoolingDown;
    private RecyclerView rvReadyReview;
    private RecyclerView rvSavingsHistory;

    private TextView tvEmptyCooling;
    private TextView tvEmptyReady;
    private TextView tvEmptySavings;

    private TextView tvTotalMoneySaved;
    private TextView tvDiscardedCount;
    private com.project.norushcart.ui.WeeklySavingsChartView chartWeeklySavings;

    private BottomNavigationView bottomNavigation;

    private CooldownAdapter adapterCoolingDown;
    private ReviewAdapter adapterReadyReview;
    private com.project.norushcart.ui.SavedAdapter adapterSavings;

    private DatabaseHelper dbHelper;
    private Handler timerHandler;
    private Runnable timerRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!com.project.norushcart.location.LocationHelper.isOnboardingCompleted(this)) {
            startActivity(new Intent(this, com.project.norushcart.ui.OnboardingActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);


        Toolbar toolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);

        dbHelper = DatabaseHelper.getInstance(this);

        containerCoolingDown = findViewById(R.id.containerCoolingDown);
        containerReadyReview = findViewById(R.id.containerReadyReview);
        containerSavings = findViewById(R.id.containerSavings);
        containerMap = findViewById(R.id.containerMap);

        rvCoolingDown = findViewById(R.id.rvCoolingDown);
        rvReadyReview = findViewById(R.id.rvReadyReview);
        rvSavingsHistory = findViewById(R.id.rvSavingsHistory);

        tvEmptyCooling = findViewById(R.id.tvEmptyCooling);
        tvEmptyReady = findViewById(R.id.tvEmptyReady);
        tvEmptySavings = findViewById(R.id.tvEmptySavings);

        tvTotalMoneySaved = findViewById(R.id.tvTotalMoneySaved);
        tvDiscardedCount = findViewById(R.id.tvDiscardedCount);
        chartWeeklySavings = findViewById(R.id.chartWeeklySavings);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        setupRecyclerViews();
        setupBottomNavigation();


        // Attach ImpulseMapFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerMap, new ImpulseMapFragment())
                .commit();

        checkNotificationPermission();

        int selectedTab = getIntent().getIntExtra("SELECT_TAB", 0);
        selectTab(selectedTab);

        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                refreshData();
                timerHandler.postDelayed(this, 1000);
            }
        };
    }

    private void setupRecyclerViews() {
        rvCoolingDown.setLayoutManager(new LinearLayoutManager(this));
        adapterCoolingDown = new CooldownAdapter(this, this);
        rvCoolingDown.setAdapter(adapterCoolingDown);

        rvReadyReview.setLayoutManager(new LinearLayoutManager(this));
        adapterReadyReview = new ReviewAdapter(this, this);
        rvReadyReview.setAdapter(adapterReadyReview);

        rvSavingsHistory.setLayoutManager(new LinearLayoutManager(this));
        adapterSavings = new com.project.norushcart.ui.SavedAdapter(this);
        rvSavingsHistory.setAdapter(adapterSavings);
    }

    private boolean isNavigating = false;

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (isNavigating) return true;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_cooling_down) {
                updateTabVisibility(0);
                return true;
            } else if (itemId == R.id.navigation_ready_review) {
                updateTabVisibility(1);
                return true;
            } else if (itemId == R.id.navigation_map) {
                updateTabVisibility(2);
                return true;
            } else if (itemId == R.id.navigation_savings) {
                updateTabVisibility(3);
                return true;
            }
            return false;
        });
    }

    private void updateTabVisibility(int index) {
        containerCoolingDown.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        containerReadyReview.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        containerMap.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        containerSavings.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        refreshData();
    }

    private void selectTab(int index) {
        isNavigating = true;
        int targetId = R.id.navigation_cooling_down;
        if (index == 1) {
            targetId = R.id.navigation_ready_review;
        } else if (index == 2) {
            targetId = R.id.navigation_map;
        } else if (index == 3) {
            targetId = R.id.navigation_savings;
        }

        bottomNavigation.setSelectedItemId(targetId);
        updateTabVisibility(index);
        isNavigating = false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler.post(timerRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void refreshData() {
        // Tab 1: Locked Cooling Down items
        List<ProductItem> lockedItems = dbHelper.getItemsByStatus(ProductItem.STATUS_LOCKED);
        adapterCoolingDown.setItems(lockedItems);
        tvEmptyCooling.setVisibility(lockedItems.isEmpty() ? View.VISIBLE : View.GONE);

        // Tab 2: Ready for Review (Unlocked) items
        List<ProductItem> unlockedItems = dbHelper.getItemsByStatus(ProductItem.STATUS_UNLOCKED);
        adapterReadyReview.setItems(unlockedItems);
        tvEmptyReady.setVisibility(unlockedItems.isEmpty() ? View.VISIBLE : View.GONE);

        // Tab 4: Savings Dashboard (Discarded items)
        List<ProductItem> discardedItems = dbHelper.getItemsByStatus(ProductItem.STATUS_DISCARDED);
        adapterSavings.setItems(discardedItems);
        tvEmptySavings.setVisibility(discardedItems.isEmpty() ? View.VISIBLE : View.GONE);

        // Update Savings Totals
        double totalSaved = dbHelper.getTotalMoneySaved();
        int countDiscarded = dbHelper.getDiscardedCount();

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        tvTotalMoneySaved.setText(format.format(totalSaved));
        tvDiscardedCount.setText(countDiscarded + " impulse purchases successfully resisted!");

        // Update 6-week progression chart
        List<com.project.norushcart.ui.WeeklySavingsChartView.BarData> weeklyBars = new ArrayList<>();
        long now = System.currentTimeMillis();
        long weekDurationMs = 7L * 24L * 60L * 60L * 1000L;
        for (int i = 5; i >= 0; i--) {
            long windowEnd = now - (i * weekDurationMs);
            long windowStart = windowEnd - weekDurationMs;
            double weekSum = 0.0;
            for (ProductItem item : discardedItems) {
                long t = item.getTimestampAdded();
                if (t >= windowStart && t <= windowEnd) {
                    weekSum += item.getPrice();
                }
            }
            weeklyBars.add(new com.project.norushcart.ui.WeeklySavingsChartView.BarData("W" + (6 - i), weekSum, i == 0));
        }
        if (chartWeeklySavings != null) {
            chartWeeklySavings.setWeeklyData(weeklyBars);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 100, 0, "Add Link")
                .setIcon(android.R.drawable.ic_input_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 101, 1, "Settings")
                .setIcon(R.drawable.ic_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 100) {
            showAddLinkDialog();
            return true;
        } else if (item.getItemId() == 101) {
            startActivity(new Intent(this, com.project.norushcart.ui.SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showAddLinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Intercept Product Link");
        builder.setMessage("Paste an Amazon or Flipkart product URL to place it into the 48-hour quarantine queue:");

        final EditText input = new EditText(this);
        input.setHint("https://www.amazon.in/dp/... or https://dl.flipkart.com/...");
        builder.setView(input);

        builder.setPositiveButton("Quarantine 48h", (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, ShareReceiverActivity.class);
                intent.putExtra("MANUAL_URL", url);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a valid link", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // CooldownAdapter callback
    @Override
    public void onFastForward(ProductItem item) {
        SchedulerHelper.scheduleDebugFastForward(this, item.getId());
        refreshData();
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, "⚡ Fast-forward active! Cooldown expires in 10 seconds.", Snackbar.LENGTH_LONG).show();
        }
    }

    // ReviewAdapter callbacks
    @Override
    public void onDiscard(ProductItem item, int position) {
        refreshData();
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, "🎉 Item discarded! You saved " + item.getFormattedPrice() + "!", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBuyNow(ProductItem item) {
        refreshData();
        Intent intent = new Intent(this, CheckoutWebViewActivity.class);
        intent.putExtra(CheckoutWebViewActivity.EXTRA_ITEM_ID, item.getId());
        intent.putExtra(CheckoutWebViewActivity.EXTRA_URL, item.getOriginalUrl());
        intent.putExtra(CheckoutWebViewActivity.EXTRA_TITLE, item.getTitle());
        startActivity(intent);
    }

    @Override
    public void onFindInStore(ProductItem item) {
        if (!com.project.norushcart.location.NearbyStoresHelper.isFindNearbyStoresEnabled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.settings_find_nearby_stores)
                    .setMessage(R.string.store_search_disabled_msg)
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        startActivity(new Intent(MainActivity.this, com.project.norushcart.ui.SettingsActivity.class));
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
            return;
        }

        String query = com.project.norushcart.location.NearbyStoresHelper.detectStoreQuery(item.getTitle());

        // Check 24-hour cache first
        List<com.project.norushcart.data.NearbyStore> cachedStores = dbHelper.getCachedNearbyStores(item.getId());
        if (!cachedStores.isEmpty()) {
            selectTab(2);
            androidx.fragment.app.Fragment mapFrag = getSupportFragmentManager().findFragmentById(R.id.containerMap);
            if (mapFrag instanceof ImpulseMapFragment) {
                ((ImpulseMapFragment) mapFrag).displayNearbyStores(cachedStores, query);
            }
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                Snackbar.make(rootView, "Showing cached nearby stores (valid for 24h).", Snackbar.LENGTH_SHORT).show();
            }
            return;
        }

        // Fresh location only (never stored coordinates)
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, getString(R.string.store_search_searching), Snackbar.LENGTH_SHORT).show();
        }

        com.project.norushcart.location.LocationHelper.getFreshLocationForStoreSearch(this, (latitude, longitude) -> {
            if (latitude == null || longitude == null) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Location Needed")
                        .setMessage("Could not obtain a fresh GPS fix. Open Google Maps search directly?")
                        .setPositiveButton("Open Maps", (dialog, which) -> {
                            com.project.norushcart.location.NearbyStoresHelper.openGoogleMapsFallback(MainActivity.this, query);
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
                return;
            }

            com.google.android.gms.maps.model.LatLng currentLatLng =
                    new com.google.android.gms.maps.model.LatLng(latitude, longitude);

            com.project.norushcart.location.NearbyStoresHelper.searchNearbyStores(
                    MainActivity.this,
                    item.getId(),
                    query,
                    currentLatLng,
                    new com.project.norushcart.location.NearbyStoresHelper.StoreSearchCallback() {
                        @Override
                        public void onSuccess(List<com.project.norushcart.data.NearbyStore> stores, String queryUsed) {
                            // Cache results for 24 hours in SQLite
                            dbHelper.saveNearbyStoresCache(
                                    item.getId(),
                                    queryUsed,
                                    com.project.norushcart.data.NearbyStore.listToJson(stores),
                                    latitude,
                                    longitude
                            );

                            selectTab(2);
                            androidx.fragment.app.Fragment mapFrag = getSupportFragmentManager().findFragmentById(R.id.containerMap);
                            if (mapFrag instanceof ImpulseMapFragment) {
                                ((ImpulseMapFragment) mapFrag).displayNearbyStores(stores, queryUsed);
                            }
                        }

                        @Override
                        public void onError(String message, boolean fallbackToIntent) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Nearby Stores")
                                    .setMessage(message)
                                    .setPositiveButton(fallbackToIntent ? "Open in Google Maps" : "OK", (dialog, which) -> {
                                        if (fallbackToIntent) {
                                            com.project.norushcart.location.NearbyStoresHelper.openGoogleMapsFallback(MainActivity.this, query);
                                        }
                                    })
                                    .setNegativeButton(fallbackToIntent ? "Cancel" : null, null)
                                    .show();
                        }
                    }
            );
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_NOTIFICATIONS
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
