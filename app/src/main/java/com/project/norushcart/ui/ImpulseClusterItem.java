package com.project.norushcart.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.project.norushcart.data.ProductItem;

import java.util.Calendar;

public class ImpulseClusterItem implements ClusterItem {

    private final ProductItem item;
    private final LatLng position;

    public ImpulseClusterItem(ProductItem item) {
        this.item = item;
        double lat = item.getLatitudeSafe();
        double lng = item.getLongitudeSafe();
        this.position = new LatLng(lat, lng);
    }

    public ProductItem getItem() {
        return item;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return position;
    }

    @Nullable
    @Override
    public String getTitle() {
        return item.getTitle();
    }

    @Nullable
    @Override
    public String getSnippet() {
        return item.getFormattedPrice() + " impulse buy quarantined here.";
    }

    @Nullable
    @Override
    public Float getZIndex() {
        return 0.0f;
    }


    public boolean isLateNight() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(item.getTimestampAdded());
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 22 || hour < 5;
    }

    public boolean isThisWeek() {
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L);
        return item.getTimestampAdded() >= sevenDaysAgo;
    }
}
