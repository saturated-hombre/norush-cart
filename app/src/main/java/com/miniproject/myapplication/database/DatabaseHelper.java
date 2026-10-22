package com.miniproject.myapplication.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.project.norushcart.data.ProductItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "norush_cart.db";
    private static final int DATABASE_VERSION = 4;

    public static final String TABLE_COOLDOWN = "cooldown_items";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_SOURCE_PLATFORM = "source_platform";
    public static final String COLUMN_ORIGINAL_URL = "original_url";
    public static final String COLUMN_TIMESTAMP_ADDED = "timestamp_added";
    public static final String COLUMN_UNLOCK_TIMESTAMP = "unlock_timestamp";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_LOCATION_ACCURACY = "location_accuracy";
    public static final String COLUMN_PLACE_LABEL = "place_label";

    public static final String TABLE_PLACES = "places";
    public static final String COLUMN_PLACE_ID = "id";
    public static final String COLUMN_PLACE_LAT_ROUNDED = "lat_rounded";
    public static final String COLUMN_PLACE_LNG_ROUNDED = "lng_rounded";
    public static final String COLUMN_PLACE_LABEL_NAME = "label";

    public static final String TABLE_NEARBY_STORES = "nearby_stores_cache";
    public static final String COLUMN_STORE_ITEM_ID = "item_id";
    public static final String COLUMN_STORE_QUERY = "query";
    public static final String COLUMN_STORE_TIMESTAMP = "cached_timestamp";
    public static final String COLUMN_STORE_JSON = "results_json";
    public static final String COLUMN_STORE_LAT = "search_lat";
    public static final String COLUMN_STORE_LNG = "search_lng";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_COOLDOWN_TABLE = "CREATE TABLE " + TABLE_COOLDOWN + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_IMAGE_URL + " TEXT, "
                + COLUMN_PRICE + " REAL NOT NULL, "
                + COLUMN_SOURCE_PLATFORM + " TEXT, "
                + COLUMN_ORIGINAL_URL + " TEXT NOT NULL, "
                + COLUMN_TIMESTAMP_ADDED + " INTEGER, "
                + COLUMN_UNLOCK_TIMESTAMP + " INTEGER, "
                + COLUMN_STATUS + " TEXT NOT NULL, "
                + COLUMN_LATITUDE + " REAL, "
                + COLUMN_LONGITUDE + " REAL, "
                + COLUMN_LOCATION_ACCURACY + " REAL, "
                + COLUMN_PLACE_LABEL + " TEXT)";
        db.execSQL(CREATE_COOLDOWN_TABLE);

        String CREATE_PLACES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PLACES + " ("
                + COLUMN_PLACE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_PLACE_LAT_ROUNDED + " REAL NOT NULL, "
                + COLUMN_PLACE_LNG_ROUNDED + " REAL NOT NULL, "
                + COLUMN_PLACE_LABEL_NAME + " TEXT NOT NULL, "
                + "UNIQUE(" + COLUMN_PLACE_LAT_ROUNDED + ", " + COLUMN_PLACE_LNG_ROUNDED + "))";
        db.execSQL(CREATE_PLACES_TABLE);

        String CREATE_STORE_CACHE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NEARBY_STORES + " ("
                + COLUMN_STORE_ITEM_ID + " INTEGER PRIMARY KEY, "
                + COLUMN_STORE_QUERY + " TEXT NOT NULL, "
                + COLUMN_STORE_TIMESTAMP + " INTEGER NOT NULL, "
                + COLUMN_STORE_JSON + " TEXT NOT NULL, "
                + COLUMN_STORE_LAT + " REAL, "
                + COLUMN_STORE_LNG + " REAL)";
        db.execSQL(CREATE_STORE_CACHE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_COOLDOWN + " ADD COLUMN " + COLUMN_LATITUDE + " REAL");
            db.execSQL("ALTER TABLE " + TABLE_COOLDOWN + " ADD COLUMN " + COLUMN_LONGITUDE + " REAL");
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_COOLDOWN + " ADD COLUMN " + COLUMN_LOCATION_ACCURACY + " REAL");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_COOLDOWN + " ADD COLUMN " + COLUMN_PLACE_LABEL + " TEXT");
            } catch (Exception ignored) {}

            // Fix legacy 0.0 coordinates -> NULL
            db.execSQL("UPDATE " + TABLE_COOLDOWN + " SET " + COLUMN_LATITUDE + " = NULL, " + COLUMN_LONGITUDE + " = NULL WHERE "
                    + COLUMN_LATITUDE + " = 0.0 AND " + COLUMN_LONGITUDE + " = 0.0");

            String CREATE_PLACES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PLACES + " ("
                    + COLUMN_PLACE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_PLACE_LAT_ROUNDED + " REAL NOT NULL, "
                    + COLUMN_PLACE_LNG_ROUNDED + " REAL NOT NULL, "
                    + COLUMN_PLACE_LABEL_NAME + " TEXT NOT NULL, "
                    + "UNIQUE(" + COLUMN_PLACE_LAT_ROUNDED + ", " + COLUMN_PLACE_LNG_ROUNDED + "))";
            db.execSQL(CREATE_PLACES_TABLE);
        }
        if (oldVersion < 4) {
            String CREATE_STORE_CACHE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NEARBY_STORES + " ("
                    + COLUMN_STORE_ITEM_ID + " INTEGER PRIMARY KEY, "
                    + COLUMN_STORE_QUERY + " TEXT NOT NULL, "
                    + COLUMN_STORE_TIMESTAMP + " INTEGER NOT NULL, "
                    + COLUMN_STORE_JSON + " TEXT NOT NULL, "
                    + COLUMN_STORE_LAT + " REAL, "
                    + COLUMN_STORE_LNG + " REAL)";
            db.execSQL(CREATE_STORE_CACHE_TABLE);
        }
    }

    public long insertItem(ProductItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, item.getTitle());
        values.put(COLUMN_IMAGE_URL, item.getImageUrl());
        values.put(COLUMN_PRICE, item.getPrice());
        values.put(COLUMN_SOURCE_PLATFORM, item.getSourcePlatform());
        values.put(COLUMN_ORIGINAL_URL, item.getOriginalUrl());
        values.put(COLUMN_TIMESTAMP_ADDED, item.getTimestampAdded());
        values.put(COLUMN_UNLOCK_TIMESTAMP, item.getUnlockTimestamp());
        values.put(COLUMN_STATUS, item.getStatus());
        values.put(COLUMN_LATITUDE, item.getLatitude());
        values.put(COLUMN_LONGITUDE, item.getLongitude());
        values.put(COLUMN_LOCATION_ACCURACY, item.getLocationAccuracy());

        // Check if there's already a saved place label for this coordinate
        String placeLabel = item.getPlaceLabel();
        if ((placeLabel == null || placeLabel.isEmpty()) && item.hasLocation()) {
            placeLabel = getPlaceLabel(item.getLatitude(), item.getLongitude());
            if (placeLabel != null) {
                item.setPlaceLabel(placeLabel);
            }
        }
        values.put(COLUMN_PLACE_LABEL, placeLabel);

        long id = db.insert(TABLE_COOLDOWN, null, values);
        if (id != -1) {
            item.setId((int) id);
        }
        return id;
    }

    public List<ProductItem> getItemsByStatus(String status) {
        List<ProductItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Automatically auto-promote expired LOCKED items to UNLOCKED if time has passed
        if (ProductItem.STATUS_UNLOCKED.equals(status) || ProductItem.STATUS_LOCKED.equals(status)) {
            checkAndAutoUnlockExpiredItems();
        }

        Cursor cursor = db.query(
                TABLE_COOLDOWN,
                null,
                COLUMN_STATUS + " = ?",
                new String[]{status},
                null, null,
                COLUMN_TIMESTAMP_ADDED + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                items.add(cursorToProductItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return items;
    }

    public List<ProductItem> getAllItems() {
        List<ProductItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_COOLDOWN,
                null,
                null,
                null,
                null, null,
                COLUMN_TIMESTAMP_ADDED + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                items.add(cursorToProductItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return items;
    }

    public List<ProductItem> getItemsWithLocation() {
        List<ProductItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_LATITUDE + " IS NOT NULL AND " + COLUMN_LONGITUDE + " IS NOT NULL";
        Cursor cursor = db.query(
                TABLE_COOLDOWN,
                null,
                selection,
                null,
                null, null,
                COLUMN_TIMESTAMP_ADDED + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                ProductItem item = cursorToProductItem(cursor);
                if (item.hasLocation()) {
                    items.add(item);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        return items;
    }

    public ProductItem getItemById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_COOLDOWN,
                null,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        ProductItem item = null;
        if (cursor != null && cursor.moveToFirst()) {
            item = cursorToProductItem(cursor);
            cursor.close();
        }
        return item;
    }

    public void updateItemStatus(int id, String newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, newStatus);
        db.update(TABLE_COOLDOWN, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void checkAndAutoUnlockExpiredItems() {
        SQLiteDatabase db = this.getWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, ProductItem.STATUS_UNLOCKED);
        db.update(
                TABLE_COOLDOWN,
                values,
                COLUMN_STATUS + " = ? AND " + COLUMN_UNLOCK_TIMESTAMP + " <= ?",
                new String[]{ProductItem.STATUS_LOCKED, String.valueOf(now)}
        );
    }

    public double getTotalMoneySaved() {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0.0;
        String query = "SELECT SUM(" + COLUMN_PRICE + ") FROM " + TABLE_COOLDOWN + " WHERE " + COLUMN_STATUS + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{ProductItem.STATUS_DISCARDED});
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public int getDiscardedCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        String query = "SELECT COUNT(*) FROM " + TABLE_COOLDOWN + " WHERE " + COLUMN_STATUS + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{ProductItem.STATUS_DISCARDED});
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public double getMoneySavedThisWeek() {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0.0;
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L);
        String query = "SELECT SUM(" + COLUMN_PRICE + ") FROM " + TABLE_COOLDOWN + " WHERE " + COLUMN_STATUS + " = ? AND " + COLUMN_TIMESTAMP_ADDED + " >= ?";
        Cursor cursor = db.rawQuery(query, new String[]{ProductItem.STATUS_DISCARDED, String.valueOf(sevenDaysAgo)});
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public int getDiscardedCountThisWeek() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L);
        String query = "SELECT COUNT(*) FROM " + TABLE_COOLDOWN + " WHERE " + COLUMN_STATUS + " = ? AND " + COLUMN_TIMESTAMP_ADDED + " >= ?";
        Cursor cursor = db.rawQuery(query, new String[]{ProductItem.STATUS_DISCARDED, String.valueOf(sevenDaysAgo)});
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public List<ProductItem> getDiscardedThisWeek() {
        List<ProductItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L);
        Cursor cursor = db.query(
                TABLE_COOLDOWN,
                null,
                COLUMN_STATUS + " = ? AND " + COLUMN_TIMESTAMP_ADDED + " >= ?",
                new String[]{ProductItem.STATUS_DISCARDED, String.valueOf(sevenDaysAgo)},
                null, null,
                COLUMN_TIMESTAMP_ADDED + " DESC"
        );
        if (cursor != null && cursor.moveToFirst()) {
            do {
                items.add(cursorToProductItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return items;
    }

    public void clearLocationHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.putNull(COLUMN_LATITUDE);
        values.putNull(COLUMN_LONGITUDE);
        values.putNull(COLUMN_LOCATION_ACCURACY);
        values.putNull(COLUMN_PLACE_LABEL);
        db.update(TABLE_COOLDOWN, values, null, null);
        db.delete(TABLE_PLACES, null, null);
        clearNearbyStoresCache();
    }

    public void upsertPlace(double lat, double lng, String label) {
        SQLiteDatabase db = this.getWritableDatabase();
        double latRounded = Math.round(lat * 1000.0) / 1000.0;
        double lngRounded = Math.round(lng * 1000.0) / 1000.0;

        ContentValues values = new ContentValues();
        values.put(COLUMN_PLACE_LAT_ROUNDED, latRounded);
        values.put(COLUMN_PLACE_LNG_ROUNDED, lngRounded);
        values.put(COLUMN_PLACE_LABEL_NAME, label);

        db.insertWithOnConflict(TABLE_PLACES, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        // Update items close to this coordinate with the new label
        ContentValues updateValues = new ContentValues();
        updateValues.put(COLUMN_PLACE_LABEL, label);
        String where = "ABS(" + COLUMN_LATITUDE + " - ?) < 0.0015 AND ABS(" + COLUMN_LONGITUDE + " - ?) < 0.0015";
        db.update(TABLE_COOLDOWN, updateValues, where, new String[]{String.valueOf(lat), String.valueOf(lng)});
    }

    public String getPlaceLabel(double lat, double lng) {
        SQLiteDatabase db = this.getReadableDatabase();
        double latRounded = Math.round(lat * 1000.0) / 1000.0;
        double lngRounded = Math.round(lng * 1000.0) / 1000.0;

        Cursor cursor = db.query(
                TABLE_PLACES,
                new String[]{COLUMN_PLACE_LABEL_NAME},
                COLUMN_PLACE_LAT_ROUNDED + " = ? AND " + COLUMN_PLACE_LNG_ROUNDED + " = ?",
                new String[]{String.valueOf(latRounded), String.valueOf(lngRounded)},
                null, null, null
        );

        String label = null;
        if (cursor != null && cursor.moveToFirst()) {
            label = cursor.getString(0);
            cursor.close();
        }
        return label;
    }

    public void deleteItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_COOLDOWN, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public long setItemUnlockTimestamp(int itemId, long unlockTimestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_UNLOCK_TIMESTAMP, unlockTimestamp);
        values.put(COLUMN_STATUS, ProductItem.STATUS_LOCKED);
        db.update(TABLE_COOLDOWN, values, COLUMN_ID + " = ?", new String[]{String.valueOf(itemId)});
        return unlockTimestamp;
    }

    public long debugFastForwardCooldown(int itemId) {
        return setItemUnlockTimestamp(itemId, System.currentTimeMillis() + 10000L);
    }

    private ProductItem cursorToProductItem(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
        String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE));
        String sourcePlatform = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE_PLATFORM));
        String originalUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ORIGINAL_URL));
        long timestampAdded = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP_ADDED));
        long unlockTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UNLOCK_TIMESTAMP));
        String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));

        Double latitude = null;
        int latIndex = cursor.getColumnIndex(COLUMN_LATITUDE);
        if (latIndex != -1 && !cursor.isNull(latIndex)) {
            latitude = cursor.getDouble(latIndex);
            if (Math.abs(latitude) < 0.000001) latitude = null;
        }

        Double longitude = null;
        int lngIndex = cursor.getColumnIndex(COLUMN_LONGITUDE);
        if (lngIndex != -1 && !cursor.isNull(lngIndex)) {
            longitude = cursor.getDouble(lngIndex);
            if (Math.abs(longitude) < 0.000001) longitude = null;
        }

        Float locationAccuracy = null;
        int accIndex = cursor.getColumnIndex(COLUMN_LOCATION_ACCURACY);
        if (accIndex != -1 && !cursor.isNull(accIndex)) {
            locationAccuracy = cursor.getFloat(accIndex);
        }

        String placeLabel = null;
        int labelIndex = cursor.getColumnIndex(COLUMN_PLACE_LABEL);
        if (labelIndex != -1 && !cursor.isNull(labelIndex)) {
            placeLabel = cursor.getString(labelIndex);
        }

        return new ProductItem(id, title, imageUrl, price, sourcePlatform, originalUrl,
                timestampAdded, unlockTimestamp, status, latitude, longitude, locationAccuracy, placeLabel);
    }

    public void saveNearbyStoresCache(int itemId, String query, String resultsJson, double lat, double lng) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STORE_ITEM_ID, itemId);
        values.put(COLUMN_STORE_QUERY, query);
        values.put(COLUMN_STORE_TIMESTAMP, System.currentTimeMillis());
        values.put(COLUMN_STORE_JSON, resultsJson);
        values.put(COLUMN_STORE_LAT, lat);
        values.put(COLUMN_STORE_LNG, lng);
        db.insertWithOnConflict(TABLE_NEARBY_STORES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getCachedNearbyStoresJson(int itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        long twentyFourHoursAgo = System.currentTimeMillis() - (24L * 60L * 60L * 1000L);
        Cursor cursor = db.query(
                TABLE_NEARBY_STORES,
                new String[]{COLUMN_STORE_JSON},
                COLUMN_STORE_ITEM_ID + " = ? AND " + COLUMN_STORE_TIMESTAMP + " >= ?",
                new String[]{String.valueOf(itemId), String.valueOf(twentyFourHoursAgo)},
                null, null, null
        );
        String json = null;
        if (cursor != null && cursor.moveToFirst()) {
            json = cursor.getString(0);
            cursor.close();
        }
        return json;
    }

    public List<com.project.norushcart.data.NearbyStore> getCachedNearbyStores(int itemId) {
        String json = getCachedNearbyStoresJson(itemId);
        if (json != null && !json.trim().isEmpty()) {
            return com.project.norushcart.data.NearbyStore.listFromJson(json);
        }
        return new ArrayList<>();
    }

    public void clearNearbyStoresCache() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_NEARBY_STORES, null, null);
        } catch (Exception ignored) {
        }
    }
}
