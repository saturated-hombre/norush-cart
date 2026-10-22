package com.project.norushcart.location;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

public class LocationHelper {

    public static final String PREFS_NAME = "norush_settings";
    public static final String KEY_TRACK_LOCATION = "track_location";
    public static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private static final long TIMEOUT_MS = 2000L;

    public interface LocationResultCallback {
        void onLocationResult(@Nullable Double latitude, @Nullable Double longitude, @Nullable Float accuracy);
    }

    public static boolean isLocationTrackingEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_TRACK_LOCATION, true);
    }

    public static void setLocationTrackingEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_TRACK_LOCATION, enabled).apply();
    }

    public static boolean isOnboardingCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public static void setOnboardingCompleted(Context context, boolean completed) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void fetchCurrentLocation(Context context, LocationResultCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (!isLocationTrackingEnabled(context) || !hasLocationPermission(context)) {
            mainHandler.post(() -> callback.onLocationResult(null, null, null));
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        CancellationTokenSource cts = new CancellationTokenSource();
        boolean[] completed = new boolean[]{false};

        // Enforce 2000ms timeout
        Runnable timeoutRunnable = () -> {
            synchronized (completed) {
                if (completed[0]) return;
                completed[0] = true;
            }
            cts.cancel();
            fallbackToLastLocation(client, mainHandler, callback);
        };
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                    .addOnSuccessListener(location -> {
                        mainHandler.removeCallbacks(timeoutRunnable);
                        synchronized (completed) {
                            if (completed[0]) return;
                            completed[0] = true;
                        }
                        if (location != null) {
                            mainHandler.post(() -> callback.onLocationResult(location.getLatitude(), location.getLongitude(), location.getAccuracy()));
                        } else {
                            fallbackToLastLocation(client, mainHandler, callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        mainHandler.removeCallbacks(timeoutRunnable);
                        synchronized (completed) {
                            if (completed[0]) return;
                            completed[0] = true;
                        }
                        fallbackToLastLocation(client, mainHandler, callback);
                    });
        } catch (SecurityException se) {
            mainHandler.removeCallbacks(timeoutRunnable);
            synchronized (completed) {
                if (completed[0]) return;
                completed[0] = true;
            }
            mainHandler.post(() -> callback.onLocationResult(null, null, null));
        }
    }

    private static void fallbackToLastLocation(FusedLocationProviderClient client, Handler mainHandler, LocationResultCallback callback) {
        try {
            client.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            mainHandler.post(() -> callback.onLocationResult(location.getLatitude(), location.getLongitude(), location.getAccuracy()));
                        } else {
                            mainHandler.post(() -> callback.onLocationResult(null, null, null));
                        }
                    })
                    .addOnFailureListener(e -> mainHandler.post(() -> callback.onLocationResult(null, null, null)));
        } catch (SecurityException se) {
            mainHandler.post(() -> callback.onLocationResult(null, null, null));
        }
    }

    public interface FreshLocationCallback {
        void onLocation(@Nullable Double latitude, @Nullable Double longitude);
    }

    public static void getFreshLocationForStoreSearch(Context context, FreshLocationCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        if (!hasLocationPermission(context)) {
            mainHandler.post(() -> callback.onLocation(null, null));
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        CancellationTokenSource cts = new CancellationTokenSource();
        boolean[] completed = new boolean[]{false};

        Runnable timeoutRunnable = () -> {
            synchronized (completed) {
                if (completed[0]) return;
                completed[0] = true;
            }
            cts.cancel();
            try {
                client.getLastLocation().addOnSuccessListener(loc -> {
                    if (loc != null) {
                        mainHandler.post(() -> callback.onLocation(loc.getLatitude(), loc.getLongitude()));
                    } else {
                        mainHandler.post(() -> callback.onLocation(null, null));
                    }
                }).addOnFailureListener(e -> mainHandler.post(() -> callback.onLocation(null, null)));
            } catch (SecurityException e) {
                mainHandler.post(() -> callback.onLocation(null, null));
            }
        };
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                    .addOnSuccessListener(loc -> {
                        synchronized (completed) {
                            if (completed[0]) return;
                            completed[0] = true;
                        }
                        mainHandler.removeCallbacks(timeoutRunnable);
                        if (loc != null) {
                            mainHandler.post(() -> callback.onLocation(loc.getLatitude(), loc.getLongitude()));
                        } else {
                            timeoutRunnable.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        synchronized (completed) {
                            if (completed[0]) return;
                            completed[0] = true;
                        }
                        mainHandler.removeCallbacks(timeoutRunnable);
                        timeoutRunnable.run();
                    });
        } catch (SecurityException e) {
            mainHandler.removeCallbacks(timeoutRunnable);
            mainHandler.post(() -> callback.onLocation(null, null));
        }
    }
}
