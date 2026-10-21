package com.miniproject.myapplication.network;

import android.util.Log;

import com.project.norushcart.data.ProductItem;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkScraper {

    private static final String TAG = "LinkScraper";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * Extracts the first HTTP/HTTPS URL contained within a given text block.
     */
    public static String extractUrl(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile("https?://[\\w\\d\\.\\-_~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return text.trim();
    }

    /**
     * Follows redirects to resolve short URLs (like amzn.to, fkrt.it) to final destination URLs.
     */
    public static String resolveRedirects(String initialUrl) {
        if (initialUrl == null || initialUrl.isEmpty()) return initialUrl;
        String currentUrl = initialUrl;

        try {
            int redirectCount = 0;
            while (redirectCount < 5) {
                URL u = new URL(currentUrl);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("User-Agent", USER_AGENT);
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
                            currentUrl = u.getProtocol() + "://" + u.getHost() + loc;
                        } else {
                            currentUrl = loc;
                        }
                        redirectCount++;
                        continue;
                    }
                }
                break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resolving redirects for " + initialUrl, e);
        }
        return currentUrl;
    }

    /**
     * Detects the platform source based on URL hostname.
     */
    public static String detectPlatform(String url) {
        if (url == null) return "Online Merchant";
        String lower = url.toLowerCase();
        if (lower.contains("amazon") || lower.contains("amzn")) {
            return "Amazon";
        } else if (lower.contains("flipkart") || lower.contains("fkrt")) {
            return "Flipkart";
        } else if (lower.contains("myntra")) {
            return "Myntra";
        } else if (lower.contains("meesho")) {
            return "Meesho";
        }
        return "Online Store";
    }

    /**
     * Scrapes product metadata from the URL using JSoup.
     */
    public static ProductItem scrapeProduct(String rawText) {
        String url = extractUrl(rawText);
        if (url == null || url.isEmpty()) {
            return null;
        }

        String finalUrl = resolveRedirects(url);
        String platform = detectPlatform(finalUrl);

        ProductItem item = new ProductItem();
        item.setOriginalUrl(finalUrl);
        item.setSourcePlatform(platform);

        try {
            Connection connection = Jsoup.connect(finalUrl)
                    .userAgent(USER_AGENT)
                    .referrer("https://www.google.com")
                    .timeout(10000)
                    .followRedirects(true);

            Document doc = connection.get();

            // 1. Extract Title
            String title = extractMetaContent(doc, "og:title");
            if (title.isEmpty()) title = extractMetaContent(doc, "twitter:title");

            if (title.isEmpty() && platform.equalsIgnoreCase("Amazon")) {
                Element titleElem = doc.selectFirst("#productTitle");
                if (titleElem != null) title = titleElem.text().trim();
            }

            if (title.isEmpty() && platform.equalsIgnoreCase("Flipkart")) {
                Element titleElem = doc.selectFirst("span.B_NuT6, h1._6ER2B8, span.VU-m1w");
                if (titleElem != null) title = titleElem.text().trim();
            }

            if (title.isEmpty()) {
                title = doc.title().trim();
            }

            if (title.isEmpty()) {
                title = platform + " Product";
            }

            item.setTitle(cleanTitle(title));

            // 2. Extract Image URL
            String imageUrl = extractMetaContent(doc, "og:image");
            if (imageUrl.isEmpty()) imageUrl = extractMetaContent(doc, "twitter:image");

            if (imageUrl.isEmpty() && platform.equalsIgnoreCase("Amazon")) {
                Element imgElem = doc.selectFirst("#landingImage, #imgBlkFront, #main-image");
                if (imgElem != null) {
                    imageUrl = imgElem.attr("src");
                    if (imageUrl.isEmpty()) imageUrl = imgElem.attr("data-old-hires");
                }
            }

            if (imageUrl.isEmpty() && platform.equalsIgnoreCase("Flipkart")) {
                Element imgElem = doc.selectFirst("img._396cs4, img._2r_T1I, img._53213s");
                if (imgElem != null) {
                    imageUrl = imgElem.attr("src");
                }
            }

            item.setImageUrl(imageUrl);

            // 3. Extract Price
            double price = extractPrice(doc, platform);
            item.setPrice(price);

        } catch (IOException e) {
            Log.e(TAG, "Failed to scrape URL: " + finalUrl, e);
            // Fallback values if offline or blocked
            if (item.getTitle() == null || item.getTitle().isEmpty()) {
                item.setTitle(platform + " Shared Item");
            }
            if (item.getPrice() <= 0) {
                item.setPrice(999.0); // Default placeholder
            }
        }

        return item;
    }

    private static String extractMetaContent(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=" + property + "]");
        if (meta == null) {
            meta = doc.selectFirst("meta[name=" + property + "]");
        }
        return meta != null ? meta.attr("content").trim() : "";
    }

    private static double extractPrice(Document doc, String platform) {
        // Try meta price properties first
        String metaPrice = extractMetaContent(doc, "og:price:amount");
        if (metaPrice.isEmpty()) metaPrice = extractMetaContent(doc, "product:price:amount");
        if (!metaPrice.isEmpty()) {
            double parsed = parseDouble(metaPrice);
            if (parsed > 0) return parsed;
        }

        // Platform specific selectors
        if (platform.equalsIgnoreCase("Amazon")) {
            Element priceElem = doc.selectFirst("span.a-price span.a-offscreen, #priceblock_ourprice, #priceblock_dealprice, span.a-price-whole");
            if (priceElem != null) {
                double parsed = parseDouble(priceElem.text());
                if (parsed > 0) return parsed;
            }
        }

        if (platform.equalsIgnoreCase("Flipkart")) {
            Element priceElem = doc.selectFirst("div._30jeq3, div._16J92d, div.Nx9bqj");
            if (priceElem != null) {
                double parsed = parseDouble(priceElem.text());
                if (parsed > 0) return parsed;
            }
        }

        // Generic fallback: regex search for currency signs (₹, $, Rs.) followed by numbers in document
        String fullText = doc.text();
        Pattern pricePattern = Pattern.compile("(?:₹|Rs\\.?|\\$)\\s*([0-9,]+(?:\\.[0-9]{2})?)");
        Matcher matcher = pricePattern.matcher(fullText);
        if (matcher.find()) {
            double parsed = parseDouble(matcher.group(1));
            if (parsed > 0) return parsed;
        }

        return 499.00; // Default sensible fallback price if non-parsable
    }

    private static double parseDouble(String text) {
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
        // Clean common shop branding suffixes
        return title.replaceAll("(?i) : Amazon\\.[a-z]+| - Flipkart\\.com| - Buy online.*| | Buy .* at Best Prices.*", "")
                .trim();
    }
}
