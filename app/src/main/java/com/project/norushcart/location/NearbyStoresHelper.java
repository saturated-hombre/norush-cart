package com.project.norushcart.location;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.google.android.libraries.places.api.net.SearchByTextResponse;
import com.miniproject.myapplication.BuildConfig;
import com.project.norushcart.data.NearbyStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NearbyStoresHelper {

    private static final String TAG = "NearbyStoresHelper";
    private static final String PREFS_NAME = "norush_prefs";
    private static final String KEY_FIND_NEARBY_STORES = "find_nearby_stores_enabled";
    private static final String KEY_NEARBY_DISCLAIMER_SEEN = "nearby_disclaimer_seen";

    public interface StoreSearchCallback {
        void onSuccess(List<NearbyStore> stores, String queryUsed);
        void onError(String message, boolean fallbackToIntent);
    }

    public static boolean isFindNearbyStoresEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FIND_NEARBY_STORES, false); // Default OFF
    }

    public static void setFindNearbyStoresEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_FIND_NEARBY_STORES, enabled).apply();
    }

    public static boolean hasSeenNearbyStoresDisclaimer(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NEARBY_DISCLAIMER_SEEN, false);
    }

    public static void setSeenNearbyStoresDisclaimer(Context context, boolean seen) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_NEARBY_DISCLAIMER_SEEN, seen).apply();
    }

    /**
     * Determines whether a valid Places API key is configured.
     */
    public static boolean hasValidApiKey() {
        String key = BuildConfig.MAPS_API_KEY;
        return key != null && !key.trim().isEmpty()
                && !key.toUpperCase().contains("YOUR_")
                && !key.equalsIgnoreCase("YOUR_MAPS_API_KEY_HERE")
                && !key.toLowerCase().contains("youractualkeyhere");
    }

    /**
     * Extracts search query: "<brand> store" if brand recognized, else "<category> store", else "store".
     */
    public static String detectStoreQuery(String productTitle) {
        if (productTitle == null || productTitle.trim().isEmpty()) {
            return "store";
        }

        String lower = productTitle.toLowerCase(Locale.ROOT);

        // Recognizable consumer brands
        String[] brands = {
                "apple", "samsung", "sony", "nike", "adidas", "puma", "bose", "jbl",
                "dell", "hp", "lenovo", "asus", "acer", "zara", "h&m", "uniqlo",
                "levi's", "levis", "boat", "noise", "oneplus", "xiaomi", "google",
                "fossil", "casio", "titan", "philips", "canon", "nikon", "under armour",
                "reebok", "skechers", "logitech", "anker", "sennheiser", "rolex", "seiko"
        };

        for (String brand : brands) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(brand) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(lower);
            if (matcher.find()) {
                // Capitalize brand nicely
                String capitalized = brand.substring(0, 1).toUpperCase(Locale.ROOT) + brand.substring(1);
                return capitalized + " store";
            }
        }

        // Category keyword mapping
        if (matchesAny(lower, "headphone", "headphones", "earphone", "earphones", "earbuds", "speaker", "speakers",
                "laptop", "computer", "monitor", "mouse", "keyboard", "mobile", "phone", "smartphone", "tablet", "tv", "television")) {
            return "electronics store";
        } else if (matchesAny(lower, "sneaker", "sneakers", "shoe", "shoes", "boot", "boots", "sandal", "sandals", "footwear")) {
            return "footwear store";
        } else if (matchesAny(lower, "shirt", "t-shirt", "tshirt", "jeans", "pants", "dress", "jacket", "hoodie", "apparel", "clothing")) {
            return "clothing store";
        } else if (matchesAny(lower, "watch", "smartwatch", "timepiece")) {
            return "watch store";
        } else if (matchesAny(lower, "perfume", "cologne", "fragrance", "makeup", "lipstick", "cosmetic", "cosmetics", "skincare")) {
            return "cosmetics store";
        } else if (matchesAny(lower, "book", "novel", "hardcover", "paperback")) {
            return "book store";
        } else if (matchesAny(lower, "coffee", "tea", "snack", "grocery", "chocolates")) {
            return "grocery store";
        }

        return "store";
    }

    private static boolean matchesAny(String input, String... keywords) {
        for (String kw : keywords) {
            if (input.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs nearby store search via Google Places SDK for Android (New).
     * If API key is missing or invalid, immediately signals fallback.
     */
    public static void searchNearbyStores(@NonNull Context context,
                                          int itemId,
                                          @NonNull String query,
                                          @NonNull LatLng currentLatLng,
                                          @NonNull StoreSearchCallback callback) {
        if (!hasValidApiKey()) {
            Log.w(TAG, "Places API key missing or unconfigured. Triggering fallback.");
            callback.onError("No Places API key configured. Opening Google Maps directly.", true);
            return;
        }

        try {
            if (!Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(context.getApplicationContext(), BuildConfig.MAPS_API_KEY);
            }

            PlacesClient placesClient = Places.createClient(context);

            List<Place.Field> placeFields = Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS,
                    Place.Field.PHONE_NUMBER,
                    Place.Field.CURRENT_OPENING_HOURS,
                    Place.Field.RATING
            );

            // 5 km circular radius bias
            CircularBounds circularBounds = CircularBounds.newInstance(currentLatLng, 5000.0);

            SearchByTextRequest request = SearchByTextRequest.builder(query, placeFields)
                    .setLocationBias(circularBounds)
                    .setMaxResultCount(10)
                    .build();

            placesClient.searchByText(request)
                    .addOnSuccessListener((SearchByTextResponse response) -> {
                        List<NearbyStore> resultList = new ArrayList<>();
                        for (Place place : response.getPlaces()) {
                            if (place.getLatLng() == null) continue;

                            Boolean openNow = null;
                            if (place.getCurrentOpeningHours() != null) {
                                openNow = place.getCurrentOpeningHours().getPeriods() != null && !place.getCurrentOpeningHours().getPeriods().isEmpty();
                            }

                            NearbyStore store = new NearbyStore(
                                    place.getId(),
                                    place.getName(),
                                    place.getAddress(),
                                    place.getLatLng().latitude,
                                    place.getLatLng().longitude,
                                    place.getPhoneNumber(),
                                    openNow,
                                    place.getRating(),
                                    itemId
                            );

                            // Calculate distance from user's current location
                            float[] dist = new float[1];
                            Location.distanceBetween(
                                    currentLatLng.latitude, currentLatLng.longitude,
                                    place.getLatLng().latitude, place.getLatLng().longitude,
                                    dist
                            );
                            store.setDistanceMeters(dist[0]);

                            resultList.add(store);
                        }

                        if (resultList.isEmpty()) {
                            callback.onError("No nearby stores found within 5 km for \"" + query + "\".", true);
                        } else {
                            callback.onSuccess(resultList, query);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Places searchByText failed: " + e.getMessage(), e);
                        callback.onError("Unable to load stores from Places API (" + e.getMessage() + "). Opening Google Maps...", true);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Exception initializing or searching Places: " + e.getMessage(), e);
            callback.onError("Places search failed: " + e.getMessage(), true);
        }
    }

    /**
     * Fallback intent: opens Google Maps with "geo:0,0?q=<query>".
     */
    public static void openGoogleMapsFallback(@NonNull Context context, @NonNull String query) {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(mapIntent);
        } else {
            // General browser fallback if Maps app is not installed
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)));
            context.startActivity(browserIntent);
        }
    }
}
