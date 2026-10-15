package com.miniproject.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.miniproject.myapplication.MainActivity;
import com.miniproject.myapplication.R;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.miniproject.myapplication.model.ProductItem;
import com.miniproject.myapplication.network.LinkScraper;
import com.miniproject.myapplication.service.SchedulerHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShareReceiverActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvStatus;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_receiver);

        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && "text/plain".equals(type)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                processSharedLink(sharedText);
            } else {
                Toast.makeText(this, "No content shared", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (intent.hasExtra("MANUAL_URL")) {
            String url = intent.getStringExtra("MANUAL_URL");
            processSharedLink(url);
        } else {
            Toast.makeText(this, "Invalid share action", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void processSharedLink(String sharedContent) {
        tvStatus.setText("Extracting link & product details...");
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            // Background thread execution
            ProductItem item = LinkScraper.scrapeProduct(sharedContent);

            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (item != null && item.getOriginalUrl() != null && !item.getOriginalUrl().isEmpty()) {
                    DatabaseHelper dbHelper = DatabaseHelper.getInstance(ShareReceiverActivity.this);
                    long id = dbHelper.insertItem(item);

                    if (id != -1) {
                        SchedulerHelper.scheduleCooldown(ShareReceiverActivity.this, item);
                        Toast.makeText(ShareReceiverActivity.this,
                                "Quarantined for 48 Hours: " + item.getTitle(),
                                Toast.LENGTH_LONG).show();

                        // Launch main activity showing tab 0 (Cooling down)
                        Intent mainIntent = new Intent(ShareReceiverActivity.this, MainActivity.class);
                        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mainIntent);
                    } else {
                        Toast.makeText(ShareReceiverActivity.this, "Failed to save item.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ShareReceiverActivity.this, "Could not extract a valid URL.", Toast.LENGTH_SHORT).show();
                }

                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
