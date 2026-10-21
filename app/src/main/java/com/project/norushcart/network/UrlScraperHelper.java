package com.project.norushcart.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.project.norushcart.data.ProductItem;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlScraperHelper {

    private static final String TAG = "UrlScraperHelper";
    private static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-S908B) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36";

    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface ScrapeCallback {
        void onSuccess(ProductItem item);
        void onError(String errorMessage);
    }

    public UrlScraperHelper() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Uses regex to pull out valid HTTP/HTTPS URLs from promotional share text.
     */
    public static String extractCleanUrl(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile("https?://[\\w\\d\\.\\-_~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }

    /**
     * Resolves short URLs (like amzn.to, fkrt.it) following HTTP 301/302 redirects.
     */
    public static String resolveRedirects(String initialUrl) {
        if (initialUrl == null || initialUrl.isEmpty()) return initialUrl;
        String currentUrl = initialUrl;

        try {
            int redirectCount = 0;
            while (redirectCount < 5) {
                URL url = new URL(currentUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("User-Agent", MOBILE_USER_AGENT);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.connect();

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_MOVED_PERM
                        || code == HttpURLConnection.HTTP_MOVED_TEMP
                        || code == HttpURLConnection.HTTP_SEE_OTHER
                        || code == 307 || code == 308) {
                    String loc = conn.getHeaderField("Location");
                    if (loc != null && !loc.isEmpty()) {
                        if (loc.startsWith("/")) {
                            currentUrl = url.getProtocol() + "://" + url.getHost() + loc;
                        } else {
                            currentUrl = loc;
                        }
                        redirectCount++;
                        conn.disconnect();
                        continue;
                    }
                }
                conn.disconnect();
                break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resolving redirects for " + initialUrl, e);
        }
        return currentUrl;
    }

    /**
     * Detects source platform ("Amazon" or "Flipkart") based on URL hostname.
     */
    public static String detectPlatform(String url) {
        if (url == null) return "Amazon";
        String lower = url.toLowerCase();
        if (lower.contains("flipkart") || lower.contains("fkrt")) {
            return "Flipkart";
        }
        return "Amazon";
    }

    /**
     * Asynchronously scrapes product metadata on a background thread and delivers result on main thread.
     */
    public void scrapeProduct(String rawUrl, ScrapeCallback callback) {
        executorService.execute(() -> {
            String cleanUrl = extractCleanUrl(rawUrl);
            if (cleanUrl == null || cleanUrl.isEmpty()) {
                postError(callback, "No valid HTTP/HTTPS URL found in input.");
                return;
            }

            String finalUrl = resolveRedirects(cleanUrl);
            String platform = detectPlatform(finalUrl);

            try {
                Connection connection = Jsoup.connect(finalUrl)
                        .userAgent(MOBILE_USER_AGENT)
                        .referrer("https://www.google.com")
                        .timeout(10000)
                        .followRedirects(true);

                Document doc = connection.get();

                // 1. Title Extraction (OpenGraph -> Fallback DOM -> Title)
                String title = extractMetaContent(doc, "og:title");
                if (title.isEmpty()) title = extractMetaContent(doc, "twitter:title");

                if (title.isEmpty() && "Amazon".equalsIgnoreCase(platform)) {
                    Element titleElem = doc.selectFirst("#productTitle");
                    if (titleElem != null) title = titleElem.text().trim();
                } else if (title.isEmpty() && "Flipkart".equalsIgnoreCase(platform)) {
                    Element titleElem = doc.selectFirst("span.B_NuT6, h1._6ER2B8, span.VU-m1w");
                    if (titleElem != null) title = titleElem.text().trim();
                }

                if (title.isEmpty()) {
                    title = doc.title().trim();
                }

                if (title.isEmpty()) {
                    title = platform + " Product";
                }

                title = cleanTitle(title);

                // 2. Image Extraction (OpenGraph -> Fallback DOM)
                String imageUrl = extractMetaContent(doc, "og:image");
                if (imageUrl.isEmpty()) imageUrl = extractMetaContent(doc, "twitter:image");

                if (imageUrl.isEmpty() && "Amazon".equalsIgnoreCase(platform)) {
                    Element imgElem = doc.selectFirst("#landingImage, #imgBlkFront, #main-image");
                    if (imgElem != null) {
                        imageUrl = imgElem.attr("src");
                        if (imageUrl.isEmpty()) imageUrl = imgElem.attr("data-old-hires");
                    }
                } else if (imageUrl.isEmpty() && "Flipkart".equalsIgnoreCase(platform)) {
                    Element imgElem = doc.selectFirst("img._396cs4, img._2r_T1I, img._53213s");
                    if (imgElem != null) {
                        imageUrl = imgElem.attr("src");
                    }
                }

                // 3. Price Extraction (OpenGraph -> Selectors -> Pattern matching)
                double price = extractPrice(doc, platform);

                ProductItem item = new ProductItem();
                item.setTitle(title);
                item.setImageUrl(imageUrl);
                item.setPrice(price);
                item.setSourcePlatform(platform);
                item.setOriginalUrl(finalUrl);

                postSuccess(callback, item);

            } catch (IOException e) {
                Log.e(TAG, "Failed to scrape product from URL: " + finalUrl, e);
                postError(callback, "Failed to fetch product details: " + e.getLocalizedMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during scraping", e);
                postError(callback, "An unexpected error occurred: " + e.getLocalizedMessage());
            }
        });
    }

    private static String extractMetaContent(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=" + property + "]");
        if (meta == null) {
            meta = doc.selectFirst("meta[name=" + property + "]");
        }
        return meta != null ? meta.attr("content").trim() : "";
    }

    private static double extractPrice(Document doc, String platform) {
        String metaPrice = extractMetaContent(doc, "og:price:amount");
        if (metaPrice.isEmpty()) metaPrice = extractMetaContent(doc, "product:price:amount");
        if (!metaPrice.isEmpty()) {
            double parsed = parseCurrencyDouble(metaPrice);
            if (parsed > 0) return parsed;
        }

        if ("Amazon".equalsIgnoreCase(platform)) {
            Element priceElem = doc.selectFirst("span.a-price-whole, span.a-price span.a-offscreen, #priceblock_ourprice, #priceblock_dealprice");
            if (priceElem != null) {
                double parsed = parseCurrencyDouble(priceElem.text());
                if (parsed > 0) return parsed;
            }
        } else if ("Flipkart".equalsIgnoreCase(platform)) {
            Element priceElem = doc.selectFirst("div._30jeq3, div._16J92d, div.Nx9bqj");
            if (priceElem != null) {
                double parsed = parseCurrencyDouble(priceElem.text());
                if (parsed > 0) return parsed;
            }
        }

        // Generic fallback regex search for currency signs
        String fullText = doc.text();
        Pattern pricePattern = Pattern.compile("(?:₹|Rs\\.?|\\$)\\s*([0-9,]+(?:\\.[0-9]{2})?)");
        Matcher matcher = pricePattern.matcher(fullText);
        if (matcher.find()) {
            double parsed = parseCurrencyDouble(matcher.group(1));
            if (parsed > 0) return parsed;
        }

        return 0.0;
    }

    private static double parseCurrencyDouble(String text) {
        if (text == null) return 0.0;
        try {
            String cleaned = text.replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) return 0.0;
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String cleanTitle(String title) {
        if (title == null) return "";
        return title.replaceAll("(?i) : Amazon\\.[a-z]+| - Flipkart\\.com| - Buy online.*", "").trim();
    }

    private void postSuccess(ScrapeCallback callback, ProductItem item) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onSuccess(item);
            }
        });
    }

    private void postError(ScrapeCallback callback, String errorMessage) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onError(errorMessage);
            }
        });
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
