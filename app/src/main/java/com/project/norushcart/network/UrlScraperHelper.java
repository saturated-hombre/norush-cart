package com.project.norushcart.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.project.norushcart.data.ProductItem;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlScraperHelper {

    private static final String TAG = "UrlScraperHelper";
    private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

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
     * Extracts a clean product title from raw promotional share text.
     * E.g. "Buy Fresho Apple Online at Best Price of Rs 140.00 - bigbasket: https://..."
     * or "Check out Fortune Sunlite Refined Sunflower Oil on Blinkit: https://..."
     */
    public static String extractTitleFromShareText(String rawText, String cleanUrl) {
        if (rawText == null || rawText.trim().isEmpty()) return "";
        String text = rawText;
        if (cleanUrl != null && !cleanUrl.isEmpty()) {
            text = text.replace(cleanUrl, "");
        }
        // Remove trailing or leading URLs or query parameters
        text = text.replaceAll("https?://\\S+", "");

        // Strip price mentions like "at ₹135", "for Rs 140.00", "₹120", "Rs. 99"
        text = text.replaceAll("(?i)\\b(?:at|for|only)?\\s*(?:₹|rs\\.?|inr)\\s*[0-9,]+(?:\\.[0-9]{1,2})?", "");

        // Strip common promotional prefixes
        text = text.replaceAll("(?i)^(?:hey,?\\s*)?(?:check out(?: this product)?|take a look at(?: this)?|found this on|look what i found on|just found this product on|buy|deal of the day:?)\\s*(?:on\\s+)?:?\\s*", "");
        text = text.replaceAll("(?i)^(?:zepto|blinkit|bigbasket|flipkart|amazon|myntra|ajio|jiomart|croma|nykaa)\\s*:\\s*", "");

        // Strip common platform suffixes
        text = text.replaceAll("(?i)\\s+(?:on\\s+)?(?:blinkit|bigbasket|zepto|instamart|swiggy|flipkart|amazon|myntra|ajio|jiomart|meesho|croma|nykaa).*$", "");
        text = text.replaceAll("(?i)\\s*-\\s*(?:bigbasket|blinkit|zepto|flipkart|amazon|myntra|ajio|jiomart|meesho|croma|nykaa).*$", "");
        text = text.replaceAll("(?i)\\s*(?:online at best price).*$", "");

        text = text.replaceAll("[:\\-–—|]$", "").trim();
        text = text.replaceAll("^[:\\-–—|]", "").trim();

        if (text.length() >= 4 && !text.matches("(?i)^(https?://|www\\.).*")) {
            return text;
        }
        return "";
    }

    /**
     * Resolves short URLs (like amzn.to, fkrt.it, blinkit, zepto onelink) following HTTP redirects.
     * Also inspects query parameters for embedded destination web URLs.
     */
    public static String resolveRedirects(String initialUrl) {
        if (initialUrl == null || initialUrl.isEmpty()) return initialUrl;
        String currentUrl = initialUrl;

        // Check for embedded destination URLs in query parameters (AppsFlyer, Branch, Firebase)
        try {
            if (currentUrl.contains("af_web_dp=") || currentUrl.contains("redirect_url=") || currentUrl.contains("url=http")) {
                Pattern p = Pattern.compile("(?:af_web_dp|redirect_url|dest|destination|url)=(https?%3A%2F%2F[^&]+|https?://[^&]+)", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(currentUrl);
                if (m.find()) {
                    String decoded = URLDecoder.decode(m.group(1), "UTF-8");
                    if (decoded.startsWith("http")) {
                        currentUrl = decoded;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            int redirectCount = 0;
            while (redirectCount < 8) {
                URL url = new URL(currentUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("User-Agent", DESKTOP_USER_AGENT);
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
                        // Non-HTTP app scheme redirect (e.g. intent://zepto...): check for fallback web url
                        if (!loc.startsWith("http://") && !loc.startsWith("https://") && !loc.startsWith("/")) {
                            if (loc.contains("browser_fallback_url=")) {
                                Pattern fbPattern = Pattern.compile("browser_fallback_url=(https?%3A%2F%2F[^;]+|https?://[^;]+)");
                                Matcher fbMatcher = fbPattern.matcher(loc);
                                if (fbMatcher.find()) {
                                    currentUrl = URLDecoder.decode(fbMatcher.group(1), "UTF-8");
                                    redirectCount++;
                                    conn.disconnect();
                                    continue;
                                }
                            }
                            // Otherwise stop redirects and use current valid HTTP url
                            conn.disconnect();
                            break;
                        }
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
     * Normalizes Amazon URLs to canonical product URLs if an ASIN (10-char code) is present.
     */
    private static String normalizeAmazonUrl(String url) {
        if (url == null) return "";
        if (url.toLowerCase().contains("amazon") || url.toLowerCase().contains("amzn")) {
            String asin = extractAsin(url);
            if (asin != null) {
                String domain = "www.amazon.in";
                if (url.contains("amazon.com")) domain = "www.amazon.com";
                else if (url.contains("amazon.co.uk")) domain = "www.amazon.co.uk";
                return "https://" + domain + "/dp/" + asin;
            }
        }
        return url;
    }

    /**
     * Detects source platform accurately for all major Indian e-commerce, quick-commerce,
     * fashion, electronics, and grocery websites, with a clean hostname fallback.
     */
    public static String detectPlatform(String url) {
        if (url == null || url.trim().isEmpty()) return "Online Store";
        String lower = url.toLowerCase();

        // 1. Quick Commerce & Grocery
        if (lower.contains("blinkit") || lower.contains("grofers")) {
            return "Blinkit";
        } else if (lower.contains("bigbasket") || lower.contains("bbinstant") || lower.contains("bbdaily")) {
            return "BigBasket";
        } else if (lower.contains("zeptonow") || lower.contains("zepto")) {
            return "Zepto";
        } else if (lower.contains("swiggy") || lower.contains("instamart")) {
            return "Swiggy Instamart";
        } else if (lower.contains("jiomart")) {
            return "JioMart";
        } else if (lower.contains("dunzo")) {
            return "Dunzo";
        }

        // 2. E-Commerce & Marketplaces
        else if (lower.contains("flipkart") || lower.contains("fkrt")) {
            return "Flipkart";
        } else if (lower.contains("amazon") || lower.contains("amzn")) {
            return "Amazon";
        } else if (lower.contains("meesho")) {
            return "Meesho";
        } else if (lower.contains("snapdeal")) {
            return "Snapdeal";
        } else if (lower.contains("indiamart")) {
            return "IndiaMART";
        }

        // 3. Fashion & Beauty
        else if (lower.contains("myntra")) {
            return "Myntra";
        } else if (lower.contains("ajio")) {
            return "Ajio";
        } else if (lower.contains("nykaa")) {
            return "Nykaa";
        } else if (lower.contains("tatacliq") || lower.contains("tata cliq")) {
            return "Tata CLiQ";
        } else if (lower.contains("tatadigital")) {
            return "Tata Neu";
        } else if (lower.contains("purplle")) {
            return "Purplle";
        } else if (lower.contains("bewakoof")) {
            return "Bewakoof";
        } else if (lower.contains("urbanic")) {
            return "Urbanic";
        }

        // 4. Electronics & Tech
        else if (lower.contains("croma")) {
            return "Croma";
        } else if (lower.contains("reliancedigital") || lower.contains("reliance digital")) {
            return "Reliance Digital";
        } else if (lower.contains("vijaysales") || lower.contains("vijay sales")) {
            return "Vijay Sales";
        } else if (lower.contains("boat-lifestyle") || lower.contains("boat")) {
            return "boAt";
        } else if (lower.contains("gonoise") || lower.contains("noise")) {
            return "Noise";
        } else if (lower.contains("oneplus")) {
            return "OnePlus";
        } else if (lower.contains("mi.com") || lower.contains("xiaomi")) {
            return "Xiaomi";
        } else if (lower.contains("samsung")) {
            return "Samsung";
        } else if (lower.contains("apple.com")) {
            return "Apple";
        }

        // 5. Health & Pharmacy
        else if (lower.contains("pharmeasy")) {
            return "PharmEasy";
        } else if (lower.contains("1mg")) {
            return "Tata 1mg";
        } else if (lower.contains("netmeds")) {
            return "Netmeds";
        } else if (lower.contains("apollo")) {
            return "Apollo Pharmacy";
        }

        // 6. Popular Specialty Stores
        else if (lower.contains("firstcry")) {
            return "FirstCry";
        } else if (lower.contains("lenskart")) {
            return "Lenskart";
        } else if (lower.contains("decathlon")) {
            return "Decathlon";
        } else if (lower.contains("ikea")) {
            return "IKEA";
        }

        // 7. Clean Domain Host Fallback
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null) {
                host = host.replaceFirst("^(?i)www\\.", "").replaceFirst("^(?i)m\\.", "");
                int dotIdx = host.indexOf('.');
                if (dotIdx > 0) {
                    String name = host.substring(0, dotIdx);
                    if (name.length() > 2) {
                        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "Online Store";
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

            String shareTextTitle = extractTitleFromShareText(rawUrl, cleanUrl);
            double textPrice = extractPriceFromText(rawUrl);

            String finalUrl = normalizeAmazonUrl(resolveRedirects(cleanUrl));
            String platform = detectPlatform(finalUrl);
            String asin = extractAsin(finalUrl);
            String urlTitle = extractTitleFromUrl(finalUrl);

            try {
                Connection connection = Jsoup.connect(finalUrl)
                        .userAgent(DESKTOP_USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "en-IN,en;q=0.9")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"")
                        .header("Sec-Ch-Ua-Mobile", "?0")
                        .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "none")
                        .header("Sec-Fetch-User", "?1")
                        .referrer("https://www.google.co.in")
                        .ignoreHttpErrors(true)
                        .timeout(12000)
                        .followRedirects(true);

                Document doc = connection.get();

                // Check if JSON-LD metadata exists
                String jsonTitle = "";
                double jsonPrice = 0.0;
                String jsonImage = "";

                Elements jsonLdScripts = doc.select("script[type=application/ld+json]");
                for (Element script : jsonLdScripts) {
                    try {
                        String jsonText = script.html().trim();
                        if (jsonText.isEmpty()) continue;

                        if (jsonText.startsWith("[")) {
                            JSONArray array = new JSONArray(jsonText);
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.optJSONObject(i);
                                if (obj != null) {
                                    if (jsonTitle.isEmpty()) jsonTitle = parseJsonLdTitle(obj);
                                    if (jsonPrice <= 0) jsonPrice = parseJsonLdPrice(obj);
                                    if (jsonImage.isEmpty()) jsonImage = parseJsonLdImage(obj);
                                }
                            }
                        } else if (jsonText.startsWith("{")) {
                            JSONObject obj = new JSONObject(jsonText);
                            if (jsonTitle.isEmpty()) jsonTitle = parseJsonLdTitle(obj);
                            if (jsonPrice <= 0) jsonPrice = parseJsonLdPrice(obj);
                            if (jsonImage.isEmpty()) jsonImage = parseJsonLdImage(obj);
                        }
                    } catch (Exception ignored) {
                    }
                }

                // 1. Title Extraction
                String title = jsonTitle;
                if (title.isEmpty()) title = extractMetaContent(doc, "og:title");
                if (title.isEmpty()) title = extractMetaContent(doc, "twitter:title");

                if (title.isEmpty()) {
                    Element titleElem = null;
                    if ("Amazon".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("#productTitle, h1#title, #title span");
                    } else if ("Flipkart".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("span.B_NuT6, h1._6ER2B8, span.VU-m1w");
                    } else if ("Blinkit".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1[class*='ProductTitle'], div[class*='ProductTitle'], div[data-test-id='pdp-title'], h1");
                    } else if ("BigBasket".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1[class*='Description___StyledH'], h1[class*='ProductTitle'], div[class*='ProductDescription'] h1, h1");
                    } else if ("Zepto".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1[data-testid='pdp-title'], h1[class*='pdp-title'], h1");
                    } else if ("Swiggy Instamart".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("div[class*='item-name'], h1");
                    } else if ("JioMart".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("div.title-section h1, h1.pdp-title, h1");
                    } else if ("Myntra".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1.pdp-title, h1.pdp-name, div.pdp-price-info h1");
                    } else if ("Ajio".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1.prod-title, div.prod-name");
                    } else if ("Nykaa".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1[class*='product-title'], h1[class*='css-1gc4x7i'], h1");
                    } else if ("Tata CLiQ".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1.ProductDetailsMainCard__productName, h1");
                    } else if ("Croma".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1.pd-title, h1");
                    } else if ("Reliance Digital".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1.pdp__title, h1");
                    } else if ("Vijay Sales".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1.pdp-title, h1");
                    } else if ("Meesho".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1[class*='ProductTitle'], span[class*='ProductTitle'], h1");
                    } else if ("Snapdeal".equalsIgnoreCase(platform)) {
                        titleElem = doc.selectFirst("h1.pdp-e-i-head, h1");
                    }
                    if (titleElem == null) {
                        titleElem = doc.selectFirst("h1");
                    }
                    if (titleElem != null) {
                        title = titleElem.text().trim();
                    }
                }

                if (title.isEmpty() || title.toLowerCase().contains("robot check")
                        || title.toLowerCase().contains("captcha")
                        || title.toLowerCase().contains("access denied")
                        || title.equalsIgnoreCase("Amazon Product")
                        || title.equalsIgnoreCase("Flipkart")
                        || title.equalsIgnoreCase(platform)) {
                    if (!shareTextTitle.isEmpty()) {
                        title = shareTextTitle;
                    } else if (!urlTitle.isEmpty()) {
                        title = urlTitle;
                    } else {
                        String docTitle = doc.title().trim();
                        if (!docTitle.isEmpty() && !docTitle.toLowerCase().contains("captcha")) {
                            title = docTitle;
                        }
                    }
                }

                if (title.isEmpty()) {
                    title = !shareTextTitle.isEmpty() ? shareTextTitle : (!urlTitle.isEmpty() ? urlTitle : platform + " Product");
                }

                title = cleanTitle(title);

                // 2. Image Extraction
                String imageUrl = jsonImage;
                if (imageUrl.isEmpty()) imageUrl = extractMetaContent(doc, "og:image");
                if (imageUrl.isEmpty()) imageUrl = extractMetaContent(doc, "twitter:image");

                if (!isValidProductImage(imageUrl)) {
                    imageUrl = "";
                }

                if (imageUrl.isEmpty()) {
                    imageUrl = extractPlatformSpecificImage(doc, platform);
                }

                // Direct ASIN CDN Image Fallback if DOM image is missing or invalid
                if ((imageUrl.isEmpty() || !isValidProductImage(imageUrl)) && asin != null) {
                    imageUrl = getAsinImageUrl(asin);
                }

                imageUrl = cleanImageUrl(imageUrl);

                // 3. Price Extraction
                double price = jsonPrice;
                if (!isPlausiblePrice(price, title)) {
                    price = 0.0;
                }

                if (price <= 0) {
                    price = extractPrice(doc, platform, title);
                }

                if (!isPlausiblePrice(price, title)) {
                    price = 0.0;
                }

                if (price <= 0 && isPlausiblePrice(textPrice, title)) {
                    price = textPrice;
                }

                ProductItem item = new ProductItem();
                item.setTitle(title);
                item.setImageUrl(imageUrl);
                item.setPrice(price);
                item.setSourcePlatform(platform);
                item.setOriginalUrl(finalUrl);

                postSuccess(callback, item);

            } catch (IOException e) {
                Log.e(TAG, "Failed to scrape product from URL: " + finalUrl, e);
                // Fallback using share text, URL slug & ASIN image
                ProductItem fallbackItem = new ProductItem();
                String fallbackTitle = !shareTextTitle.isEmpty() ? shareTextTitle : (!urlTitle.isEmpty() ? urlTitle : platform + " Product");
                String fallbackImage = (asin != null) ? getAsinImageUrl(asin) : "";

                fallbackItem.setTitle(cleanTitle(fallbackTitle));
                fallbackItem.setImageUrl(fallbackImage);
                double fallbackPrice = isPlausiblePrice(textPrice, fallbackTitle) ? textPrice : 0.0;
                fallbackItem.setPrice(fallbackPrice);
                fallbackItem.setSourcePlatform(platform);
                fallbackItem.setOriginalUrl(finalUrl);

                postSuccess(callback, fallbackItem);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during scraping", e);
                postError(callback, "An unexpected error occurred: " + e.getLocalizedMessage());
            }
        });
    }

    private static String extractAsin(String url) {
        if (url == null) return null;
        Pattern asinPattern = Pattern.compile("(?:/dp/|/gp/product/|/d/|/gp/aw/d/)([A-Z0-9]{10})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = asinPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }

    private static String getAsinImageUrl(String asin) {
        if (asin == null || asin.length() != 10) return "";
        return "https://images-na.ssl-images-amazon.com/images/P/" + asin + ".01.MAIN._SCLZZZZZZZ_.jpg";
    }

    /**
     * Universal URL slug parser: extracts the human-readable product name segment from
     * Amazon, Flipkart, Blinkit, BigBasket, Zepto, Myntra, Ajio, JioMart, Croma, etc.
     */
    public static String extractTitleFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) return "";
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isEmpty() || path.equals("/")) return "";

            // If Amazon /dp/ is present
            if (path.contains("/dp/")) {
                String slug = path.substring(0, path.indexOf("/dp/"));
                if (slug.startsWith("/")) slug = slug.substring(1);
                if (!slug.isEmpty() && !slug.equalsIgnoreCase("gp/product")) {
                    String title = formatSlugToTitle(slug);
                    if (title.length() > 3) return title;
                }
            }

            // General path segment inspection
            String[] segments = path.split("/");
            String bestSegment = "";
            int maxWordCount = 0;

            for (String seg : segments) {
                if (seg == null) continue;
                String cleanSeg = seg.trim();
                cleanSeg = cleanSeg.replaceAll("\\.(html|htm|php|aspx)$", "");
                if (cleanSeg.isEmpty()) continue;

                // Ignore technical/structural keywords
                String lower = cleanSeg.toLowerCase();
                if (lower.equals("dp") || lower.equals("gp") || lower.equals("product")
                        || lower.equals("products") || lower.equals("p") || lower.equals("pd")
                        || lower.equals("prn") || lower.equals("prid") || lower.equals("pn")
                        || lower.equals("pvid") || lower.equals("buy") || lower.equals("item")
                        || lower.equals("itm") || lower.equals("s") || lower.equals("c")
                        || lower.equals("catalog") || lower.equals("search") || lower.equals("groceries")
                        || lower.equals("grocery") || lower.equals("electronics") || lower.equals("mobile")
                        || lower.equals("fashion") || lower.equals("all") || lower.equals("details")) {
                    continue;
                }

                // Ignore UUIDs or hex hash segments
                if (cleanSeg.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
                        || cleanSeg.matches("(?i)^[0-9a-f]{16,}$")) {
                    continue;
                }

                // Count distinct alphabetic words (length >= 2)
                String[] words = cleanSeg.split("[-_]+");
                int validWords = 0;
                for (String w : words) {
                    if (w.matches("[a-zA-Z]{2,}")) {
                        validWords++;
                    }
                }

                if (validWords >= 1 && cleanSeg.length() >= 6) {
                    if (validWords > maxWordCount || (validWords == maxWordCount && cleanSeg.length() > bestSegment.length())) {
                        maxWordCount = validWords;
                        bestSegment = cleanSeg;
                    }
                }
            }

            if (!bestSegment.isEmpty()) {
                return formatSlugToTitle(bestSegment);
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String formatSlugToTitle(String slug) {
        if (slug == null) return "";
        String s = slug.replaceAll("(?i)^p-mp\\w+", "")
                .replaceAll("(?i)-p-\\w+$", "")
                .replaceAll("(?i)-prid-\\w+$", "")
                .replaceAll("(?i)-pvid-\\w+$", "")
                .replaceAll("[-_]+", " ")
                .trim();
        return capitalizeWords(s);
    }

    private static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return "";
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            if (w.length() > 1 && Character.isUpperCase(w.charAt(1))) {
                sb.append(w);
            } else {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }

    private static String parseJsonLdTitle(JSONObject obj) {
        if (obj.has("name")) {
            return obj.optString("name", "").trim();
        }
        return "";
    }

    private static String parseJsonLdImage(JSONObject obj) {
        if (obj.has("image")) {
            Object img = obj.opt("image");
            if (img instanceof String && isValidProductImage((String) img)) {
                return cleanImageUrl((String) img);
            }
            if (img instanceof JSONArray) {
                JSONArray arr = (JSONArray) img;
                for (int i = 0; i < arr.length(); i++) {
                    String str = arr.optString(i, "");
                    if (isValidProductImage(str)) {
                        return cleanImageUrl(str);
                    }
                }
            }
            if (img instanceof JSONObject) {
                String str = ((JSONObject) img).optString("url", "");
                if (isValidProductImage(str)) {
                    return cleanImageUrl(str);
                }
            }
        }
        return "";
    }

    private static String extractPlatformSpecificImage(Document doc, String platform) {
        if (doc == null) return "";
        Element imgElem = null;

        if ("Amazon".equalsIgnoreCase(platform)) {
            return extractAmazonImage(doc);
        } else if ("Flipkart".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("img._396cs4, img._2r_T1I, img._53213s, img.DByuf4, div._2r_T1I img");
        } else if ("Blinkit".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("div[class*='ImageCarousel'] img, img[class*='ProductImage'], div[class*='carousel'] img, div[data-test-id='pdp-image'] img");
        } else if ("BigBasket".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("div[class*='Image___StyledDiv'] img, img[class*='ProductImage'], div[class*='carousel'] img, section[class*='ProductImage'] img");
        } else if ("Zepto".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("img[data-testid='pdp-image'], img[class*='product-image'], div[data-testid='pdp-image-container'] img");
        } else if ("Myntra".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("div.image-grid-image, img.image-grid-image, div.image-grid-container img");
        } else if ("Ajio".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("img.rilrtl-lazy-img, div.img-container img");
        } else if ("Nykaa".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("img[class*='product-image'], img[class*='css-111z9ua'], div.css-111z9ua img");
        } else if ("Croma".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("img.product-img, div.pdp-main-image img");
        } else if ("Reliance Digital".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("img.pdp__mainSliderImg, div.pdp__mainSlider img");
        } else if ("Meesho".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("img[class*='ProductImage'], div[class*='ImageContainer'] img");
        } else if ("Tata CLiQ".equalsIgnoreCase(platform)) {
            imgElem = doc.selectFirst("img.ProductDetailsMainCard__image, div.ProductDetailsMainCard__imageContainer img");
        }

        if (imgElem != null) {
            String src = imgElem.attr("src").trim();
            if (src.isEmpty() || src.startsWith("data:")) {
                src = imgElem.attr("data-src").trim();
            }
            if (isValidProductImage(src)) {
                return cleanImageUrl(src);
            }
        }

        // Generic product image selector
        Element genericImg = doc.selectFirst("img[itemprop='image'], meta[property='og:image'], meta[name='twitter:image']");
        if (genericImg != null) {
            String src = genericImg.hasAttr("content") ? genericImg.attr("content").trim() : genericImg.attr("src").trim();
            if (isValidProductImage(src)) {
                return cleanImageUrl(src);
            }
        }

        return "";
    }

    private static String extractAmazonImage(Document doc) {
        Element imgElem = doc.selectFirst("#landingImage, #imgBlkFront, #main-image, #ebooksImgBlkFront, #imgTagWrapperId img, div.imgTagWrapper img, img.a-dynamic-image");
        if (imgElem != null) {
            String hires = imgElem.attr("data-old-hires").trim();
            if (isValidProductImage(hires)) return cleanImageUrl(hires);

            String dynamicImages = imgElem.attr("data-a-dynamic-image").trim();
            if (dynamicImages.startsWith("{")) {
                try {
                    JSONObject json = new JSONObject(dynamicImages);
                    JSONArray names = json.names();
                    if (names != null) {
                        for (int i = 0; i < names.length(); i++) {
                            String imgUrl = names.getString(i);
                            if (isValidProductImage(imgUrl)) {
                                return cleanImageUrl(imgUrl);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            String src = imgElem.attr("src").trim();
            if (isValidProductImage(src)) return cleanImageUrl(src);
        }

        String ogImg = extractMetaContent(doc, "og:image");
        if (isValidProductImage(ogImg)) return cleanImageUrl(ogImg);

        String twImg = extractMetaContent(doc, "twitter:image");
        if (isValidProductImage(twImg)) return cleanImageUrl(twImg);

        Elements itemImgs = doc.select("img[src*=/images/I/], img[data-old-hires*=/images/I/]");
        for (Element img : itemImgs) {
            String src = img.attr("data-old-hires").trim();
            if (src.isEmpty()) src = img.attr("src").trim();
            if (isValidProductImage(src)) {
                return cleanImageUrl(src);
            }
        }

        return "";
    }

    private static boolean isValidProductImage(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.toLowerCase();

        if (lower.contains("/images/g/") || lower.contains("flag") || lower.contains("logo")
                || lower.contains("badge") || lower.contains("sprite") || lower.contains("pixel")
                || lower.contains("icon") || lower.contains("transparent-pixel") || lower.contains("nav")) {
            return false;
        }

        if (lower.contains("amazon")) {
            return lower.contains("/images/i/");
        }

        return true;
    }

    private static String cleanImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) return "";
        url = url.trim();
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        return url;
    }

    private static double parseJsonLdPrice(JSONObject obj) {
        if (obj == null) return 0.0;
        String type = obj.optString("@type", "");
        if (!type.isEmpty() && !type.equalsIgnoreCase("Product") && !type.contains("Product")) {
            return 0.0;
        }

        if (obj.has("offers")) {
            Object offersObj = obj.opt("offers");
            if (offersObj instanceof JSONObject) {
                JSONObject offers = (JSONObject) offersObj;
                double price = parseCurrencyDouble(offers.optString("price", "0"));
                double highPrice = parseCurrencyDouble(offers.optString("highPrice", "0"));
                double lowPrice = parseCurrencyDouble(offers.optString("lowPrice", "0"));

                // If highPrice is a substantial product price and lowPrice is a tiny accessory/EMI (e.g. 500 vs 99990)
                if (highPrice > 0 && lowPrice > 0 && highPrice > lowPrice * 5 && lowPrice < 2000) {
                    return highPrice;
                }
                if (price > 0) return price;
                if (lowPrice > 0) return lowPrice;
                if (highPrice > 0) return highPrice;
            } else if (offersObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) offersObj;
                double maxPrice = 0.0;
                double primaryPrice = 0.0;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject offer = arr.optJSONObject(i);
                    if (offer != null) {
                        double p = parseCurrencyDouble(offer.optString("price", "0"));
                        if (p > maxPrice) maxPrice = p;
                        if (p >= 2000.0 && primaryPrice == 0.0) primaryPrice = p;
                    }
                }
                return primaryPrice > 0 ? primaryPrice : maxPrice;
            }
        }
        return 0.0;
    }

    private static String extractMetaContent(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=" + property + "]");
        if (meta == null) {
            meta = doc.selectFirst("meta[name=" + property + "]");
        }
        return meta != null ? meta.attr("content").trim() : "";
    }

    /**
     * Extracts the real product purchase price, rigorously avoiding coupon traps (e.g. ₹500 off),
     * bank discount offers, monthly EMI figures, warranty add-ons, and header banners.
     */
    private static double extractPrice(Document doc, String platform, String title) {
        if (doc == null) return 0.0;

        // 1. Meta tag prices (og:price:amount, product:price:amount)
        String metaPrice = extractMetaContent(doc, "og:price:amount");
        if (metaPrice.isEmpty()) metaPrice = extractMetaContent(doc, "product:price:amount");
        if (!metaPrice.isEmpty()) {
            double parsed = parseCurrencyDouble(metaPrice);
            if (isPlausiblePrice(parsed, title)) return parsed;
        }

        // Twitter data card price (used by Myntra, Nykaa, etc.)
        String twData1 = extractMetaContent(doc, "twitter:data1");
        if (!twData1.isEmpty()) {
            double parsed = parseCurrencyDouble(twData1);
            if (isPlausiblePrice(parsed, title)) return parsed;
        }

        // 2. High-precision Platform-specific DOM Buybox Selectors
        double domPrice = extractPlatformSpecificPrice(doc, platform, title);
        if (isPlausiblePrice(domPrice, title)) {
            return domPrice;
        }

        // 3. Embedded script JSON state search (Next.js __NEXT_DATA__, Flipkart initial state, etc.)
        double scriptPrice = extractScriptPrice(doc, title);
        if (isPlausiblePrice(scriptPrice, title)) {
            return scriptPrice;
        }

        // 4. Clean Document Body: Strip header, nav, footer, coupons, EMI, warranty & promo banners
        Document cleanDoc = doc.clone();
        cleanDoc.select("header, nav, footer, div#coupons, div[id*='coupon'], div[class*='coupon'], div.inEmilOffers, div#inEmilOffers, div#attach-warranty-pane, div#sopp_feature_div, div#applicable_promotion_list_sec, div#promotions_feature_div, div[class*='bank-offer'], div[class*='offer-box'], div[class*='emi']").remove();

        // Check for price in the cleaned DOM
        Elements priceCandidates = cleanDoc.select("span[class*='price'], div[class*='price'], *[data-price], *[data-selling-price]");
        double bestCandidate = 0.0;
        for (Element el : priceCandidates) {
            String txt = el.hasAttr("data-price") ? el.attr("data-price") : el.text();
            if (txt.contains("₹") || txt.toLowerCase().contains("rs") || el.hasAttr("data-price")) {
                double parsed = parseCurrencyDouble(txt);
                if (isPlausiblePrice(parsed, title) && parsed > bestCandidate) {
                    bestCandidate = parsed;
                }
            }
        }
        if (bestCandidate > 0) return bestCandidate;

        // 5. Final fallback text regex on cleaned body (select highest plausible candidate to avoid small coupon values)
        String fullText = cleanDoc.text();
        Pattern pricePattern = Pattern.compile("(?:₹|Rs\\.?)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{2})?|[0-9]+(?:\\.[0-9]{2})?)");
        Matcher matcher = pricePattern.matcher(fullText);
        while (matcher.find()) {
            double parsed = parseCurrencyDouble(matcher.group(1));
            if (isPlausiblePrice(parsed, title) && parsed > bestCandidate) {
                bestCandidate = parsed;
            }
        }

        return bestCandidate;
    }

    private static double extractPlatformSpecificPrice(Document doc, String platform, String title) {
        if ("Amazon".equalsIgnoreCase(platform)) {
            // Priority order of official Amazon buybox pricing elements (never loose span.a-price)
            String[] selectors = {
                    "#corePriceDisplay_desktop_feature_div span.priceToPay span.a-offscreen",
                    "#corePrice_feature_div span.priceToPay span.a-offscreen",
                    "#corePriceDisplay_desktop_feature_div span.a-price[data-a-color='price'] span.a-offscreen",
                    "#corePrice_feature_div span.a-price[data-a-color='price'] span.a-offscreen",
                    "#apex_desktop .apexPriceToPay span.a-offscreen",
                    "#apex_mobile .apexPriceToPay span.a-offscreen",
                    "#corePriceDisplay_desktop_feature_div span.a-price-whole",
                    "#corePrice_feature_div span.a-price-whole",
                    "#priceInsideBuyBox_feature_div span.a-price span.a-offscreen",
                    "#priceblock_ourprice",
                    "#priceblock_dealprice",
                    "#priceblock_saleprice"
            };
            for (String sel : selectors) {
                Element el = doc.selectFirst(sel);
                if (el != null) {
                    double val = parseCurrencyDouble(el.text());
                    if (isPlausiblePrice(val, title)) return val;
                }
            }
        } else if ("Flipkart".equalsIgnoreCase(platform)) {
            // Target Flipkart main hero product price container (CxhGGd font size class)
            String[] selectors = {
                    "div.hl05eU div.Nx9bqj.CxhGGd",
                    "div._25b18c div.Nx9bqj.CxhGGd",
                    "div.Nx9bqj.CxhGGd",
                    "div._25b18c div._30jeq3._16J92d",
                    "div._30jeq3._16J92d",
                    "div._25b18c div.Nx9bqj",
                    "div._25b18c div._30jeq3"
            };
            for (String sel : selectors) {
                Element el = doc.selectFirst(sel);
                if (el != null) {
                    double val = parseCurrencyDouble(el.text());
                    if (isPlausiblePrice(val, title)) return val;
                }
            }
        } else if ("Blinkit".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("div[class*='ProductPrice'], div[class*='PriceContainer'] span, div[data-test-id='pdp-price'], div[class*='SellingPrice']");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("BigBasket".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("span[class*='Price___StyledSpan'], span[class*='DiscountedPrice'], td[class*='price']");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Zepto".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("h4[data-testid='pdp-selling-price'], span[data-testid='pdp-selling-price']");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Swiggy Instamart".equalsIgnoreCase(platform) || "Swiggy".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("div[class*='item-price'], span[class*='item-price'], div[class*='Price']");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("JioMart".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("div.sp-price span, div.price-box span.sp, span#price, span.jm-heading-xxs");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Myntra".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("span.pdp-price, strong.pdp-price, div.pdp-price-info strong");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Ajio".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("div.prod-sp, span.prod-price");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Nykaa".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("span[class*='post-card__price'], span[class*='css-111z9ua'], span.css-1jczs19");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Tata CLiQ".equalsIgnoreCase(platform) || "Tata Neu".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("h3.ProductDetailsMainCard__price, span.ProductDetailsMainCard__price");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Croma".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("span.amount[data-testid='new-price'], span.amount, span.new-price");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Reliance Digital".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("span.gimCqV, span.pdp__offerPrice, li.pdp__priceSection__priceListText span");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Vijay Sales".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("span.vsp-price, span.sp-price");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Meesho".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("h4[class*='ProductPrice'], span[class*='ProductPrice']");
            if (el != null) return parseCurrencyDouble(el.text());
        } else if ("Snapdeal".equalsIgnoreCase(platform)) {
            Element el = doc.selectFirst("span.payBlkBig, span.pdp-final-price");
            if (el != null) return parseCurrencyDouble(el.text());
        }
        return 0.0;
    }

    private static double extractScriptPrice(Document doc, String title) {
        if (doc == null) return 0.0;
        Elements scripts = doc.select("script");
        for (Element s : scripts) {
            String scriptHtml = s.html();
            if (scriptHtml.contains("price") || scriptHtml.contains("Price") || scriptHtml.contains("sp")) {
                // Priority 1: High-confidence selling price JSON keys
                Pattern p1 = Pattern.compile("\"(?:priceToPay|specialPrice|finalPrice|discountedPrice|sellingPrice|selling_price|offerPrice)\":\\s*\\{\\s*\"value\":\\s*([0-9]+)", Pattern.CASE_INSENSITIVE);
                Matcher m1 = p1.matcher(scriptHtml);
                if (m1.find()) {
                    double parsed = parseCurrencyDouble(m1.group(1));
                    if (isPlausiblePrice(parsed, title)) return parsed;
                }

                // BigBasket Next.js JSON: "sp": 140.00 or "sp":"140.00"
                Pattern pBb = Pattern.compile("\"sp\":\\s*\"?([0-9,]+(?:\\.[0-9]{2})?)\"?", Pattern.CASE_INSENSITIVE);
                Matcher mBb = pBb.matcher(scriptHtml);
                if (mBb.find()) {
                    double parsed = parseCurrencyDouble(mBb.group(1));
                    if (isPlausiblePrice(parsed, title)) return parsed;
                }

                // Specific key value pairs (excluding loose 'price' to prevent matching discount metadata)
                Pattern p2 = Pattern.compile("\"(?:priceToPay|specialPrice|finalPrice|discountedPrice|sellingPrice|selling_price|offerPrice|landingPrice|dealPrice|priceAmount|buyingPrice)\":\\s*\"?([0-9,]+(?:\\.[0-9]{2})?)\"?", Pattern.CASE_INSENSITIVE);
                Matcher m2 = p2.matcher(scriptHtml);
                if (m2.find()) {
                    double parsed = parseCurrencyDouble(m2.group(1));
                    if (isPlausiblePrice(parsed, title)) return parsed;
                }
            }
        }
        return 0.0;
    }

    /**
     * Validates whether a candidate price makes logical sense for the product category.
     * Prevents coupons, bank discounts, delivery charges, or EMI amounts (e.g. ₹500)
     * from masquerading as product prices for expensive items like laptops, MacBooks, or phones.
     */
    public static boolean isPlausiblePrice(double price, String title) {
        if (price <= 0.0) return false;
        if (title == null || title.trim().isEmpty()) return true;

        String lowerTitle = title.toLowerCase();

        boolean isHighValueItem = lowerTitle.contains("laptop")
                || lowerTitle.contains("macbook")
                || lowerTitle.contains("notebook")
                || lowerTitle.contains("thinkpad")
                || lowerTitle.contains("ideapad")
                || lowerTitle.contains("vivobook")
                || lowerTitle.contains("zenbook")
                || lowerTitle.contains("pavilion")
                || lowerTitle.contains("inspiron")
                || lowerTitle.contains("vostro")
                || lowerTitle.contains("legion")
                || lowerTitle.contains("predator")
                || lowerTitle.contains("omen")
                || lowerTitle.contains("victus")
                || lowerTitle.contains("galaxy book")
                || lowerTitle.contains("surface pro")
                || lowerTitle.contains("iphone")
                || lowerTitle.contains("television")
                || lowerTitle.contains("smart tv")
                || lowerTitle.contains("oled tv")
                || lowerTitle.contains("refrigerator")
                || lowerTitle.contains("washing machine")
                || lowerTitle.contains("air conditioner");

        if (isHighValueItem) {
            // A laptop, TV, or high-value appliance NEVER costs <= ₹2,000.
            // Any price like ₹500 is a coupon, EMI, warranty, or delivery fee!
            if (price < 2000.0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extracts product price from raw text (e.g. share strings), strictly ignoring
     * promotional discounts, coupon codes, and cashback mentions like '₹500 off' or 'Save ₹500'.
     */
    public static double extractPriceFromText(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;

        // Remove discount/coupon/cashback snippets first so they don't pollute price extraction
        // E.g. 'Save ₹500', 'Flat ₹500 off', '₹500 discount', '₹500 cashback', '₹500 coupon'
        String cleanedText = text.replaceAll("(?i)(?:flat|extra|save|upto|up to|get|instant)?\\s*(?:₹|rs\\.?|inr)\\s*[0-9,]+(?:\\.[0-9]{1,2})?\\s*(?:off|discount|cashback|coupon|voucher|savings|instant discount|cash back)", " ");
        cleanedText = cleanedText.replaceAll("(?i)(?:save|discount of|cashback of|coupon of|off)\\s*(?:₹|rs\\.?|inr)\\s*[0-9,]+(?:\\.[0-9]{1,2})?", " ");

        // Priority 1: Price preceded by indicator like "price:", "at ₹", "for ₹", "deal price:"
        Pattern pIndicator = Pattern.compile("(?i)(?:deal price|special price|selling price|price|final price|now at|buy at|at|for)\\s*[:\\-]?\\s*(?:₹|rs\\.?|inr)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{2})?|[0-9]+(?:\\.[0-9]{2})?)");
        Matcher mIndicator = pIndicator.matcher(cleanedText);
        if (mIndicator.find()) {
            double val = parseCurrencyDouble(mIndicator.group(1));
            if (val > 0) return val;
        }

        // Priority 2: General standalone currency regex from cleaned text (take highest plausible number)
        Pattern p = Pattern.compile("(?:₹|Rs\\.?|INR|\\$)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{2})?|[0-9]+(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(cleanedText);
        double maxCandidate = 0.0;
        while (m.find()) {
            double val = parseCurrencyDouble(m.group(1));
            if (val > maxCandidate) {
                maxCandidate = val;
            }
        }
        return maxCandidate;
    }

    private static double parseCurrencyDouble(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        try {
            String s = text.replaceAll("(?i)rs\\.?|inr|\\$|€|£", "").trim();
            s = s.replace(",", "");
            Pattern p = Pattern.compile("\\d+(?:\\.\\d+)?");
            Matcher m = p.matcher(s);
            if (m.find()) {
                return Double.parseDouble(m.group());
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String cleanTitle(String title) {
        if (title == null) return "";
        return title
                // Amazon
                .replaceAll("(?i)\\s*:\\s*Amazon\\.[a-z.]+", "")
                .replaceAll("(?i)\\s*-\\s*Amazon\\.[a-z.]+", "")
                // Flipkart
                .replaceAll("(?i)\\s*-\\s*Flipkart\\.com", "")
                .replaceAll("(?i)\\s*:\\s*Flipkart\\.com", "")
                .replaceAll("(?i)\\s*\\|\\s*Flipkart\\.com", "")
                // Blinkit / Grofers
                .replaceAll("(?i)\\s*\\|\\s*Blinkit.*", "")
                .replaceAll("(?i)\\s*-\\s*Blinkit.*", "")
                // BigBasket
                .replaceAll("(?i)\\s*-\\s*bigbasket.*", "")
                .replaceAll("(?i)\\s*\\|\\s*bigbasket.*", "")
                .replaceAll("(?i)\\s*-\\s*Buy online.*", "")
                .replaceAll("(?i)Buy\\s+(.*?)\\s+Online at Best Price.*", "$1")
                // Zepto
                .replaceAll("(?i)\\s*\\|\\s*Zepto.*", "")
                .replaceAll("(?i)\\s*-\\s*Zepto.*", "")
                // Myntra
                .replaceAll("(?i)\\s*-\\s*Buy .*? Online at Best Price in India \\| Myntra.*", "")
                .replaceAll("(?i)\\s*\\|\\s*Myntra.*", "")
                // Ajio
                .replaceAll("(?i)\\s*\\|\\s*AJIO.*", "")
                .replaceAll("(?i)\\s*-\\s*AJIO.*", "")
                // Nykaa
                .replaceAll("(?i)\\s*\\|\\s*Nykaa.*", "")
                .replaceAll("(?i)\\s*-\\s*Nykaa.*", "")
                // Tata CLiQ
                .replaceAll("(?i)\\s*\\|\\s*Tata CLiQ.*", "")
                .replaceAll("(?i)\\s*\\|\\s*Tata Neu.*", "")
                // Croma
                .replaceAll("(?i)\\s*\\|\\s*Croma.*", "")
                .replaceAll("(?i)\\s*-\\s*Croma.*", "")
                // Reliance Digital
                .replaceAll("(?i)\\s*\\|\\s*Reliance Digital.*", "")
                // Generic noise
                .replaceAll("(?i)\\s*\\|\\s*Online Shopping in India.*", "")
                .replaceAll("(?i)\\s*:\\s*Buy online at lowest prices.*", "")
                .replaceAll("(?i)\\s*\\|\\s*Latest Price.*", "")
                .trim();
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
