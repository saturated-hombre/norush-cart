package com.project.norushcart.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.miniproject.myapplication.R;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.ProductItem;

public class CheckoutWebViewActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "extra_item_id";
    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";

    private WebView webViewCheckout;
    private LinearProgressIndicator progressBarCheckout;
    private DatabaseHelper dbHelper;

    private int itemId = -1;
    private String originalUrl;
    private String productTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_webview);

        dbHelper = DatabaseHelper.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbarCheckout);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        webViewCheckout = findViewById(R.id.webViewCheckout);
        progressBarCheckout = findViewById(R.id.progressBarCheckout);

        Intent intent = getIntent();
        itemId = intent.getIntExtra(EXTRA_ITEM_ID, -1);
        originalUrl = intent.getStringExtra(EXTRA_URL);
        productTitle = intent.getStringExtra(EXTRA_TITLE);

        if (productTitle != null && !productTitle.isEmpty()) {
            setTitle(productTitle);
        } else {
            setTitle("Merchant Store");
        }

        if (originalUrl == null || originalUrl.isEmpty()) {
            Toast.makeText(this, "Invalid merchant URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupWebView();
        setupBackPressedHandler();

        showReflectionDialog();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webViewCheckout.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webViewCheckout.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false; // Keep browsing inside app's WebView
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBarCheckout.setVisibility(View.GONE);
            }
        });

        webViewCheckout.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBarCheckout.setVisibility(View.VISIBLE);
                    progressBarCheckout.setProgress(newProgress);
                } else {
                    progressBarCheckout.setVisibility(View.GONE);
                }
            }
        });
    }

    private void showReflectionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("48-Hour Buffer Elapsed")
                .setMessage("You took 2 days to reflect. Are you sure you still want to buy this?")
                .setCancelable(false)
                .setPositiveButton("Proceed to Checkout", (dialog, which) -> {
                    if (itemId != -1) {
                        dbHelper.updateItemStatus(itemId, ProductItem.STATUS_PURCHASED);
                    }
                    webViewCheckout.loadUrl(originalUrl);
                })
                .setNegativeButton("Changed My Mind", (dialog, which) -> {
                    if (itemId != -1) {
                        dbHelper.updateItemStatus(itemId, ProductItem.STATUS_DISCARDED);
                    }
                    Toast.makeText(CheckoutWebViewActivity.this, "🎉 Great decision! You saved money.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .show();
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webViewCheckout != null && webViewCheckout.canGoBack()) {
                    webViewCheckout.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Open in External Browser")
                .setIcon(android.R.drawable.ic_menu_compass)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 1) {
            if (originalUrl != null) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(originalUrl));
                startActivity(browserIntent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
