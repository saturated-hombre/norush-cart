package com.project.norushcart.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.android.gms.maps.model.MarkerOptions;
import com.miniproject.myapplication.R;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.ProductItem;

import java.util.List;

public class ImpulseMapFragment extends Fragment implements OnMapReadyCallback {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_impulse_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(requireContext());
        List<ProductItem> items = dbHelper.getAllItems();

        LatLng lastLatLng = null;

        for (ProductItem item : items) {
            if (item.getLatitude() != 0.0 || item.getLongitude() != 0.0) {
                LatLng latLng = new LatLng(item.getLatitude(), item.getLongitude());

                float markerHue;
                if (ProductItem.STATUS_LOCKED.equalsIgnoreCase(item.getStatus())) {
                    markerHue = BitmapDescriptorFactory.HUE_RED;
                } else if (ProductItem.STATUS_DISCARDED.equalsIgnoreCase(item.getStatus())) {
                    markerHue = BitmapDescriptorFactory.HUE_GREEN;
                } else {
                    markerHue = BitmapDescriptorFactory.HUE_AZURE;
                }

                googleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(item.getTitle())
                        .snippet(item.getFormattedPrice() + " • Status: " + item.getStatus())
                        .icon(BitmapDescriptorFactory.defaultMarker(markerHue)));

                lastLatLng = latLng;
            }
        }

        if (lastLatLng != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 12f));
        } else {
            // Default center if no items with coordinates yet
            LatLng defaultCenter = new LatLng(20.5937, 78.9629); // India center
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCenter, 5f));
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
            }
        }
    }
}
