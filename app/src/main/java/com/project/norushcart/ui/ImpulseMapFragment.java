package com.project.norushcart.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.miniproject.myapplication.R;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.NearbyStore;
import com.project.norushcart.data.ProductItem;
import com.project.norushcart.location.LocationHelper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ImpulseMapFragment extends Fragment implements OnMapReadyCallback {

    public enum FilterMode {
        ALL, NIGHT, DAY, THIS_WEEK, STORES
    }

    private GoogleMap googleMap;
    private ClusterManager<ImpulseClusterItem> clusterManager;
    private List<ProductItem> allGeotaggedItems = new ArrayList<>();
    private List<NearbyStore> nearbyStores = new ArrayList<>();
    private List<Marker> storeMarkers = new ArrayList<>();
    private String currentStoreQuery = "";
    private FilterMode currentFilter = FilterMode.ALL;

    private ChipGroup chipGroupFilters;
    private View cardMapSummary;
    private TextView tvMapSummaryCount;
    private TextView tvMapSummaryInsight;

    private View cardMapEmptyState;
    private TextView tvMapEmptyTitle;
    private TextView tvMapEmptyDesc;
    private MaterialButton btnMapEmptyAction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_impulse_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardMapSummary = view.findViewById(R.id.cardMapSummary);
        tvMapSummaryCount = view.findViewById(R.id.tvMapSummaryCount);
        tvMapSummaryInsight = view.findViewById(R.id.tvMapSummaryInsight);

        cardMapEmptyState = view.findViewById(R.id.cardMapEmptyState);
        tvMapEmptyTitle = view.findViewById(R.id.tvMapEmptyTitle);
        tvMapEmptyDesc = view.findViewById(R.id.tvMapEmptyDesc);
        btnMapEmptyAction = view.findViewById(R.id.btnMapEmptyAction);

        btnMapEmptyAction.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), SettingsActivity.class));
        });

        chipGroupFilters = view.findViewById(R.id.chipGroupMapFilters);
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipFilterAll) {
                currentFilter = FilterMode.ALL;
            } else if (checkedId == R.id.chipFilterNight) {
                currentFilter = FilterMode.NIGHT;
            } else if (checkedId == R.id.chipFilterDay) {
                currentFilter = FilterMode.DAY;
            } else if (checkedId == R.id.chipFilterWeek) {
                currentFilter = FilterMode.THIS_WEEK;
            } else if (checkedId == R.id.chipFilterStores) {
                currentFilter = FilterMode.STORES;
            }
            applyFilterAndCluster();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (googleMap != null) {
            loadItemsAndRender();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Apply Mindful Sanctuary Dark Style JSON
        try {
            googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark)
            );
        } catch (Exception ignored) {
        }

        // Setup clustering
        clusterManager = new ClusterManager<>(requireContext(), googleMap);
        clusterManager.setRenderer(new ImpulseClusterRenderer(requireContext(), googleMap, clusterManager));

        googleMap.setOnCameraIdleListener(clusterManager);

        // Marker click listener handles both custom store markers and cluster items
        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof NearbyStore) {
                showStoreBottomSheet((NearbyStore) tag);
                return true;
            }
            return clusterManager.onMarkerClick(marker);
        });

        googleMap.setOnInfoWindowClickListener(clusterManager);

        // Click on individual cluster item -> Open place bottom sheet
        clusterManager.setOnClusterItemClickListener(item -> {
            showPlaceBottomSheet(item.getPosition().latitude, item.getPosition().longitude, Collections.singletonList(item.getItem()));
            return true;
        });

        // Click on a cluster -> Open place bottom sheet listing all items in the cluster
        clusterManager.setOnClusterClickListener(cluster -> {
            List<ProductItem> clusterItems = new ArrayList<>();
            for (ImpulseClusterItem ci : cluster.getItems()) {
                clusterItems.add(ci.getItem());
            }
            showPlaceBottomSheet(cluster.getPosition().latitude, cluster.getPosition().longitude, clusterItems);
            return true;
        });

        // Location layer if permission granted
        if (LocationHelper.hasLocationPermission(requireContext())) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
            }
        }

        loadItemsAndRender();
    }

    public void displayNearbyStores(@NonNull List<NearbyStore> stores, @NonNull String query) {
        this.nearbyStores = new ArrayList<>(stores);
        this.currentStoreQuery = query;
        this.currentFilter = FilterMode.STORES;

        if (chipGroupFilters != null) {
            Chip chipStores = chipGroupFilters.findViewById(R.id.chipFilterStores);
            if (chipStores != null) {
                chipStores.setChecked(true);
            }
        }

        applyFilterAndCluster();
    }

    private void loadItemsAndRender() {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(requireContext());
        allGeotaggedItems = dbHelper.getItemsWithLocation();
        applyFilterAndCluster();
    }

    private void applyFilterAndCluster() {
        if (googleMap == null || clusterManager == null) return;

        // Clear existing store markers
        for (Marker m : storeMarkers) {
            m.remove();
        }
        storeMarkers.clear();

        if (currentFilter == FilterMode.STORES) {
            // Show Stores Layer exclusively
            clusterManager.clearItems();
            clusterManager.cluster();

            renderStoreMarkers();
            return;
        }

        clusterManager.clearItems();

        List<ProductItem> filteredList = new ArrayList<>();
        int lateNightCount = 0;
        double totalFilteredAmount = 0.0;

        for (ProductItem item : allGeotaggedItems) {
            ImpulseClusterItem clusterItem = new ImpulseClusterItem(item);

            if (clusterItem.isLateNight()) {
                lateNightCount++;
            }

            boolean matches = false;
            switch (currentFilter) {
                case ALL:
                    matches = true;
                    break;
                case NIGHT:
                    matches = clusterItem.isLateNight();
                    break;
                case DAY:
                    matches = !clusterItem.isLateNight();
                    break;
                case THIS_WEEK:
                    matches = clusterItem.isThisWeek();
                    break;
                case STORES:
                    matches = false;
                    break;
            }

            if (matches) {
                filteredList.add(item);
                totalFilteredAmount += item.getPrice();
                clusterManager.addItem(clusterItem);
            }
        }

        clusterManager.cluster();

        // Update UI Summary / Empty State
        updateSummaryAndEmptyState(filteredList, totalFilteredAmount, lateNightCount);

        // Auto-fit camera
        autoFitCamera(filteredList);
    }

    private void renderStoreMarkers() {
        if (googleMap == null) return;

        if (nearbyStores.isEmpty()) {
            cardMapEmptyState.setVisibility(View.VISIBLE);
            cardMapSummary.setVisibility(View.GONE);
            tvMapEmptyTitle.setText("No Nearby Stores");
            tvMapEmptyDesc.setText("No stores found within 5 km. Tap 'Find in nearby store' on any Ready item to search local retailers.");
            btnMapEmptyAction.setVisibility(View.GONE);
            return;
        }

        cardMapEmptyState.setVisibility(View.GONE);
        cardMapSummary.setVisibility(View.VISIBLE);

        tvMapSummaryCount.setText("🏬 " + nearbyStores.size() + " stores near you · " + currentStoreQuery);
        tvMapSummaryInsight.setText(R.string.store_sheet_disclaimer);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (NearbyStore store : nearbyStores) {
            LatLng pos = new LatLng(store.getLatitude(), store.getLongitude());
            builder.include(pos);

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(store.getName())
                    .snippet(store.getFormattedDistance() + " · " + store.getOpenStatusText())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

            if (marker != null) {
                marker.setTag(store);
                storeMarkers.add(marker);
            }
        }

        try {
            int padding = (int) (100 * getResources().getDisplayMetrics().density);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
        } catch (Exception e) {
            if (!nearbyStores.isEmpty()) {
                NearbyStore first = nearbyStores.get(0);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(first.getLatitude(), first.getLongitude()), 13f));
            }
        }
    }

    private void updateSummaryAndEmptyState(List<ProductItem> filteredList, double totalFilteredAmount, int lateNightCount) {
        boolean locationTrackingEnabled = LocationHelper.isLocationTrackingEnabled(requireContext());
        boolean hasPermission = LocationHelper.hasLocationPermission(requireContext());

        if (!locationTrackingEnabled || !hasPermission) {
            cardMapEmptyState.setVisibility(View.VISIBLE);
            cardMapSummary.setVisibility(View.GONE);
            tvMapEmptyTitle.setText("Location Tracking Disabled");
            tvMapEmptyDesc.setText(R.string.map_empty_location_denied);
            btnMapEmptyAction.setVisibility(View.VISIBLE);
            return;
        }

        if (allGeotaggedItems.isEmpty()) {
            cardMapEmptyState.setVisibility(View.VISIBLE);
            cardMapSummary.setVisibility(View.GONE);
            tvMapEmptyTitle.setText("No Impulse Hotspots Yet");
            tvMapEmptyDesc.setText(R.string.map_empty_no_items);
            btnMapEmptyAction.setVisibility(View.GONE);
            return;
        }

        cardMapEmptyState.setVisibility(View.GONE);
        cardMapSummary.setVisibility(View.VISIBLE);

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        tvMapSummaryCount.setText(filteredList.size() + " triggers · " + format.format(totalFilteredAmount));

        if (!allGeotaggedItems.isEmpty()) {
            int pctNight = Math.round((lateNightCount * 100f) / allGeotaggedItems.size());
            if (pctNight > 40) {
                tvMapSummaryInsight.setText("🌙 " + pctNight + "% of your spending impulses strike late night (10PM–5AM).");
            } else {
                tvMapSummaryInsight.setText("📍 Hotspots mapped. Pausing purchases gives your mind space to decide.");
            }
        }
    }

    private void autoFitCamera(List<ProductItem> items) {
        if (googleMap == null || items.isEmpty()) return;

        if (items.size() == 1) {
            ProductItem single = items.get(0);
            LatLng latLng = new LatLng(single.getLatitudeSafe(), single.getLongitudeSafe());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        } else {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (ProductItem item : items) {
                builder.include(new LatLng(item.getLatitudeSafe(), item.getLongitudeSafe()));
            }
            try {
                int padding = (int) (80 * getResources().getDisplayMetrics().density);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
            } catch (Exception e) {
                ProductItem first = items.get(0);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(first.getLatitudeSafe(), first.getLongitudeSafe()), 12f));
            }
        }
    }

    private void showPlaceBottomSheet(double lat, double lng, List<ProductItem> items) {
        PlaceBottomSheetFragment fragment = PlaceBottomSheetFragment.newInstance(lat, lng, items);
        fragment.setOnPlaceLabelUpdatedListener(this::loadItemsAndRender);
        fragment.show(getChildFragmentManager(), "PlaceBottomSheet");
    }

    private void showStoreBottomSheet(NearbyStore store) {
        StoreBottomSheetFragment fragment = StoreBottomSheetFragment.newInstance(store);
        fragment.show(getChildFragmentManager(), "StoreBottomSheet");
    }

    // Custom Cluster Renderer with time-of-day tinted markers
    private static class ImpulseClusterRenderer extends DefaultClusterRenderer<ImpulseClusterItem> {

        public ImpulseClusterRenderer(android.content.Context context, GoogleMap map, ClusterManager<ImpulseClusterItem> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(@NonNull ImpulseClusterItem item, @NonNull MarkerOptions markerOptions) {
            float hue;
            if (item.isLateNight()) {
                hue = BitmapDescriptorFactory.HUE_ROSE; // Fuchsia/Pink for late-night triggers
            } else if (ProductItem.STATUS_UNLOCKED.equals(item.getItem().getStatus())) {
                hue = BitmapDescriptorFactory.HUE_GREEN; // Mint green for ready items
            } else {
                hue = BitmapDescriptorFactory.HUE_CYAN; // Cyan for daytime triggers
            }

            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
                    .title(item.getTitle())
                    .snippet(item.getSnippet());
        }

        @Override
        protected void onBeforeClusterRendered(@NonNull Cluster<ImpulseClusterItem> cluster, @NonNull MarkerOptions markerOptions) {
            super.onBeforeClusterRendered(cluster, markerOptions);
        }
    }
}
