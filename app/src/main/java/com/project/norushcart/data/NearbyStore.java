package com.project.norushcart.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NearbyStore implements Serializable {

    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String phoneNumber;
    private Boolean isOpenNow;
    private Double rating;
    private float distanceMeters;
    private int associatedItemId;

    public NearbyStore() {
    }

    public NearbyStore(String id, String name, String address, double latitude, double longitude,
                       String phoneNumber, Boolean isOpenNow, Double rating, int associatedItemId) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.isOpenNow = isOpenNow;
        this.rating = rating;
        this.associatedItemId = associatedItemId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "Local Retailer";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address != null ? address : "";
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getIsOpenNow() {
        return isOpenNow;
    }

    public void setIsOpenNow(Boolean openNow) {
        isOpenNow = openNow;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public float getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(float distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public int getAssociatedItemId() {
        return associatedItemId;
    }

    public void setAssociatedItemId(int associatedItemId) {
        this.associatedItemId = associatedItemId;
    }

    public String getFormattedDistance() {
        if (distanceMeters <= 0) {
            return "Nearby";
        }
        if (distanceMeters < 1000) {
            return String.format(Locale.getDefault(), "%d m away", Math.round(distanceMeters));
        } else {
            return String.format(Locale.getDefault(), "%.1f km away", distanceMeters / 1000f);
        }
    }

    public String getOpenStatusText() {
        if (isOpenNow == null) {
            return "Hours unlisted";
        }
        return isOpenNow ? "🟢 Open now" : "🔴 Closed";
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("name", name);
        obj.put("address", address);
        obj.put("lat", latitude);
        obj.put("lng", longitude);
        obj.put("phone", phoneNumber != null ? phoneNumber : "");
        obj.put("openNow", isOpenNow != null ? isOpenNow : JSONObject.NULL);
        obj.put("rating", rating != null ? rating : JSONObject.NULL);
        obj.put("itemId", associatedItemId);
        return obj;
    }

    public static NearbyStore fromJsonObject(JSONObject obj) {
        NearbyStore store = new NearbyStore();
        store.setId(obj.optString("id", ""));
        store.setName(obj.optString("name", "Store"));
        store.setAddress(obj.optString("address", ""));
        store.setLatitude(obj.optDouble("lat", 0.0));
        store.setLongitude(obj.optDouble("lng", 0.0));
        String phone = obj.optString("phone", "");
        store.setPhoneNumber(!phone.isEmpty() ? phone : null);
        if (!obj.isNull("openNow")) {
            store.setIsOpenNow(obj.optBoolean("openNow"));
        }
        if (!obj.isNull("rating")) {
            store.setRating(obj.optDouble("rating"));
        }
        store.setAssociatedItemId(obj.optInt("itemId", 0));
        return store;
    }

    public static String listToJson(List<NearbyStore> list) {
        JSONArray arr = new JSONArray();
        if (list != null) {
            for (NearbyStore store : list) {
                try {
                    arr.put(store.toJsonObject());
                } catch (JSONException ignored) {
                }
            }
        }
        return arr.toString();
    }

    public static List<NearbyStore> listFromJson(String jsonStr) {
        List<NearbyStore> list = new ArrayList<>();
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return list;
        }
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(fromJsonObject(obj));
            }
        } catch (JSONException ignored) {
        }
        return list;
    }
}
