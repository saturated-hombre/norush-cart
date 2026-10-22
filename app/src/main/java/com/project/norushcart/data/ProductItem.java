package com.project.norushcart.data;

import java.text.NumberFormat;
import java.util.Locale;

public class ProductItem {
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_UNLOCKED = "UNLOCKED";
    public static final String STATUS_DISCARDED = "DISCARDED";
    public static final String STATUS_PURCHASED = "PURCHASED";

    private int id;
    private String title;
    private String imageUrl;
    private double price;
    private String sourcePlatform;
    private String originalUrl;
    private long timestampAdded;
    private long unlockTimestamp;
    private String status;
    private Double latitude;
    private Double longitude;
    private Float locationAccuracy;
    private String placeLabel;

    public ProductItem() {
        this.timestampAdded = System.currentTimeMillis();
        this.unlockTimestamp = this.timestampAdded + 172800000L; // 48 hours in milliseconds
        this.status = STATUS_LOCKED;
        this.sourcePlatform = "Amazon";
        this.latitude = null;
        this.longitude = null;
        this.locationAccuracy = null;
        this.placeLabel = null;
    }

    public ProductItem(int id, String title, String imageUrl, double price, String sourcePlatform,
                       String originalUrl, long timestampAdded, long unlockTimestamp, String status,
                       Double latitude, Double longitude, Float locationAccuracy, String placeLabel) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.price = price;
        this.sourcePlatform = sourcePlatform;
        this.originalUrl = originalUrl;
        this.timestampAdded = timestampAdded;
        this.unlockTimestamp = unlockTimestamp;
        this.status = status;
        this.latitude = (latitude != null && (Math.abs(latitude) > 0.000001 || (longitude != null && Math.abs(longitude) > 0.000001))) ? latitude : null;
        this.longitude = (longitude != null && (Math.abs(longitude) > 0.000001 || (latitude != null && Math.abs(latitude) > 0.000001))) ? longitude : null;
        this.locationAccuracy = locationAccuracy;
        this.placeLabel = placeLabel;
    }

    public ProductItem(int id, String title, String imageUrl, double price, String sourcePlatform,
                       String originalUrl, long timestampAdded, long unlockTimestamp, String status,
                       Double latitude, Double longitude) {
        this(id, title, imageUrl, price, sourcePlatform, originalUrl, timestampAdded, unlockTimestamp, status, latitude, longitude, null, null);
    }

    public ProductItem(int id, String title, String imageUrl, double price, String sourcePlatform,
                       String originalUrl, long timestampAdded, long unlockTimestamp, String status) {
        this(id, title, imageUrl, price, sourcePlatform, originalUrl, timestampAdded, unlockTimestamp, status, null, null, null, null);
    }

    public ProductItem(String title, String imageUrl, double price, String sourcePlatform, String originalUrl) {
        this();
        this.title = title;
        this.imageUrl = imageUrl;
        this.price = price;
        this.sourcePlatform = sourcePlatform;
        this.originalUrl = originalUrl;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getSourcePlatform() {
        return sourcePlatform;
    }

    public void setSourcePlatform(String sourcePlatform) {
        this.sourcePlatform = sourcePlatform;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public long getTimestampAdded() {
        return timestampAdded;
    }

    public void setTimestampAdded(long timestampAdded) {
        this.timestampAdded = timestampAdded;
    }

    public long getUnlockTimestamp() {
        return unlockTimestamp;
    }

    public void setUnlockTimestamp(long unlockTimestamp) {
        this.unlockTimestamp = unlockTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getLatitude() {
        return latitude;
    }

    public double getLatitudeSafe() {
        return latitude != null ? latitude : 0.0;
    }

    public void setLatitude(Double latitude) {
        this.latitude = (latitude != null && Math.abs(latitude) > 0.000001) ? latitude : null;
    }

    public Double getLongitude() {
        return longitude;
    }

    public double getLongitudeSafe() {
        return longitude != null ? longitude : 0.0;
    }

    public void setLongitude(Double longitude) {
        this.longitude = (longitude != null && Math.abs(longitude) > 0.000001) ? longitude : null;
    }

    public Float getLocationAccuracy() {
        return locationAccuracy;
    }

    public void setLocationAccuracy(Float locationAccuracy) {
        this.locationAccuracy = locationAccuracy;
    }

    public String getPlaceLabel() {
        return placeLabel;
    }

    public void setPlaceLabel(String placeLabel) {
        this.placeLabel = placeLabel;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null &&
                (Math.abs(latitude) > 0.000001 || Math.abs(longitude) > 0.000001);
    }


    public boolean isLocked() {
        return STATUS_LOCKED.equalsIgnoreCase(this.status) && System.currentTimeMillis() < this.unlockTimestamp;
    }

    public String getFormattedPrice() {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return format.format(this.price);
    }

    public String getRemainingTimeFormatted() {
        long millisLeft = this.unlockTimestamp - System.currentTimeMillis();
        if (millisLeft <= 0) {
            return "Cooldown Complete";
        }

        long seconds = millisLeft / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format(Locale.getDefault(), "%dd %dh remaining", days, hours % 24);
        } else if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm remaining", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%dm %ds remaining", minutes, seconds % 60);
        } else {
            return String.format(Locale.getDefault(), "%ds remaining", seconds);
        }
    }
}
