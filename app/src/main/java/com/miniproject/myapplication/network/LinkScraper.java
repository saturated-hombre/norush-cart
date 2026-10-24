package com.miniproject.myapplication.network;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkScraper {

    private static final String TAG = "LinkScraper";
    private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

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

        String finalUrl = normalizeAmazonUrl(resolveRedirects(url));
        String platform = detectPlatform(finalUrl);
        String asin = extractAsin(finalUrl);
        String urlTitle = extractTitleFromUrl(finalUrl);

        ProductItem item = new ProductItem();
        item.setOriginalUrl(finalUrl);
        item.setSourcePlatform(platform);

        try {
            Connection connection = Jsoup.connect(finalUrl)
                    .userAgent(DESKTOP_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .referrer("https://www.google.com")
                    .timeout(12000)
                    .followRedirects(true);

            Document doc = connection.get();

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

            // 1. Extract Title
            String title = jsonTitle;
            if (title.isEmpty()) title = extractMetaContent(doc, "og:title");
            if (title.isEmpty()) title = extractMetaContent(doc, "twitter:title");

            if (title.isEmpty() && platform.equalsIgnoreCase("Amazon")) {
                Element titleElem = doc.selectFirst("#productTitle, h1#title, #title span");
                if (titleElem != null) title = titleElem.text().trim();
            }

            if (title.isEmpty() && platform.equalsIgnoreCase("Flipkart")) {
                Element titleElem = doc.selectFirst("span.B_NuT6, h1._6ER2B8, span.VU-m1w");
                if (titleElem != null) title = titleElem.text().trim();
            }

            if (title.isEmpty() || title.equalsIgnoreCase("Amazon Product") || title.toLowerCase().contains("robot check") || title.toLowerCase().contains("captcha")) {
                if (!urlTitle.isEmpty()) {
                    title = urlTitle;
                } else {
                    title = doc.title().trim();
                }
            }

            if (title.isEmpty()) {
                title = platform + " Product";
            }

            item.setTitle(cleanTitle(title));

            // 2. Extract Image URL
            String imageUrl = jsonImage;
            if (imageUrl.isEmpty()) imageUrl = extractMetaContent(doc, "og:image");
            if (imageUrl.isEmpty()) imageUrl = extractMetaContent(doc, "twitter:image");

            if (!isValidProductImage(imageUrl)) {
                imageUrl = "";
            }

            if (imageUrl.isEmpty() && platform.equalsIgnoreCase("Amazon")) {
                imageUrl = extractAmazonImage(doc);
            } else if (imageUrl.isEmpty() && platform.equalsIgnoreCase("Flipkart")) {
                Element imgElem = doc.selectFirst("img._396cs4, img._2r_T1I, img._53213s");
                if (imgElem != null) {
                    imageUrl = cleanImageUrl(imgElem.attr("src"));
                }
            }

            // Direct ASIN CDN Image Fallback if DOM image is missing or invalid
            if ((imageUrl.isEmpty() || !isValidProductImage(imageUrl)) && asin != null) {
                imageUrl = getAsinImageUrl(asin);
            }

            imageUrl = cleanImageUrl(imageUrl);
            item.setImageUrl(imageUrl);

            // 3. Extract Price
            double price = jsonPrice;
            if (price <= 0) {
                price = extractPrice(doc, platform);
            }
            item.setPrice(price);

        } catch (IOException e) {
            Log.e(TAG, "Failed to scrape URL: " + finalUrl, e);
            String fallbackTitle = !urlTitle.isEmpty() ? urlTitle : platform + " Shared Item";
            String fallbackImage = (asin != null) ? getAsinImageUrl(asin) : "";

            item.setTitle(cleanTitle(fallbackTitle));
            item.setImageUrl(fallbackImage);
            if (item.getPrice() <= 0) {
                item.setPrice(999.0);
            }
        }

        return item;
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

    private static String extractTitleFromUrl(String url) {
        if (url == null) return "";
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path != null && path.contains("/dp/")) {
                String slug = path.substring(0, path.indexOf("/dp/"));
                if (slug.startsWith("/")) slug = slug.substring(1);
                if (!slug.isEmpty() && !slug.equalsIgnoreCase("gp/product")) {
                    String title = slug.replace("-", " ").replace("_", " ").trim();
                    if (title.length() > 3) {
                        return title;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
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

    private static String cleanImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return "";
        String url = rawUrl.trim();
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        return url;
    }

    private static double parseJsonLdPrice(JSONObject obj) {
        if (obj.has("offers")) {
            Object offersObj = obj.opt("offers");
            if (offersObj instanceof JSONObject) {
                JSONObject offers = (JSONObject) offersObj;
                if (offers.has("price")) {
                    return parseCurrencyDouble(offers.optString("price", "0"));
                } else if (offers.has("lowPrice")) {
                    return parseCurrencyDouble(offers.optString("lowPrice", "0"));
                }
            } else if (offersObj instanceof JSONArray && ((JSONArray) offersObj).length() > 0) {
                JSONObject firstOffer = ((JSONArray) offersObj).optJSONObject(0);
                if (firstOffer != null && firstOffer.has("price")) {
                    return parseCurrencyDouble(firstOffer.optString("price", "0"));
                }
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

    private static double extractPrice(Document doc, String platform) {
        String metaPrice = extractMetaContent(doc, "og:price:amount");
        if (metaPrice.isEmpty()) metaPrice = extractMetaContent(doc, "product:price:amount");
        if (!metaPrice.isEmpty()) {
            double parsed = parseCurrencyDouble(metaPrice);
            if (parsed > 0) return parsed;
        }

        if (platform.equalsIgnoreCase("Amazon")) {
            Element priceElem = doc.selectFirst(".apexPriceToPay span.a-offscreen, #corePrice_feature_div span.a-offscreen, #corePriceDisplay_desktop_feature_div span.a-offscreen, span.a-price span.a-offscreen, #priceblock_ourprice, #priceblock_dealprice, span.a-price-whole");
            if (priceElem != null) {
                double parsed = parseCurrencyDouble(priceElem.text());
                if (parsed > 0) return parsed;
            }
        }

        if (platform.equalsIgnoreCase("Flipkart")) {
            Element priceElem = doc.selectFirst("div._30jeq3, div._16J92d, div.Nx9bqj");
            if (priceElem != null) {
                double parsed = parseCurrencyDouble(priceElem.text());
                if (parsed > 0) return parsed;
            }
        }

        String fullText = doc.text();
        Pattern pricePattern = Pattern.compile("(?:₹|Rs\\.?|\\$)\\s*([0-9,]+(?:\\.[0-9]{2})?)");
        Matcher matcher = pricePattern.matcher(fullText);
        if (matcher.find()) {
            double parsed = parseCurrencyDouble(matcher.group(1));
            if (parsed > 0) return parsed;
        }

        return 499.00;
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
        return title.replaceAll("(?i)\\s*:\\s*Amazon\\.[a-z.]+|\\s*-\\s*Flipkart\\.com|\\s*-\\s*Buy online.*", "").trim();
    }
}
