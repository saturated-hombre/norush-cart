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
import com.miniproject.myapplication.database.DatabaseHelper;
import com.miniproject.myapplication.model.ProductItem;
import com.miniproject.myapplication.service.SchedulerHelper;
import com.miniproject.myapplication.ui.ProductAdapter;
import com.miniproject.myapplication.ui.ShareReceiverActivity;
import com.miniproject.myapplication.ui.WebViewActivity;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ProductAdapter.OnItemActionListener {

    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 101;

    private LinearLayout containerCoolingDown;
    private LinearLayout containerReadyReview;
    private LinearLayout containerSavings;

    private RecyclerView rvCoolingDown;
    private RecyclerView rvReadyReview;
    private RecyclerView rvSavingsHistory;

    private TextView tvEmptyCooling;
    private TextView tvEmptyReady;
    private TextView tvEmptySavings;

    private TextView tvTotalMoneySaved;
    private TextView tvDiscardedCount;

    private BottomNavigationView bottomNavigation;

    private ProductAdapter adapterCoolingDown;
    private ProductAdapter adapterReadyReview;
    private ProductAdapter adapterSavings;

    private DatabaseHelper dbHelper;
    private Handler timerHandler;
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);

        dbHelper = DatabaseHelper.getInstance(this);

        containerCoolingDown = findViewById(R.id.containerCoolingDown);
        containerReadyReview = findViewById(R.id.containerReadyReview);
        containerSavings = findViewById(R.id.containerSavings);

        rvCoolingDown = findViewById(R.id.rvCoolingDown);
        rvReadyReview = findViewById(R.id.rvReadyReview);
        rvSavingsHistory = findViewById(R.id.rvSavingsHistory);

        tvEmptyCooling = findViewById(R.id.tvEmptyCooling);
        tvEmptyReady = findViewById(R.id.tvEmptyReady);
        tvEmptySavings = findViewById(R.id.tvEmptySavings);

        tvTotalMoneySaved = findViewById(R.id.tvTotalMoneySaved);
        tvDiscardedCount = findViewById(R.id.tvDiscardedCount);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        setupRecyclerViews();
        setupBottomNavigation();

        checkNotificationPermission();

        // Check if intent directs us to a specific tab
        int selectedTab = getIntent().getIntExtra("SELECT_TAB", 0);
        selectTab(selectedTab);

        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // Refresh countdown timers and check for auto unlocked items
                refreshData();
                timerHandler.postDelayed(this, 1000); // refresh every 1s
            }
        };
    }

    private void setupRecyclerViews() {
        rvCoolingDown.setLayoutManager(new LinearLayoutManager(this));
        adapterCoolingDown = new ProductAdapter(this, this);
        rvCoolingDown.setAdapter(adapterCoolingDown);

        rvReadyReview.setLayoutManager(new LinearLayoutManager(this));
        adapterReadyReview = new ProductAdapter(this, this);
        rvReadyReview.setAdapter(adapterReadyReview);

        rvSavingsHistory.setLayoutManager(new LinearLayoutManager(this));
        adapterSavings = new ProductAdapter(this, this);
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
            } else if (itemId == R.id.navigation_savings) {
                updateTabVisibility(2);
                return true;
            }
            return false;
        });
    }

    private void updateTabVisibility(int index) {
        containerCoolingDown.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        containerReadyReview.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        containerSavings.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        refreshData();
    }

    private void selectTab(int index) {
        isNavigating = true;
        int targetId = R.id.navigation_cooling_down;
        if (index == 1) {
            targetId = R.id.navigation_ready_review;
        } else if (index == 2) {
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
        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
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

        // Tab 3: Savings Dashboard (Discarded items)
        List<ProductItem> discardedItems = dbHelper.getItemsByStatus(ProductItem.STATUS_DISCARDED);
        adapterSavings.setItems(discardedItems);
        tvEmptySavings.setVisibility(discardedItems.isEmpty() ? View.VISIBLE : View.GONE);

        // Update Savings Totals
        double totalSaved = dbHelper.getTotalMoneySaved();
        int countDiscarded = dbHelper.getDiscardedCount();

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        tvTotalMoneySaved.setText(format.format(totalSaved));
        tvDiscardedCount.setText(countDiscarded + " impulse purchases successfully prevented!");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 100, 0, "Add Link")
                .setIcon(android.R.drawable.ic_input_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 100) {
            showAddLinkDialog();
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

    // ProductAdapter Callbacks
    @Override
    public void onBuyNow(ProductItem item) {
        dbHelper.updateItemStatus(item.getId(), ProductItem.STATUS_PURCHASED);
        refreshData();

        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_URL, item.getOriginalUrl());
        intent.putExtra(WebViewActivity.EXTRA_TITLE, item.getTitle());
        startActivity(intent);
    }

    @Override
    public void onDiscard(ProductItem item) {
        dbHelper.updateItemStatus(item.getId(), ProductItem.STATUS_DISCARDED);
        refreshData();
        Toast.makeText(this, "🎉 Item discarded! You saved " + item.getFormattedPrice() + "!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDebugFastForward(ProductItem item) {
        SchedulerHelper.scheduleDebugFastForward(this, item.getId());
        refreshData();
        Toast.makeText(this, "⚡ Fast-forward active! Cooldown will expire in 10 seconds.", Toast.LENGTH_LONG).show();
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
